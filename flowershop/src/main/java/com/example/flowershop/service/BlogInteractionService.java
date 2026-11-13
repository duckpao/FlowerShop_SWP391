package com.example.flowershop.service;

import com.example.flowershop.entity.Blog;
import com.example.flowershop.entity.BlogComment;
import com.example.flowershop.entity.BlogInteraction;
import com.example.flowershop.entity.BlogInteractionId;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.BlogStatus;
import com.example.flowershop.entity.enums.InteractionType;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.entity.enums.UserRole;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
            String content, LocalDateTime createdDate, String parentCommentId, List<CommentResult> replies) {
    }

    public record CommentResults(List<CommentResult> content, int page, int totalPages, long totalElements) {
    }

    private final BlogRepository blogs;
    private final BlogInteractionRepository interactions;
    private final BlogCommentRepository comments;
    private final UserRepository users;
    private final BlogService blogService;

    public BlogInteractionService(BlogRepository blogs, BlogInteractionRepository interactions,
            BlogCommentRepository comments, UserRepository users, BlogService blogService) {
        this.blogs = blogs;
        this.interactions = interactions;
        this.comments = comments;
        this.users = users;
        this.blogService = blogService;
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

    public BlogService.Results saved(String actor, int page) {
        if (page < 0 || page > 100000) {
            throw new IllegalArgumentException("Trang không hợp lệ.");
        }
        return blogService.results(interactions.findVisibleBlogsByUserAndType(
                actor, InteractionType.SAVE, PageRequest.of(page, 12)));
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
        return commentPage(blogId, page);
    }

    public CommentResults managedComments(String shopId, String blogId, String actor, int page) {
        blogService.managedBlog(shopId, blogId, actor, false);
        return commentPage(blogId, page);
    }

    public EngagementResult managedEngagement(String shopId, String blogId, String actor) {
        blogService.managedBlog(shopId, blogId, actor, false);
        return new EngagementResult(
                interactions.countByIdBlogIdAndIdInteractionType(blogId, InteractionType.LIKE),
                comments.countByBlogId(blogId));
    }

    private CommentResults commentPage(String blogId, int page) {
        if (page < 0 || page > 100000) {
            throw new IllegalArgumentException("Trang không hợp lệ.");
        }
        var result = comments.findByBlogIdAndParentCommentIsNull(blogId, PageRequest.of(page, 10,
                Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("id"))));
        List<String> parentIds = result.getContent().stream().map(BlogComment::getId).toList();
        Map<String, List<BlogComment>> replies = parentIds.isEmpty() ? Map.of()
                : comments.findByBlogIdAndParentCommentIdInOrderByCreatedDateAscIdAsc(blogId, parentIds).stream()
                        .collect(Collectors.groupingBy(reply -> reply.getParentComment().getId()));
        return new CommentResults(result.getContent().stream()
                .map(comment -> commentResult(comment, replies.getOrDefault(comment.getId(), List.of()).stream()
                        .map(BlogInteractionService::commentResult).toList())).toList(),
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
                .content(commentContent(input))
                .createdBy(actor)
                .build();
        return commentResult(comments.saveAndFlush(comment));
    }

    @Transactional
    public CommentResult updateComment(String blogId, String commentId, String actor, CommentInput input) {
        visibleBlog(blogId);
        BlogComment comment = ownComment(blogId, commentId, actor);
        comment.setContent(commentContent(input));
        comment.setLastModifyBy(actor);
        return commentResult(comments.saveAndFlush(comment));
    }

    @Transactional
    public CommentResult reply(String shopId, String blogId, String commentId, String actor, CommentInput input) {
        Blog blog = blogService.managedBlog(shopId, blogId, actor, true);
        BlogComment parent = comments.findByIdAndBlogId(commentId, blogId)
                .orElseThrow(BlogInteractionService::missingComment);
        if (parent.getParentComment() != null
                || parent.getUser().getRole() != UserRole.CUSTOMER) {
            throw new IllegalArgumentException("Chỉ có thể trả lời bình luận của khách hàng.");
        }
        User user = users.findById(actor).orElseThrow(BlogInteractionService::missingUser);
        BlogComment reply = BlogComment.builder()
                .id(UUID.randomUUID().toString()).blog(blog).user(user).parentComment(parent)
                .content(commentContent(input)).createdBy(actor).build();
        BlogComment saved = comments.save(reply);
        parent.getReplies().add(saved);
        comments.flush();
        return commentResult(saved);
    }

    @Transactional
    public void moderateComment(String shopId, String blogId, String commentId, String actor) {
        blogService.managedBlog(shopId, blogId, actor, true);
        BlogComment comment = comments.findByIdAndBlogId(commentId, blogId)
                .orElseThrow(BlogInteractionService::missingComment);
        removeComment(comment);
    }

    @Transactional
    public void deleteComment(String blogId, String commentId, String actor) {
        visibleBlog(blogId);
        removeComment(ownComment(blogId, commentId, actor));
    }

    private void removeComment(BlogComment comment) {
        if (comment.getParentComment() != null) {
            comment.getParentComment().getReplies().removeIf(reply -> reply.getId().equals(comment.getId()));
        }
        comments.delete(comment);
        comments.flush();
    }

    private BlogComment ownComment(String blogId, String commentId, String actor) {
        BlogComment comment = comments.findByIdAndBlogId(commentId, blogId)
                .orElseThrow(BlogInteractionService::missingComment);
        if (!comment.getUser().getId().equals(actor)) {
            throw missingComment();
        }
        return comment;
    }

    private static String commentContent(CommentInput input) {
        if (input == null || input.content() == null || input.content().isBlank() || input.content().length() > 2000) {
            throw new IllegalArgumentException("Bình luận phải có nội dung từ 1 đến 2000 ký tự.");
        }
        return input.content().strip();
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
        return commentResult(comment, List.of());
    }

    private static CommentResult commentResult(BlogComment comment, List<CommentResult> replies) {
        User author = comment.getUser();
        String name = author.getFullName();
        return new CommentResult(comment.getId(), author.getId(),
                name == null || name.isBlank() ? "Khách hàng" : name,
                author.getAvatarUrl(), comment.getContent(), comment.getCreatedDate(),
                comment.getParentComment() == null ? null : comment.getParentComment().getId(), replies);
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
