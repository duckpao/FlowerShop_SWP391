package com.example.flowershop.repository;

import com.example.flowershop.entity.BlogComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlogCommentRepository extends JpaRepository<BlogComment, String> {
    Page<BlogComment> findByBlogId(String blogId, Pageable pageable);

    long countByBlogId(String blogId);

    Optional<BlogComment> findByIdAndBlogId(String id, String blogId);
}
