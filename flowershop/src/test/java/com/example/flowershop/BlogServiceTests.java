package com.example.flowershop;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.entity.Shop;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.BlogStatus;
import com.example.flowershop.entity.enums.InteractionType;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.entity.enums.UserRole;
import com.example.flowershop.entity.enums.UserStatus;
import com.example.flowershop.repository.BlogRepository;
import com.example.flowershop.repository.BlogCommentRepository;
import com.example.flowershop.repository.ShopRepository;
import com.example.flowershop.repository.UserRepository;
import com.example.flowershop.service.BlogService;
import com.example.flowershop.service.BlogInteractionService;
import com.example.flowershop.service.CloudinaryService;
import com.example.flowershop.service.TokenAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    @MockitoBean TokenAuthService tokens;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired BlogRepository blogs;
    @Autowired BlogCommentRepository comments;
    @Autowired BlogService service;
    @Autowired BlogInteractionService interactionService;
    @Autowired MockMvc mvc;
    @Autowired jakarta.persistence.EntityManager entityManager;

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
    void managerCanCreateUpdateHideAndRepublishPost() {
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
        assertThatThrownBy(() -> service.publicDetail(published.id()))
                .isInstanceOf(ResponseStatusException.class);

        service.publish(shop.getId(), published.id(), owner.getId());
        assertThat(service.listPublic(null, "roses", 0).content()).extracting(BlogService.Result::id)
                .containsExactly(published.id());
        var restored = service.publicDetail(published.id());
        assertThat(restored.status()).isEqualTo(BlogStatus.PUBLISHED);
        assertThat(restored.title()).isEqualTo(published.title());
        assertThat(restored.content()).isEqualTo(published.content());
        assertThat(restored.images()).isEqualTo(published.images());
        assertThat(blogs.findById(published.id()).orElseThrow().getLastModifyBy()).isEqualTo(owner.getId());
    }

    @Test
    void inactiveAndForeignShopsCannotPublish() {
        var stranger = users.saveAndFlush(User.builder().id(id()).email(id() + "@example.test")
                .role(UserRole.SHOP).status(UserStatus.ACTIVE).isEmailVerified(true).build());
        var input = new BlogService.Input("Flower guide", "Enough useful content", BlogStatus.PUBLISHED, List.of());
        var post = service.save(shop.getId(), null, owner.getId(), input);
        service.hide(shop.getId(), post.id(), owner.getId());
        assertThatThrownBy(() -> service.publish(shop.getId(), post.id(), stranger.getId()))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.save(shop.getId(), null, stranger.getId(), input))
                .isInstanceOf(ResponseStatusException.class);
        shop.setStatus(ShopStatus.INACTIVE);
        shops.saveAndFlush(shop);
        assertThatThrownBy(() -> service.publish(shop.getId(), post.id(), owner.getId()))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(blogs.findById(post.id()).orElseThrow().getStatus()).isEqualTo(BlogStatus.HIDDEN);
        assertThatThrownBy(() -> service.save(shop.getId(), null, owner.getId(), input))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void managerCanRepublishHiddenPostThroughApi() throws Exception {
        var post = service.save(shop.getId(), null, owner.getId(),
                new BlogService.Input("Rose care", "Keep the vase clean.", BlogStatus.PUBLISHED, List.of()));
        service.hide(shop.getId(), post.id(), owner.getId());
        mvc.perform(get("/api/public/blogs/" + post.id())).andExpect(status().isNotFound());

        var csrfResponse = mvc.perform(get("/api/auth/csrf")).andReturn();
        var csrf = new ObjectMapper().readTree(csrfResponse.getResponse().getContentAsString());
        var session = (MockHttpSession) csrfResponse.getRequest().getSession(false);
        String path = "/api/shop/mine/" + shop.getId() + "/blogs/" + post.id() + "/publish";
        mvc.perform(put(path).session(session)
                .header(csrf.path("headerName").asText(), csrf.path("token").asText()))
                .andExpect(status().isUnauthorized());
        when(tokens.authenticate("shop-test")).thenReturn(CurrentUser.from(owner));
        mvc.perform(put(path).session(session)
                .header(csrf.path("headerName").asText(), csrf.path("token").asText())
                .header("Authorization", "Bearer shop-test"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/public/blogs/" + post.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/public/blogs").param("q", "Rose care"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(post.id()));
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
        mvc.perform(get("/api/public/blogs/" + post.id() + "/engagement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.commentCount").value(0));
        mvc.perform(get("/api/public/blogs/" + post.id() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/shop/mine/" + shop.getId() + "/blogs")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerCanLikeSaveCommentAndRemoveOwnComment() {
        var customer = users.saveAndFlush(User.builder().id(id()).email(id() + "@example.test")
                .fullName("Rose Lover").role(UserRole.CUSTOMER).status(UserStatus.ACTIVE)
                .isEmailVerified(true).build());
        var post = service.save(shop.getId(), null, owner.getId(),
                new BlogService.Input("Rose care", "Useful flower care content", BlogStatus.PUBLISHED, List.of()));

        assertThat(interactionService.engagement(post.id()).likeCount()).isZero();
        assertThat(interactionService.mine(post.id(), customer.getId()))
                .isEqualTo(new BlogInteractionService.InteractionResult(false, false));

        interactionService.setInteraction(post.id(), customer.getId(), InteractionType.LIKE, true);
        interactionService.setInteraction(post.id(), customer.getId(), InteractionType.LIKE, true);
        interactionService.setInteraction(post.id(), customer.getId(), InteractionType.SAVE, true);
        assertThat(interactionService.engagement(post.id()).likeCount()).isEqualTo(1);
        assertThat(interactionService.mine(post.id(), customer.getId()))
                .isEqualTo(new BlogInteractionService.InteractionResult(true, true));
        assertThat(interactionService.saved(customer.getId(), 0).content())
                .extracting(BlogService.Result::id).containsExactly(post.id());

        var comment = interactionService.addComment(post.id(), customer.getId(),
                new BlogInteractionService.CommentInput("  Bài viết rất hữu ích!  "));
        assertThat(comment.content()).isEqualTo("Bài viết rất hữu ích!");
        assertThat(interactionService.comments(post.id(), 0).content())
                .extracting(BlogInteractionService.CommentResult::id).containsExactly(comment.id());
        assertThat(interactionService.engagement(post.id()).commentCount()).isEqualTo(1);

        interactionService.deleteComment(post.id(), comment.id(), customer.getId());
        interactionService.setInteraction(post.id(), customer.getId(), InteractionType.LIKE, false);
        interactionService.setInteraction(post.id(), customer.getId(), InteractionType.SAVE, false);
        assertThat(interactionService.engagement(post.id()))
                .isEqualTo(new BlogInteractionService.EngagementResult(0, 0));
        assertThat(interactionService.mine(post.id(), customer.getId()))
                .isEqualTo(new BlogInteractionService.InteractionResult(false, false));
        assertThat(interactionService.saved(customer.getId(), 0).content()).isEmpty();
    }

    @Test
    void customersCanEditOnlyTheirOwnCommentsAndCannotMoveThemAcrossBlogs() {
        var customer = customer();
        var stranger = customer();
        var post = publishedPost();
        var otherPost = publishedPost();
        var comment = interactionService.addComment(post.id(), customer.getId(),
                new BlogInteractionService.CommentInput("Original comment"));

        var updated = interactionService.updateComment(post.id(), comment.id(), customer.getId(),
                new BlogInteractionService.CommentInput("  Updated comment  "));
        assertThat(updated.content()).isEqualTo("Updated comment");
        assertThat(updated.createdDate()).isEqualTo(comment.createdDate());
        assertThat(comments.findById(comment.id()).orElseThrow().getLastModifyBy()).isEqualTo(customer.getId());
        assertThatThrownBy(() -> interactionService.updateComment(post.id(), comment.id(), stranger.getId(),
                new BlogInteractionService.CommentInput("Not mine"))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> interactionService.deleteComment(post.id(), comment.id(), stranger.getId()))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> interactionService.updateComment(otherPost.id(), comment.id(), customer.getId(),
                new BlogInteractionService.CommentInput("Wrong blog"))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> interactionService.updateComment(post.id(), comment.id(), customer.getId(),
                new BlogInteractionService.CommentInput("   "))).isInstanceOf(IllegalArgumentException.class);
        assertThat(comments.findById(comment.id()).orElseThrow().getContent()).isEqualTo("Updated comment");
    }

    @Test
    void shopRepliesStayWithTheirParentAcrossCommentPages() {
        var customer = customer();
        var post = publishedPost();
        var parent = interactionService.addComment(post.id(), customer.getId(),
                new BlogInteractionService.CommentInput("How often should I water?"));
        for (int i = 0; i < 10; i++) {
            interactionService.addComment(post.id(), customer.getId(),
                    new BlogInteractionService.CommentInput("Other comment " + i));
        }
        var reply = interactionService.reply(shop.getId(), post.id(), parent.id(), owner.getId(),
                new BlogInteractionService.CommentInput("  Once a week.  "));
        assertThat(reply.parentCommentId()).isEqualTo(parent.id());
        assertThat(reply.authorId()).isEqualTo(owner.getId());
        assertThat(reply.content()).isEqualTo("Once a week.");
        assertThat(interactionService.comments(post.id(), 0).totalElements()).isEqualTo(11);
        assertThat(interactionService.comments(post.id(), 0).content()).hasSize(10);
        var secondPage = interactionService.comments(post.id(), 1);
        assertThat(secondPage.content()).extracting(BlogInteractionService.CommentResult::id).containsExactly(parent.id());
        assertThat(secondPage.content().get(0).replies()).extracting(BlogInteractionService.CommentResult::id)
                .containsExactly(reply.id());
        assertThat(interactionService.engagement(post.id()).commentCount()).isEqualTo(12);
        assertThatThrownBy(() -> interactionService.reply(shop.getId(), post.id(), reply.id(), owner.getId(),
                new BlogInteractionService.CommentInput("Nested reply"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void managerAndCustomerDeletionRemoveRepliesWithoutLeavingOrphans() {
        var customer = customer();
        var post = publishedPost();
        var parent = interactionService.addComment(post.id(), customer.getId(),
                new BlogInteractionService.CommentInput("Comment to moderate"));
        var reply = interactionService.reply(shop.getId(), post.id(), parent.id(), owner.getId(),
                new BlogInteractionService.CommentInput("Shop reply"));
        interactionService.moderateComment(shop.getId(), post.id(), parent.id(), owner.getId());
        assertThat(comments.findById(parent.id())).isEmpty();
        assertThat(comments.findById(reply.id())).isEmpty();
        assertThat(interactionService.engagement(post.id()).commentCount()).isZero();

        parent = interactionService.addComment(post.id(), customer.getId(),
                new BlogInteractionService.CommentInput("My comment"));
        reply = interactionService.reply(shop.getId(), post.id(), parent.id(), owner.getId(),
                new BlogInteractionService.CommentInput("Another reply"));
        interactionService.deleteComment(post.id(), parent.id(), customer.getId());
        assertThat(comments.findById(parent.id())).isEmpty();
        assertThat(comments.findById(reply.id())).isEmpty();
        assertThat(interactionService.comments(post.id(), 0).totalElements()).isZero();
    }

    @Test
    void deletingAReplyKeepsTheCustomerCommentAndFreshlyLoadedThreadsCanBeDeleted() {
        var customer = customer();
        var post = publishedPost();
        var parent = interactionService.addComment(post.id(), customer.getId(),
                new BlogInteractionService.CommentInput("Customer comment"));
        var reply = interactionService.reply(shop.getId(), post.id(), parent.id(), owner.getId(),
                new BlogInteractionService.CommentInput("First response"));
        var keptReply = interactionService.reply(shop.getId(), post.id(), parent.id(), owner.getId(),
                new BlogInteractionService.CommentInput("Second response"));
        entityManager.clear();

        interactionService.moderateComment(shop.getId(), post.id(), reply.id(), owner.getId());
        assertThat(comments.findById(parent.id())).isPresent();
        assertThat(comments.findById(reply.id())).isEmpty();
        assertThat(interactionService.comments(post.id(), 0).content().get(0).replies())
                .extracting(BlogInteractionService.CommentResult::id).containsExactly(keptReply.id());
        entityManager.clear();

        interactionService.deleteComment(post.id(), parent.id(), customer.getId());
        assertThat(comments.findById(parent.id())).isEmpty();
        assertThat(comments.findById(keptReply.id())).isEmpty();
        assertThat(interactionService.engagement(post.id()).commentCount()).isZero();
    }

    @Test
    void managerCanModerateHiddenBlogsButCannotActOnAnotherShop() {
        var customer = customer();
        var post = publishedPost();
        var parent = interactionService.addComment(post.id(), customer.getId(),
                new BlogInteractionService.CommentInput("Customer feedback"));
        var foreignOwner = users.saveAndFlush(User.builder().id(id()).email(id() + "@example.test")
                .role(UserRole.SHOP).status(UserStatus.ACTIVE).isEmailVerified(true).build());
        var foreignShop = shops.saveAndFlush(Shop.builder().id(id()).owner(foreignOwner).name("Other shop")
                .status(ShopStatus.ACTIVE).build());
        assertThatThrownBy(() -> interactionService.managedComments(shop.getId(), post.id(), foreignOwner.getId(), 0))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> interactionService.reply(shop.getId(), post.id(), parent.id(), foreignOwner.getId(),
                new BlogInteractionService.CommentInput("Foreign reply"))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> interactionService.moderateComment(shop.getId(), post.id(), parent.id(), foreignOwner.getId()))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> interactionService.moderateComment(foreignShop.getId(), post.id(), parent.id(), foreignOwner.getId()))
                .isInstanceOf(ResponseStatusException.class);

        service.hide(shop.getId(), post.id(), owner.getId());
        assertThat(interactionService.managedComments(shop.getId(), post.id(), owner.getId(), 0).totalElements()).isEqualTo(1);
        interactionService.reply(shop.getId(), post.id(), parent.id(), owner.getId(),
                new BlogInteractionService.CommentInput("Response while hidden"));
        interactionService.moderateComment(shop.getId(), post.id(), parent.id(), owner.getId());
        assertThat(interactionService.managedEngagement(shop.getId(), post.id(), owner.getId()).commentCount()).isZero();
    }

    @Test
    void commentApisEnforceRolesOwnershipAndContentValidation() throws Exception {
        var customer = customer();
        var stranger = customer();
        var post = publishedPost();
        var parent = interactionService.addComment(post.id(), customer.getId(),
                new BlogInteractionService.CommentInput("Original"));
        String customerPath = "/api/customer/blogs/" + post.id() + "/comments/" + parent.id();
        String managerPath = "/api/shop/mine/" + shop.getId() + "/blogs/" + post.id() + "/comments";

        performAs(customer, put(customerPath).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Edited\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("Edited"));
        performAs(stranger, put(customerPath).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Stolen\"}"))
                .andExpect(status().isNotFound());
        performAs(stranger, delete(customerPath)).andExpect(status().isNotFound());
        performAs(customer, put(customerPath).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());
        performAs(customer, put(customerPath).contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(new BlogInteractionService.CommentInput("x".repeat(2001)))))
                .andExpect(status().isBadRequest());
        performAs(customer, get(managerPath)).andExpect(status().isForbidden());
        performAs(customer, post(managerPath + "/" + parent.id() + "/replies")
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Fake shop\"}"))
                .andExpect(status().isForbidden());
        performAs(customer, delete(managerPath + "/" + parent.id())).andExpect(status().isForbidden());
        performAs(owner, put(customerPath).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Manager edit\"}"))
                .andExpect(status().isForbidden());
        performAs(owner, post(managerPath + "/" + parent.id() + "/replies")
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Shop response\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.parentCommentId").value(parent.id()));
        mvc.perform(get("/api/public/blogs/" + post.id() + "/comments"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].replies[0].content").value("Shop response"));
        performAs(owner, delete(managerPath + "/" + parent.id())).andExpect(status().isNoContent());
        mvc.perform(get("/api/public/blogs/" + post.id() + "/comments"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    private ResultActions performAs(User actor, MockHttpServletRequestBuilder request) throws Exception {
        String token = "test-" + actor.getId();
        when(tokens.authenticate(token)).thenReturn(CurrentUser.from(actor));
        var csrfResponse = mvc.perform(get("/api/auth/csrf")).andReturn();
        var csrf = new ObjectMapper().readTree(csrfResponse.getResponse().getContentAsString());
        var session = (MockHttpSession) csrfResponse.getRequest().getSession(false);
        return mvc.perform(request.session(session)
                .header(csrf.path("headerName").asText(), csrf.path("token").asText())
                .header("Authorization", "Bearer " + token));
    }

    private User customer() {
        return users.saveAndFlush(User.builder().id(id()).email(id() + "@example.test")
                .fullName("Customer").role(UserRole.CUSTOMER).status(UserStatus.ACTIVE).isEmailVerified(true).build());
    }

    private BlogService.Result publishedPost() {
        return service.save(shop.getId(), null, owner.getId(),
                new BlogService.Input("Flower care", "Care instructions", BlogStatus.PUBLISHED, List.of()));
    }

    private String id() {
        return UUID.randomUUID().toString();
    }
}
