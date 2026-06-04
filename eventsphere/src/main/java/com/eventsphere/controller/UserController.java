package com.eventsphere.controller;

import com.eventsphere.dto.request.CreateUserRequest;
import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.entity.Student;
import com.eventsphere.entity.User;
import com.eventsphere.enums.Role;
import com.eventsphere.exception.GlobalExceptionHandler.ConflictException;
import com.eventsphere.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.eventsphere.repository.StudentRepository;
import com.eventsphere.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Users", description = "User management — ADMIN only (except /me)")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository,
                          StudentRepository studentRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── GET /api/users ────────────────────────────────────────────────────────
    @Operation(
        summary = "List all users",
        security = @SecurityRequirement(name = "bearerAuth"),
        description = "Paginated list of all users. Filter by role or search by username/email/name. ADMIN only."
    )
    @GetMapping
    public ResponseEntity<ApiResponse.Success<Map<String, Object>>> getUsers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")             @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search by username, email or full name") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by role: ADMIN or STUDENT") @RequestParam(required = false) Role role
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<User> result = (search != null && !search.isBlank()) || role != null
                ? userRepository.search(search, role, pageable)
                : userRepository.findAll(pageable);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content",       result.getContent().stream().map(this::toUserData).toList());
        data.put("page",          result.getNumber());
        data.put("size",          result.getSize());
        data.put("totalElements", result.getTotalElements());
        data.put("totalPages",    result.getTotalPages());
        data.put("last",          result.isLast());

        return ResponseEntity.ok(ApiResponse.Success.of("Users fetched successfully", data));
    }

    // ── GET /api/users/me ─────────────────────────────────────────────────────
    @Operation(
        summary = "Get my profile",
        security = @SecurityRequirement(name = "bearerAuth"),
        description = "Returns the profile of the currently authenticated user."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse.Success<ApiResponse.UserData>> me(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(ApiResponse.Success.of("Profile fetched successfully", toUserData(user)));
    }

    // ── GET /api/users/{id} ───────────────────────────────────────────────────
    @Operation(
        summary = "Get user by ID",
        security = @SecurityRequirement(name = "bearerAuth"),
        description = "Returns a single user by their ID. ADMIN only."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse.Success<ApiResponse.UserData>> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return ResponseEntity.ok(ApiResponse.Success.of("User fetched successfully", toUserData(user)));
    }

    // ── POST /api/users ───────────────────────────────────────────────────────
    @Operation(
        summary = "Create a new user",
        security = @SecurityRequirement(name = "bearerAuth"),
        description = "Admin creates a new user directly — no CAPTCHA needed. " +
                      "Set role to ADMIN or STUDENT. If STUDENT, studentId is required."
    )
    @PostMapping
    public ResponseEntity<ApiResponse.Success<ApiResponse.UserData>> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        // Check uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email '" + request.getEmail() + "' already exists");
        }
        if (request.getRole() == Role.STUDENT
                && request.getStudentId() != null
                && studentRepository.existsByStudentId(request.getStudentId())) {
            throw new ConflictException("Student ID '" + request.getStudentId() + "' already exists");
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEnabled(true);
        user.setTwoFactorEnabled(false);
        user = userRepository.save(user);

        // If student role, create student profile too
        if (request.getRole() == Role.STUDENT && request.getStudentId() != null) {
            Student student = new Student();
            student.setUser(user);
            student.setStudentId(request.getStudentId());
            student.setDepartment(request.getDepartment());
            student.setYearOfStudy(request.getYearOfStudy());
            student.setPhone(request.getPhone());
            studentRepository.save(student);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of("User created successfully", toUserData(user)));
    }

    // ── PUT /api/users/{id} ───────────────────────────────────────────────────
    @Operation(
        summary = "Update a user",
        security = @SecurityRequirement(name = "bearerAuth"),
        description = "Update fullName, email, role, or enabled status. ADMIN only."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse.Success<ApiResponse.UserData>> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getFullName() != null)  user.setFullName(request.getFullName());
        if (request.getEmail() != null)     user.setEmail(request.getEmail());
        if (request.getRole() != null)      user.setRole(request.getRole());
        if (request.getEnabled() != null)   user.setEnabled(request.getEnabled());

        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.Success.of("User updated successfully", toUserData(user)));
    }

    // ── DELETE /api/users/{id} ────────────────────────────────────────────────
    @Operation(
        summary = "Delete a user",
        security = @SecurityRequirement(name = "bearerAuth"),
        description = "Permanently deletes a user. ADMIN only."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse.Success<Void>> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
        return ResponseEntity.ok(ApiResponse.Success.of("User deleted successfully", null));
    }

    // ── helper ────────────────────────────────────────────────────────────────
    private ApiResponse.UserData toUserData(User user) {
        ApiResponse.UserData d = new ApiResponse.UserData();
        d.setId(user.getId());
        d.setUsername(user.getUsername());
        d.setEmail(user.getEmail());
        d.setFullName(user.getFullName());
        d.setRole(user.getRole());
        d.setEnabled(user.isEnabled());
        d.setTwoFactorEnabled(user.isTwoFactorEnabled());
        return d;
    }

    // ── inner DTO for update ──────────────────────────────────────────────────
    public static class UpdateUserRequest {
        private String fullName;
        private String email;
        private Role role;
        private Boolean enabled;

        public String getFullName()       { return fullName; }
        public void setFullName(String v) { this.fullName = v; }
        public String getEmail()          { return email; }
        public void setEmail(String v)    { this.email = v; }
        public Role getRole()             { return role; }
        public void setRole(Role v)       { this.role = v; }
        public Boolean getEnabled()       { return enabled; }
        public void setEnabled(Boolean v) { this.enabled = v; }
    }
}
