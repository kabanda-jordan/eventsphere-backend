package com.eventsphere.controller;

import com.eventsphere.dto.request.EventRequest;
import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.enums.EventStatus;
import com.eventsphere.service.impl.EventServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Events", description = "Event management and registration endpoints")
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventServiceImpl eventService;

    public EventController(EventServiceImpl eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "List all events",
               description = "Returns a paginated list of events. Supports optional search by title/location and filtering by status. Public endpoint.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Events fetched successfully")
    })
    @GetMapping
    public ResponseEntity<ApiResponse.Success<Map<String, Object>>> getEvents(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search by title or location") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by event status: ACTIVE, CANCELLED, COMPLETED") @RequestParam(required = false) EventStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.Success.of("Events fetched successfully",
                eventService.getEvents(search, status, pageable)));
    }

    @Operation(summary = "Get event by ID", description = "Returns a single event by its ID. Public endpoint.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse.Success<ApiResponse.EventData>> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.Success.of("Event fetched successfully", eventService.getEvent(id)));
    }

    @Operation(summary = "Get upcoming events", description = "Returns upcoming events ordered by date ascending. Public endpoint.")
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse.Success<List<ApiResponse.EventData>>> getUpcomingEvents(
            @Parameter(description = "Maximum number of events to return") @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.Success.of("Upcoming events fetched successfully",
                eventService.getUpcomingEvents(limit)));
    }

    @Operation(summary = "Get past events", description = "Returns past events ordered by date descending. Public endpoint.")
    @GetMapping("/past")
    public ResponseEntity<ApiResponse.Success<List<ApiResponse.EventData>>> getPastEvents(
            @Parameter(description = "Maximum number of events to return") @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.Success.of("Past events fetched successfully",
                eventService.getPastEvents(limit)));
    }

    @Operation(summary = "Create event", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Creates a new event. Requires ADMIN role.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse.Success<ApiResponse.EventData>> createEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(ApiResponse.Success.of("Event created successfully",
                eventService.createEvent(request, userDetails.getUsername())));
    }

    @Operation(summary = "Update event", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Updates an existing event. Requires ADMIN role.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse.Success<ApiResponse.EventData>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(ApiResponse.Success.of("Event updated successfully",
                eventService.updateEvent(id, request)));
    }

    @Operation(summary = "Delete event", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Deletes an event. Requires ADMIN role.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse.Success<Void>> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.Success.of("Event deleted successfully", null));
    }

    @Operation(summary = "Register for event", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Registers the authenticated student for an event. The user must have a student profile.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registration successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Already registered or event not active",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event or student profile not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Event is at full capacity",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @PostMapping("/{id}/register")
    public ResponseEntity<ApiResponse.Success<ApiResponse.EventData>> registerForEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.Success.of("Event registration successful",
                eventService.registerForEvent(id, userDetails.getUsername())));
    }

    @Operation(summary = "Cancel event registration", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Cancels the authenticated student's registration for an event.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registration cancelled successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Registration, event, or student not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @DeleteMapping("/{id}/register")
    public ResponseEntity<ApiResponse.Success<Void>> cancelRegistration(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        eventService.cancelRegistration(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.Success.of("Event registration cancelled successfully", null));
    }
}
