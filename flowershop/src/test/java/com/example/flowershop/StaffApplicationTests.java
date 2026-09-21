package com.example.flowershop;
import com.example.flowershop.service.StaffApplicationService;
import com.example.flowershop.repository.StaffApplicationRepository;
import com.example.flowershop.entity.enums.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class StaffApplicationTests extends ManagerShopTests {
    @Autowired StaffApplicationService applications;
    @Autowired StaffApplicationRepository applicationRows;
    @Override @BeforeEach void setup() {applicationRows.deleteAll();super.setup();}
    @AfterEach void clearApplications() {applicationRows.deleteAll();}
    StaffApplicationService.Input input() {return new StaffApplicationService.Input("Applicant","0912345678","Flower arranging experience");}
    @Test void approvalChangesRoleAndAssignsOnlyChosenShop() {
        var customer=user(UserRole.CUSTOMER);
        var token=auth.login(new com.example.flowershop.dto.auth.LoginRequest(customer.getEmail(),"FlowerShop123"));
        var a=applications.apply(shop.getId(),customer.getId(),input());
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(service.assigned(customer.getId())).isEmpty();
        assertThatThrownBy(()->applications.decide(shop.getId(),a.id(),other.getId(),true)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        applications.decide(shop.getId(),a.id(),owner.getId(),true);
        assertThatThrownBy(()->auth.authenticate(token.accessToken())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->auth.refresh(token.refreshToken())).isInstanceOf(RuntimeException.class);
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(UserRole.SHOP_STAFF);
        assertThat(service.assigned(customer.getId())).extracting("id").containsExactly(shop.getId());
        assertThatThrownBy(()->applications.decide(shop.getId(),a.id(),owner.getId(),true)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(members.findByShopIdOrderByIdAsc(shop.getId())).hasSize(1);
        org.mockito.Mockito.verify(mail).send(org.mockito.ArgumentMatchers.argThat((org.springframework.mail.SimpleMailMessage message) ->
            message.getSubject().contains("Có đơn đăng ký") && java.util.Arrays.asList(message.getTo()).contains(owner.getEmail())));
        org.mockito.Mockito.verify(mail).send(org.mockito.ArgumentMatchers.argThat((org.springframework.mail.SimpleMailMessage message) ->
            message.getSubject().contains("đã được duyệt") && java.util.Arrays.asList(message.getTo()).contains(customer.getEmail())));
    }
    @Test void rejectionKeepsCustomerAndAllowsResubmit() {
        var customer=user(UserRole.CUSTOMER);var a=applications.apply(shop.getId(),customer.getId(),input());
        assertThatThrownBy(()->applications.apply(shop.getId(),customer.getId(),input())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        applications.decide(shop.getId(),a.id(),owner.getId(),false);
        org.mockito.Mockito.verify(mail).send(org.mockito.ArgumentMatchers.argThat((org.springframework.mail.SimpleMailMessage message) ->
            message.getSubject().contains("Kết quả ứng tuyển") && java.util.Arrays.asList(message.getTo()).contains(customer.getEmail())));
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(service.assigned(customer.getId())).isEmpty();
        assertThat(applications.apply(shop.getId(),customer.getId(),input()).status()).isEqualTo("PENDING");
    }
    @Test void rejectsIneligibleApplicantsAndClosedShops() {
        assertThatThrownBy(()->applications.apply(shop.getId(),staff.getId(),input())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var customer=user(UserRole.CUSTOMER);customer.setIsEmailVerified(false);users.saveAndFlush(customer);
        assertThatThrownBy(()->applications.apply(shop.getId(),customer.getId(),input())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        customer.setIsEmailVerified(true);users.saveAndFlush(customer);shop.setStatus(ShopStatus.BANNED);shops.saveAndFlush(shop);
        assertThatThrownBy(()->applications.apply(shop.getId(),customer.getId(),input())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    @Test void publicSearchAndAnonymousApplicationSecurity() throws Exception {
        mvc.perform(get("/api/public/shops/"+shop.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Test")).andExpect(jsonPath("$.owner").doesNotExist());
        mvc.perform(get("/api/public/shops").param("q","Test")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Test")).andExpect(jsonPath("$[0].owner").doesNotExist());
        mvc.perform(post("/api/customer/shops/"+shop.getId()+"/applications").contentType("application/json").content("{}" )).andExpect(status().isForbidden());
        shop.setStatus(ShopStatus.BANNED);shops.saveAndFlush(shop);
        mvc.perform(get("/api/public/shops/"+shop.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/shops")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void customerCannotReviewApplicationsAndManagerCannotReadForeignShop() throws Exception {
        var customer=user(UserRole.CUSTOMER);
        var customerToken=auth.login(new com.example.flowershop.dto.auth.LoginRequest(customer.getEmail(),"FlowerShop123")).accessToken();
        mvc.perform(get("/api/shop/mine/"+shop.getId()+"/applications").header("Authorization","Bearer "+customerToken)).andExpect(status().isForbidden());
        var otherToken=auth.login(new com.example.flowershop.dto.auth.LoginRequest(other.getEmail(),"FlowerShop123")).accessToken();
        mvc.perform(get("/api/shop/mine/"+shop.getId()+"/applications").header("Authorization","Bearer "+otherToken)).andExpect(status().isNotFound());
        mvc.perform(post("/api/customer/shops/"+shop.getId()+"/applications").header("Authorization","Bearer "+customerToken).contentType("application/json").content("{}" )).andExpect(status().isForbidden());
    }
}
