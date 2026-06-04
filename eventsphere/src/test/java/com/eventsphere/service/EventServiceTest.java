package com.eventsphere.service;

import com.eventsphere.dto.request.EventRequest;
import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.entity.Event;
import com.eventsphere.entity.Registration;
import com.eventsphere.entity.Student;
import com.eventsphere.entity.User;
import com.eventsphere.enums.EventStatus;
import com.eventsphere.enums.Role;
import com.eventsphere.exception.GlobalExceptionHandler.BadRequestException;
import com.eventsphere.exception.GlobalExceptionHandler.EventFullException;
import com.eventsphere.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.eventsphere.repository.EventRepository;
import com.eventsphere.repository.RegistrationRepository;
import com.eventsphere.repository.StudentRepository;
import com.eventsphere.repository.UserRepository;
import com.eventsphere.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EventServiceImpl — covers event creation, registration, and capacity enforcement.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private User adminUser;
    private Event activeEvent;
    private Student student;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setFullName("System Admin");
        adminUser.setRole(Role.ADMIN);

        activeEvent = new Event();
        activeEvent.setId(10L);
        activeEvent.setTitle("Spring Conference 2026");
        activeEvent.setDescription("Annual spring conference");
        activeEvent.setEventDate(LocalDateTime.now().plusDays(30));
        activeEvent.setLocation("Nairobi, Kenya");
        activeEvent.setCapacity(100);
        activeEvent.setStatus(EventStatus.ACTIVE);
        activeEvent.setCreatedBy(adminUser);

        student = new Student();
        student.setId(5L);
        student.setStudentId("STU001");
        User studentUser = new User();
        studentUser.setId(2L);
        studentUser.setUsername("student1");
        student.setUser(studentUser);
    }

    // ─── Test 6: Create event successfully ──────────────────────────────────────

    @Test
    @DisplayName("createEvent() — success: persists event and returns EventData")
    void createEvent_success() {
        // Arrange
        EventRequest request = new EventRequest(
                "Spring Conference 2026",
                "Annual spring conference",
                LocalDateTime.now().plusDays(30),
                "Nairobi, Kenya",
                100,
                EventStatus.ACTIVE
        );

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(eventRepository.save(any(Event.class))).thenReturn(activeEvent);
        when(registrationRepository.countByEventId(10L)).thenReturn(0L);

        // Act
        ApiResponse.EventData result = eventService.createEvent(request, "admin");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Spring Conference 2026");
        assertThat(result.getStatus()).isEqualTo(EventStatus.ACTIVE);
        assertThat(result.getCapacity()).isEqualTo(100);
        verify(eventRepository).save(any(Event.class));
    }

    // ─── Test 7: Register for event — success ───────────────────────────────────

    @Test
    @DisplayName("registerForEvent() — success: creates registration when event is active and has capacity")
    void registerForEvent_success() {
        // Arrange
        when(eventRepository.findById(10L)).thenReturn(Optional.of(activeEvent));
        when(studentRepository.findByUserUsername("student1")).thenReturn(Optional.of(student));
        when(registrationRepository.existsByEventAndStudent(activeEvent, student)).thenReturn(false);
        when(registrationRepository.countByEvent(activeEvent)).thenReturn(50L); // 50 of 100 used
        when(registrationRepository.save(any(Registration.class))).thenReturn(new Registration());
        when(registrationRepository.countByEventId(10L)).thenReturn(51L);

        // Act
        ApiResponse.EventData result = eventService.registerForEvent(10L, "student1");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Spring Conference 2026");
        verify(registrationRepository).save(any(Registration.class));
    }

    // ─── Test 8: Register for event — already registered ────────────────────────

    @Test
    @DisplayName("registerForEvent() — failure: throws BadRequestException when already registered")
    void registerForEvent_alreadyRegistered_throwsBadRequest() {
        // Arrange
        when(eventRepository.findById(10L)).thenReturn(Optional.of(activeEvent));
        when(studentRepository.findByUserUsername("student1")).thenReturn(Optional.of(student));
        when(registrationRepository.existsByEventAndStudent(activeEvent, student)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> eventService.registerForEvent(10L, "student1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");

        verify(registrationRepository, never()).save(any());
    }

    // ─── Test 9: Register for event — event at full capacity ────────────────────

    @Test
    @DisplayName("registerForEvent() — failure: throws EventFullException when capacity is reached")
    void registerForEvent_eventFull_throwsEventFull() {
        // Arrange
        activeEvent.setCapacity(50);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(activeEvent));
        when(studentRepository.findByUserUsername("student1")).thenReturn(Optional.of(student));
        when(registrationRepository.existsByEventAndStudent(activeEvent, student)).thenReturn(false);
        when(registrationRepository.countByEvent(activeEvent)).thenReturn(50L); // exactly at capacity

        // Act & Assert
        assertThatThrownBy(() -> eventService.registerForEvent(10L, "student1"))
                .isInstanceOf(EventFullException.class);

        verify(registrationRepository, never()).save(any());
    }

    // ─── Test 10: Register for cancelled event ──────────────────────────────────

    @Test
    @DisplayName("registerForEvent() — failure: throws BadRequestException when event is not ACTIVE")
    void registerForEvent_cancelledEvent_throwsBadRequest() {
        // Arrange
        activeEvent.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(activeEvent));
        when(studentRepository.findByUserUsername("student1")).thenReturn(Optional.of(student));

        // Act & Assert
        assertThatThrownBy(() -> eventService.registerForEvent(10L, "student1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("active events");

        verify(registrationRepository, never()).save(any());
    }

    // ─── Test 11: Get event — not found ─────────────────────────────────────────

    @Test
    @DisplayName("getEvent() — failure: throws ResourceNotFoundException when event does not exist")
    void getEvent_notFound_throwsResourceNotFound() {
        // Arrange
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> eventService.getEvent(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event not found");
    }
}
