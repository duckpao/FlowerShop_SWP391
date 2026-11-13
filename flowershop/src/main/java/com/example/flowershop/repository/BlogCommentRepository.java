package com.example.flowershop.repository;

import com.example.flowershop.entity.BlogComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BlogCommentRepository extends JpaRepository<BlogComment, String> {
    Page<BlogComment> findByBlogId(String blogId, Pageable pageable);

    Page<BlogComment> findByBlogIdAndParentCommentIsNull(String blogId, Pageable pageable);

    List<BlogComment> findByBlogIdAndParentCommentIdInOrderByCreatedDateAscIdAsc(String blogId, List<String> parentIds);

    long countByBlogId(String blogId);

    Optional<BlogComment> findByIdAndBlogId(String id, String blogId);

    @Query("select count(distinct c.user.id) from BlogComment c where c.blog.shop.id = :shopId "
            + "and c.createdDate >= :start and c.createdDate < :end")
    long countCommentersForReport(@Param("shopId") String shopId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select c.blog.id, c.blog.title, c.blog.status, count(distinct c.user.id), count(c) "
            + "from BlogComment c where c.blog.shop.id = :shopId "
            + "and c.createdDate >= :start and c.createdDate < :end "
            + "group by c.blog.id, c.blog.title, c.blog.status")
    List<Object[]> engagementForReport(@Param("shopId") String shopId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
