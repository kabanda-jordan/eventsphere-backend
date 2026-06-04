package com.eventsphere.controller;

import com.eventsphere.config.TestSecurityConfig;
import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.enums.EventStatus;
import com.eventsphere.exception.GlobalExceptionHandler;
import com.eventsphere.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.eventsphere.security.JwtTokenProvider;
import com.eventsphere.security.UserDetailsServiceImpl;
import com.eventsphere.service.impl.EventServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice tests for EventController — tests HTTP layer, response shape, and validation.
 * Uses TestSecurityConfig to bypass JWT auth so tests focus on business logic.
 */
@WebMvcTest(EventController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private EventServiceImpl eventService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    // ─── Test 12: GET /api/events — returns 200 with paginated data ─────────────

    @Test
    @DisplayName("GET /api/events — returns 200 with paginated data")
    void getEvents_returns200() throws Exception {
        Map<String, Object> pagedResult = new LinkedHashMap<>();
        pagedResult.put("content", List.of());
        pagedResult.put("page", 0);
        pagedResult.put("size", 10);
        pagedResult.put("totalElements", 0L);
        pagedResult.put("totalPages", 0);
        pagedResult.put("last", true);

        when(eventService.getEvents(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pagedResult);

        mockMvc.perform(get("/api/events").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Events fetched successfully"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // ─── Test 13: GET /api/events/{id} — not found returns 404 ─────────────────

    @Test
    @DisplayName("GET /api/events/{id} — returns 404 when event does not exist")
    void getEvent_notFound_returns404() throws Exception {
        when(eventService.getEvent(999L))
                .thenThrow(new ResourceNotFoundException("Event not found"));

        mockMvc.perform(get("/api/events/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Event not found"));
    }
`
    // ─── Test 14: POST /api/events — validation: missing required fields → 400 ──

    @Test
    @DisplayName("POST /api/events — returns 400 when required fields are missing")
    void createEvent_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Missing title, date, location, capacity"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── Test 15: GET /api/events/upcoming — returns list ───────────────────────

    @Test
    @DisplayName("GET /api/events/upcoming — returns 200 with list of upcoming events")
    void getUpcomingEvents_returns200() throws Exception {
        ApiResponse.EventData event = new ApiResponse.EventData();
        event.setId(1L);
        event.setTitle("Upcoming Event");
        event.setEventDate(LocalDateTime.now().plusDays(7));
        event.setStatus(EventStatus.ACTIVE);
        event.setCapacity(100);
        event.setLocation("Nairobi");
        event.setCreatedById(1L);
        event.setCreatedByName("Admin");

        when(eventService.getUpcomingEvents(5)).thenReturn(List.of(event));

        mockMvc.perform(get("/api/events/upcoming?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Upcoming Event"));
    }

    // ─── Test 16: POST /api/events — valid body with mock user → 200 ────────────

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("POST /api/events — returns 200 when valid body and service succeeds")
    void createEvent_validBody_returns200() throws Exception {
        ApiResponse.EventData created = new ApiResponse.EventData();
        created.setId(1L);
        created.setTitle("New Event");
        created.setStatus(EventStatus.ACTIVE);
        created.setCapacity(50);
        created.setLocation("Nairobi");
        created.setEventDate(LocalDateTime.now().plusDays(10));
        created.setCreatedById(1L);
        created.setCreatedByName("Admin");

        when(eventService.createEvent(any(), any())).thenReturn(created);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "New Event",
                                  "eventDate": "2027-06-01T10:00:00",
                                  "location": "Nairobi",
                                  "capacity": 50
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("New Event"));
    }
}
