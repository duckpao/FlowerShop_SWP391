package com.example.flowershop.repository;

import com.example.flowershop.entity.BlogInteraction;
import com.example.flowershop.entity.BlogInteractionId;
import com.example.flowershop.entity.Blog;
import com.example.flowershop.entity.enums.InteractionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BlogInteractionRepository extends JpaRepository<BlogInteraction, BlogInteractionId> {
    long countByIdBlogIdAndIdInteractionType(String blogId, InteractionType interactionType);

    boolean existsByIdUserIdAndIdBlogIdAndIdInteractionType(
            String userId, String blogId, InteractionType interactionType);

    void deleteByIdUserIdAndIdBlogIdAndIdInteractionType(
            String userId, String blogId, InteractionType interactionType);

    @Query("select i.blog.id, i.blog.title, i.blog.status, i.id.interactionType, count(i) "
            + "from BlogInteraction i where i.blog.shop.id = :shopId "
            + "and i.createdDate >= :start and i.createdDate < :end "
            + "group by i.blog.id, i.blog.title, i.blog.status, i.id.interactionType")
    List<Object[]> engagementForReport(@Param("shopId") String shopId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "select interaction.blog from BlogInteraction interaction "
            + "where interaction.id.userId = :userId "
            + "and interaction.id.interactionType = :interactionType "
            + "and interaction.blog.status = com.example.flowershop.entity.enums.BlogStatus.PUBLISHED "
            + "and interaction.blog.shop.status = com.example.flowershop.entity.enums.ShopStatus.ACTIVE "
            + "order by interaction.createdDate desc",
            countQuery = "select count(interaction) from BlogInteraction interaction "
                    + "where interaction.id.userId = :userId "
                    + "and interaction.id.interactionType = :interactionType "
                    + "and interaction.blog.status = com.example.flowershop.entity.enums.BlogStatus.PUBLISHED "
                    + "and interaction.blog.shop.status = com.example.flowershop.entity.enums.ShopStatus.ACTIVE")
    Page<Blog> findVisibleBlogsByUserAndType(@Param("userId") String userId,
            @Param("interactionType") InteractionType interactionType, Pageable pageable);
}
