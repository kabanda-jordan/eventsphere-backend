package com.eventsphere.service.impl;

import com.eventsphere.dto.request.ChatMessageRequest;
import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.entity.ChatMessage;
import com.eventsphere.entity.User;
import com.eventsphere.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.eventsphere.repository.ChatMessageRepository;
import com.eventsphere.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class ChatServiceImpl {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatServiceImpl(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ApiResponse.ChatMessageData sendMessage(String senderUsername, ChatMessageRequest request) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(request.getContent());
        message.setRead(false);

        return toChatMessageData(chatMessageRepository.save(message));
    }

    @Transactional
    public Map<String, Object> getConversation(String username, Long otherUserId, Pageable pageable) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation user not found"));

        Page<ChatMessage> page = chatMessageRepository.findConversation(currentUser.getId(), otherUserId, pageable);
        chatMessageRepository.markConversationAsRead(otherUserId, currentUser.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", page.getContent().stream().map(this::toChatMessageData).toList());
        result.put("page", page.getNumber());
        result.put("size", page.getSize());
        result.put("totalElements", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("last", page.isLast());
        return result;
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return chatMessageRepository.countByReceiverAndIsReadFalse(currentUser);
    }

    private ApiResponse.ChatMessageData toChatMessageData(ChatMessage message) {
        ApiResponse.ChatMessageData data = new ApiResponse.ChatMessageData();
        data.setId(message.getId());
        data.setSenderId(message.getSender().getId());
        data.setSenderName(message.getSender().getFullName());
        data.setReceiverId(message.getReceiver().getId());
        data.setReceiverName(message.getReceiver().getFullName());
        data.setContent(message.getContent());
        data.setRead(message.isRead());
        data.setSentAt(message.getSentAt());
        return data;
    }
}
