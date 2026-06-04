package com.eventsphere.repository;

import com.eventsphere.entity.ChatMessage;
import com.eventsphere.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            select m from ChatMessage m
            where (m.sender.id = :userId and m.receiver.id = :otherUserId)
               or (m.sender.id = :otherUserId and m.receiver.id = :userId)
            order by m.sentAt asc
            """)
    Page<ChatMessage> findConversation(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId, Pageable pageable);

    long countByReceiverAndIsReadFalse(User receiver);

    @Modifying
    @Query("""
            update ChatMessage m
            set m.isRead = true
            where m.sender.id = :senderId
              and m.receiver.id = :receiverId
              and m.isRead = false
            """)
    int markConversationAsRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}
