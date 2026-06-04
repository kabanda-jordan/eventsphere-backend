package com.eventsphere.controller;

import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.enums.EventStatus;
import com.eventsphere.repository.ChatMessageRepository;
import com.eventsphere.repository.EventRepository;
import com.eventsphere.repository.RegistrationRepository;
import com.eventsphere.repository.StudentRepository;
import com.eventsphere.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.Map;

@Tag(name = "Admin", description = "Admin-only management endpoints")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final EventRepository eventRepository;
    private final StudentRepository studentRepository;
    private final RegistrationRepository registrationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public AdminController(EventRepository eventRepository,
                           StudentRepository studentRepository,
                           RegistrationRepository registrationRepository,
                           ChatMessageRepository chatMessageRepository,
                           UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.studentRepository = studentRepository;
        this.registrationRepository = registrationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get dashboard statistics", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Returns aggregate statistics: total students, total registrations, unread messages for the admin, and event counts by status. Requires ADMIN role.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard statistics fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse.Success<ApiResponse.DashboardStats>> dashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<EventStatus, Long> eventsByStatus = new EnumMap<>(EventStatus.class);
        for (EventStatus status : EventStatus.values()) {
            eventsByStatus.put(status, eventRepository.countByStatus(status));
        }

        var currentUser = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        ApiResponse.DashboardStats stats = new ApiResponse.DashboardStats(
                studentRepository.count(),
                registrationRepository.count(),
                chatMessageRepository.countByReceiverAndIsReadFalse(currentUser),
                eventsByStatus
        );

        return ResponseEntity.ok(ApiResponse.Success.of("Dashboard statistics fetched successfully", stats));
    }
}
