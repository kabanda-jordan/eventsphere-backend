package com.eventsphere.service.impl;

import com.eventsphere.dto.request.EventRequest;
import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.entity.Event;
import com.eventsphere.entity.Registration;
import com.eventsphere.entity.Student;
import com.eventsphere.entity.User;
import com.eventsphere.enums.EventStatus;
import com.eventsphere.exception.GlobalExceptionHandler.BadRequestException;
import com.eventsphere.exception.GlobalExceptionHandler.EventFullException;
import com.eventsphere.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.eventsphere.repository.EventRepository;
import com.eventsphere.repository.RegistrationRepository;
import com.eventsphere.repository.StudentRepository;
import com.eventsphere.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EventServiceImpl {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public EventServiceImpl(EventRepository eventRepository, RegistrationRepository registrationRepository,
                            StudentRepository studentRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEvents(String search, EventStatus status, Pageable pageable) {
        Page<Event> page = eventRepository.search(search, status, pageable);
        return pagedResult(page.map(this::toEventData));
    }

    @Transactional(readOnly = true)
    public ApiResponse.EventData getEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return toEventData(event);
    }

    @Transactional(readOnly = true)
    public List<ApiResponse.EventData> getUpcomingEvents(int limit) {
        return eventRepository.findByEventDateAfterOrderByEventDateAsc(LocalDateTime.now(), PageRequest.of(0, limit))
                .stream()
                .map(this::toEventData)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApiResponse.EventData> getPastEvents(int limit) {
        return eventRepository.findByEventDateBeforeOrderByEventDateDesc(LocalDateTime.now(), PageRequest.of(0, limit))
                .stream()
                .map(this::toEventData)
                .toList();
    }

    @Transactional
    public ApiResponse.EventData createEvent(EventRequest request, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        Event event = new Event();
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setLocation(request.getLocation());
        event.setCapacity(request.getCapacity());
        event.setStatus(request.getStatus() == null ? EventStatus.ACTIVE : request.getStatus());
        event.setCreatedBy(admin);

        return toEventData(eventRepository.save(event));
    }

    @Transactional
    public ApiResponse.EventData updateEvent(Long id, EventRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setLocation(request.getLocation());
        event.setCapacity(request.getCapacity());
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }
        return toEventData(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        eventRepository.delete(event);
    }

    @Transactional
    public ApiResponse.EventData registerForEvent(Long eventId, String username) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        Student student = studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new BadRequestException("Only active events can accept registrations");
        }
        if (registrationRepository.existsByEventAndStudent(event, student)) {
            throw new BadRequestException("Student is already registered for this event");
        }
        if (registrationRepository.countByEvent(event) >= event.getCapacity()) {
            throw new EventFullException();
        }

        Registration registration = new Registration();
        registration.setEvent(event);
        registration.setStudent(student);
        registrationRepository.save(registration);

        return toEventData(event);
    }

    @Transactional
    public void cancelRegistration(Long eventId, String username) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        Student student = studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        if (!registrationRepository.existsByEventAndStudent(event, student)) {
            throw new ResourceNotFoundException("Registration not found");
        }
        registrationRepository.deleteByEventAndStudent(event, student);
    }

    private ApiResponse.EventData toEventData(Event event) {
        ApiResponse.EventData data = new ApiResponse.EventData();
        data.setId(event.getId());
        data.setTitle(event.getTitle());
        data.setDescription(event.getDescription());
        data.setEventDate(event.getEventDate());
        data.setLocation(event.getLocation());
        data.setCapacity(event.getCapacity());
        data.setStatus(event.getStatus());
        data.setCreatedById(event.getCreatedBy().getId());
        data.setCreatedByName(event.getCreatedBy().getFullName());
        data.setRegistrationsCount(registrationRepository.countByEventId(event.getId()));
        return data;
    }

    private Map<String, Object> pagedResult(Page<?> page) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", page.getContent());
        result.put("page", page.getNumber());
        result.put("size", page.getSize());
        result.put("totalElements", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("last", page.isLast());
        return result;
    }
}
