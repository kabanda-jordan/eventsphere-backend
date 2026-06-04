package com.eventsphere.controller;

import com.eventsphere.dto.request.ChatMessageRequest;
import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.service.impl.ChatServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Chat", description = "Direct messaging between users")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatServiceImpl chatService;

    public ChatController(ChatServiceImpl chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "Send a message", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Sends a direct message to another user identified by their user ID.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Message sent successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Receiver not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @PostMapping("/send")
    public ResponseEntity<ApiResponse.Success<ApiResponse.ChatMessageData>> send(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.Success.of("Message sent successfully",
                chatService.sendMessage(userDetails.getUsername(), request)));
    }

    @Operation(summary = "Get conversation", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Returns paginated conversation history between the authenticated user and another user. Also marks received messages as read.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversation fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @GetMapping("/conversation/{userId}")
    public ResponseEntity<ApiResponse.Success<Map<String, Object>>> conversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID of the other user in the conversation") @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.Success.of("Conversation fetched successfully",
                chatService.getConversation(userDetails.getUsername(), userId, PageRequest.of(page, size))));
    }

    @Operation(summary = "Get unread message count", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Returns the number of unread messages for the authenticated user.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unread count fetched successfully")
    })
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse.Success<Long>> unreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.Success.of("Unread count fetched successfully",
                chatService.getUnreadCount(userDetails.getUsername())));
    }
}
