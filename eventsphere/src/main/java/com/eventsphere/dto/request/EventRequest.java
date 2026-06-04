package com.eventsphere.dto.request;

import com.eventsphere.enums.EventStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EventRequest {

    @NotBlank(message = "title: must not be blank")
    @Size(max = 200, message = "title: size must be less than or equal to 200")
    private String title;
    private String description;
    @NotNull(message = "eventDate: must not be null")
    @Future(message = "eventDate: must be a future date")
    private LocalDateTime eventDate;
    @NotBlank(message = "location: must not be blank")
    private String location;
    @NotNull(message = "capacity: must not be null")
    @Min(value = 1, message = "capacity: must be greater than or equal to 1")
    private Integer capacity;
    private EventStatus status;

    public EventRequest() {
    }

    public EventRequest(String title, String description, LocalDateTime eventDate, String location, Integer capacity, EventStatus status) {
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.location = location;
        this.capacity = capacity;
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }
}
