package com.example.flowershop.repository;

import com.example.flowershop.entity.ChatMessage;
import com.example.flowershop.entity.enums.SenderType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    @Query("select m from ChatMessage m join fetch m.sender where m.session.id = :sessionId "
            + "order by m.createdDate desc, m.id desc")
    List<ChatMessage> latest(String sessionId, Pageable page);

    @Query("select m from ChatMessage m join fetch m.sender where m.session.id = :sessionId and m.createdDate < :before "
            + "order by m.createdDate desc, m.id desc")
    List<ChatMessage> olderThan(String sessionId, LocalDateTime before, Pageable page);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ChatMessage m set m.isRead = true where m.session.id = :sessionId and m.senderType = :sender and m.isRead = false")
    int markRead(String sessionId, SenderType sender);
}
