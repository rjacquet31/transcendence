package com.transcendance.backend.repository;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transcendance.backend.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            SELECT m FROM ChatMessage m
            WHERE ((m.sender.idUser = :userA AND m.receiver.idUser = :userB)
                OR (m.sender.idUser = :userB AND m.receiver.idUser = :userA))
              AND (:before IS NULL OR m.id < :before)
            ORDER BY m.id DESC
            """)
    Slice<ChatMessage> findConversation(
            @Param("userA") UUID userA,
            @Param("userB") UUID userB,
            @Param("before") Long before,
            Pageable pageable);


    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ChatMessage m SET m.isRead = true
            WHERE m.receiver.idUser = :me
              AND m.sender.idUser = :other
              AND m.isRead = false
            """)
    int markConversationAsRead(@Param("me") UUID me, @Param("other") UUID other);

}
