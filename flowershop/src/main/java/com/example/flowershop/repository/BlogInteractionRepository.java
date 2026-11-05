package com.example.flowershop.repository;

import com.example.flowershop.entity.BlogInteraction;
import com.example.flowershop.entity.BlogInteractionId;
import com.example.flowershop.entity.enums.InteractionType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogInteractionRepository extends JpaRepository<BlogInteraction, BlogInteractionId> {
    long countByIdBlogIdAndIdInteractionType(String blogId, InteractionType interactionType);

    boolean existsByIdUserIdAndIdBlogIdAndIdInteractionType(
            String userId, String blogId, InteractionType interactionType);

    void deleteByIdUserIdAndIdBlogIdAndIdInteractionType(
            String userId, String blogId, InteractionType interactionType);
}
