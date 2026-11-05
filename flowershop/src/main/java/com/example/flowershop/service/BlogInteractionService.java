package com.example.flowershop.service;

import com.example.flowershop.entity.Blog;
import com.example.flowershop.entity.BlogComment;
import com.example.flowershop.entity.BlogInteraction;
import com.example.flowershop.entity.BlogInteractionId;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.BlogStatus;
import com.example.flowershop.entity.enums.InteractionType;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.repository.BlogCommentRepository;
import com.example.flowershop.repository.BlogInteractionRepository;
import com.example.flowershop.repository.BlogRepository;
import com.example.flowershop.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BlogInteractionService {
    public record EngagementResult(long likeCount, long commentCount) {
    }

    public record InteractionResult(boolean liked, boolean saved) {
    }

    public record CommentInput(@NotBlank @Size(max = 2000) String content) {
    }

    public record CommentResult(String id, String authorId, String authorName, String authorAvatarUrl,
            String content, LocalDateTime createdDate) {
    }

    public record CommentResults(List<CommentResult> content, int page, int totalPages, long totalElements) {
    }

    private final BlogRepository blogs;
    private final BlogInteractionRepository interactions;
    private final BlogCommentRepository comments;
    private final UserRepository users;

    public BlogInteractionService(BlogRepository blogs, BlogInteractionRepository interactions,
            BlogCommentRepository comments, UserRepository users) {
        this.blogs = blogs;
        this.interactions = interactions;
        this.comments = comments;
        this.users = users;
    }

    public EngagementResult engagement(String blogId) {
        visibleBlog(blogId);
        return new EngagementResult(
                interactions.countByIdBlogIdAndIdInteractionType(blogId, InteractionType.LIKE),
                comments.countByBlogId(blogId));
    }

    public InteractionResult mine(String blogId, String actor) {
        visibleBlog(blogId);
        return interactionResult(blogId, actor);
    }

    @Transactional
    public InteractionResult setInteraction(String blogId, String actor, InteractionType type, boolean active) {
        Blog blog = visibleBlog(blogId);
        boolean exists = interactions.existsByIdUserIdAndIdBlogIdAndIdInteractionType(actor, blogId, type);
        if (active && !exists) {
            User user = users.findById(actor).orElseThrow(BlogInteractionService::missingUser);
            interactions.saveAndFlush(BlogInteraction.builder()
                    .id(new BlogInteractionId(actor, blogId, type))
                    .user(user)
                    .blog(blog)
                    .createdBy(actor)
                    .build());
        } else if (!active && exists) {
            interactions.deleteByIdUserIdAndIdBlogIdAndIdInteractionType(actor, blogId, type);
            interactions.flush();
        }
        return interactionResult(blogId, actor);
    }

    public CommentResults comments(String blogId, int page) {
        visibleBlog(blogId);
        if (page < 0 || page > 100000) {
            throw new IllegalArgumentException("Trang không hợp lệ.");
        }
        var result = comments.findByBlogId(blogId, PageRequest.of(page, 10,
                Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("id"))));
        return new CommentResults(result.getContent().stream().map(BlogInteractionService::commentResult).toList(),
                result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }

    @Transactional
    public CommentResult addComment(String blogId, String actor, CommentInput input) {
        Blog blog = visibleBlog(blogId);
        User user = users.findById(actor).orElseThrow(BlogInteractionService::missingUser);
        BlogComment comment = BlogComment.builder()
                .id(UUID.randomUUID().toString())
                .blog(blog)
                .user(user)
                .content(input.content().strip())
                .createdBy(actor)
                .build();
        return commentResult(comments.saveAndFlush(comment));
    }

    @Transactional
    public void deleteComment(String blogId, String commentId, String actor) {
        visibleBlog(blogId);
        BlogComment comment = comments.findByIdAndBlogId(commentId, blogId)
                .orElseThrow(BlogInteractionService::missingComment);
        if (!comment.getUser().getId().equals(actor)) {
            throw missingComment();
        }
        comments.delete(comment);
    }

    private InteractionResult interactionResult(String blogId, String actor) {
        return new InteractionResult(
                interactions.existsByIdUserIdAndIdBlogIdAndIdInteractionType(actor, blogId, InteractionType.LIKE),
                interactions.existsByIdUserIdAndIdBlogIdAndIdInteractionType(actor, blogId, InteractionType.SAVE));
    }

    private Blog visibleBlog(String blogId) {
        return blogs.findByIdAndStatusAndShop_Status(blogId, BlogStatus.PUBLISHED, ShopStatus.ACTIVE)
                .orElseThrow(BlogInteractionService::missingBlog);
    }

    private static CommentResult commentResult(BlogComment comment) {
        User author = comment.getUser();
        String name = author.getFullName();
        return new CommentResult(comment.getId(), author.getId(),
                name == null || name.isBlank() ? "Khách hàng" : name,
                author.getAvatarUrl(), comment.getContent(), comment.getCreatedDate());
    }

    private static ResponseStatusException missingBlog() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết.");
    }

    private static ResponseStatusException missingComment() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bình luận.");
    }

    private static ResponseStatusException missingUser() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng.");
    }
}
