package com.example.flowershop;

import com.example.flowershop.entity.Shop;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.BlogStatus;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.entity.enums.UserRole;
import com.example.flowershop.entity.enums.UserStatus;
import com.example.flowershop.repository.BlogRepository;
import com.example.flowershop.repository.ShopRepository;
import com.example.flowershop.repository.UserRepository;
import com.example.flowershop.service.BlogService;
import com.example.flowershop.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:blogs;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.shop-follow-mail.enabled=false", "spring.jpa.show-sql=false"
})
@Transactional
@AutoConfigureMockMvc
class BlogServiceTests {
    @MockitoBean JavaMailSender mail;
    @MockitoBean CloudinaryService cloudinary;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired BlogRepository blogs;
    @Autowired BlogService service;
    @Autowired MockMvc mvc;

    User owner;
    Shop shop;

    @BeforeEach
    void fixture() {
        owner = users.saveAndFlush(User.builder().id(id()).email(id() + "@example.test").fullName("Florist")
                .role(UserRole.SHOP).status(UserStatus.ACTIVE).isEmailVerified(true).build());
        shop = shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("Bloom shop")
                .status(ShopStatus.ACTIVE).build());
    }

    @Test
    void managerCanCreateUpdateAndHidePost() {
        when(cloudinary.uploadBlogImage(any(), any(), any())).thenReturn("https://cdn.example.test/rose.jpg");
        var file = new MockMultipartFile("images", "rose.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
        var created = service.save(shop.getId(), null, owner.getId(),
                new BlogService.Input("Keep roses fresh", "Trim stems every day.", BlogStatus.DRAFT,
                        List.of()), List.of(file));
        assertThat(created.status()).isEqualTo(BlogStatus.DRAFT);
        assertThat(created.images()).containsExactly("https://cdn.example.test/rose.jpg");
        assertThat(service.listPublic(null, "roses", 0).content()).isEmpty();

        var published = service.save(shop.getId(), created.id(), owner.getId(),
                new BlogService.Input("Keep roses fresh", "Use a clean vase.", BlogStatus.PUBLISHED,
                        created.images()));
        assertThat(service.listPublic(null, "roses", 0).content()).extracting(BlogService.Result::id)
                .containsExactly(published.id());
        assertThat(service.publicDetail(published.id()).images()).hasSize(1);

        service.hide(shop.getId(), published.id(), owner.getId());
        assertThat(service.listPublic(null, "roses", 0).content()).isEmpty();
        assertThat(blogs.findById(published.id()).orElseThrow().getStatus()).isEqualTo(BlogStatus.HIDDEN);
    }

    @Test
    void inactiveAndForeignShopsCannotPublish() {
        var stranger = users.saveAndFlush(User.builder().id(id()).email(id() + "@example.test")
                .role(UserRole.SHOP).status(UserStatus.ACTIVE).isEmailVerified(true).build());
        var input = new BlogService.Input("Flower guide", "Enough useful content", BlogStatus.PUBLISHED, List.of());
        assertThatThrownBy(() -> service.save(shop.getId(), null, stranger.getId(), input))
                .isInstanceOf(ResponseStatusException.class);
        shop.setStatus(ShopStatus.INACTIVE);
        shops.saveAndFlush(shop);
        assertThatThrownBy(() -> service.save(shop.getId(), null, owner.getId(), input))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void publicApiOnlyReturnsPublishedPosts() throws Exception {
        var post = service.save(shop.getId(), null, owner.getId(),
                new BlogService.Input("Orchid care", "Avoid direct sunlight.", BlogStatus.PUBLISHED, List.of()));
        service.save(shop.getId(), null, owner.getId(),
                new BlogService.Input("Private draft", "Not ready", BlogStatus.DRAFT, List.of()));

        mvc.perform(get("/api/public/blogs").param("q", "orchid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(post.id()));
        mvc.perform(get("/api/public/blogs/" + post.id()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Orchid care"));
        mvc.perform(get("/api/shop/mine/" + shop.getId() + "/blogs")).andExpect(status().isUnauthorized());
    }

    private String id() {
        return UUID.randomUUID().toString();
    }
}
