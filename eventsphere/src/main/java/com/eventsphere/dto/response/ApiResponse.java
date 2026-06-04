package com.eventsphere.dto.response;

import com.eventsphere.enums.EventStatus;
import com.eventsphere.enums.Role;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ApiResponse {

    private ApiResponse() {
    }

    @Getter
    @Setter
    public static class Success<T> {
        private boolean success = true;
        private String message;
        private T data;
        private LocalDateTime timestamp = LocalDateTime.now();

        public Success() {
        }

        public Success(boolean success, String message, T data, LocalDateTime timestamp) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.timestamp = timestamp;
        }

        public static <T> Success<T> of(String message, T data) {
            return new Success<>(true, message, data, LocalDateTime.now());
        }
    }

    @Getter
    @Setter
    public static class Error {
        private boolean success = false;
        private int status;
        private String message;
        private List<String> errors;
        private LocalDateTime timestamp = LocalDateTime.now();

        public Error() {
        }

        public Error(boolean success, int status, String message, List<String> errors, LocalDateTime timestamp) {
            this.success = success;
            this.status = status;
            this.message = message;
            this.errors = errors;
            this.timestamp = timestamp;
        }
    }

    @Getter
    @Setter
    public static class AuthData {
        private String accessToken;
        private String refreshToken;
        private boolean requiresTwoFactor;
        private UserData user;

        public AuthData() {
        }

        public AuthData(String accessToken, String refreshToken, boolean requiresTwoFactor, UserData user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.requiresTwoFactor = requiresTwoFactor;
            this.user = user;
        }
    }

    @Getter
    @Setter
    public static class UserData {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private Role role;
        private boolean enabled;
        private boolean twoFactorEnabled;

        public UserData() {
        }

        public UserData(Long id, String username, String email, String fullName, Role role, boolean enabled, boolean twoFactorEnabled) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.role = role;
            this.enabled = enabled;
            this.twoFactorEnabled = twoFactorEnabled;
        }
    }

    @Getter
    @Setter
    public static class EventData {
        private Long id;
        private String title;
        private String description;
        private LocalDateTime eventDate;
        private String location;
        private Integer capacity;
        private EventStatus status;
        private Long createdById;
        private String createdByName;
        private long registrationsCount;

        public EventData() {
        }

        public EventData(Long id, String title, String description, LocalDateTime eventDate, String location,
                         Integer capacity, EventStatus status, Long createdById, String createdByName, long registrationsCount) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.eventDate = eventDate;
            this.location = location;
            this.capacity = capacity;
            this.status = status;
            this.createdById = createdById;
            this.createdByName = createdByName;
            this.registrationsCount = registrationsCount;
        }
    }

    @Getter
    @Setter
    public static class StudentData {
        private Long id;
        private Long userId;
        private String username;
        private String email;
        private String fullName;
        private String studentId;
        private String department;
        private Integer yearOfStudy;
        private String phone;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public StudentData() {
        }

        public StudentData(Long id, Long userId, String username, String email, String fullName, String studentId,
                           String department, Integer yearOfStudy, String phone, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.studentId = studentId;
            this.department = department;
            this.yearOfStudy = yearOfStudy;
            this.phone = phone;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }

    @Getter
    @Setter
    public static class ChatMessageData {
        private Long id;
        private Long senderId;
        private String senderName;
        private Long receiverId;
        private String receiverName;
        private String content;
        private boolean isRead;
        private LocalDateTime sentAt;

        public ChatMessageData() {
        }

        public ChatMessageData(Long id, Long senderId, String senderName, Long receiverId, String receiverName,
                               String content, boolean isRead, LocalDateTime sentAt) {
            this.id = id;
            this.senderId = senderId;
            this.senderName = senderName;
            this.receiverId = receiverId;
            this.receiverName = receiverName;
            this.content = content;
            this.isRead = isRead;
            this.sentAt = sentAt;
        }
    }

    @Getter
    @Setter
    public static class DashboardStats {
        private long totalStudents;
        private long totalRegistrations;
        private long unreadMessages;
        private Map<EventStatus, Long> eventsByStatus;

        public DashboardStats() {
        }

        public DashboardStats(long totalStudents, long totalRegistrations, long unreadMessages, Map<EventStatus, Long> eventsByStatus) {
            this.totalStudents = totalStudents;
            this.totalRegistrations = totalRegistrations;
            this.unreadMessages = unreadMessages;
            this.eventsByStatus = eventsByStatus;
        }
    }
}
