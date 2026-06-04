package com.eventsphere.controller;

import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.entity.User;
import com.eventsphere.enums.Role;
import com.eventsphere.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * One-time setup endpoint.
 * - Seeds the admin user if missing
 * - Resets ALL user passwords to known values so you can log in
 * Safe to call multiple times.
 */
@Tag(name = "Setup", description = "One-time setup — seed and reset credentials")
@RestController
@RequestMapping("/api/setup")
public class SetupController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SetupController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(
        summary = "Seed admin + reset all passwords",
        description = "Creates admin if missing. Resets ALL user passwords: admin→Admin@1234, everyone else→Password@1234"
    )
    @PostMapping("/seed-admin")
    public ResponseEntity<ApiResponse.Success<List<String>>> seedAdmin() {
        List<String> results = new ArrayList<>();

        // ── Create admin if not present ──────────────────────────────────────
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@eventsphere.com");
            admin.setFullName("System Admin");
            admin.setPassword(passwordEncoder.encode("Admin@1234"));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            admin.setTwoFactorEnabled(false);
            userRepository.save(admin);
            results.add("✅ Created admin user");
        } else {
            results.add("ℹ️  Admin already exists");
        }

        // ── Reset ALL passwords to known values ──────────────────────────────
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if ("admin".equals(user.getUsername())) {
                user.setPassword(passwordEncoder.encode("Admin@1234"));
                userRepository.save(user);
                results.add("🔑 Reset password → admin / Admin@1234");
            } else {
                user.setPassword(passwordEncoder.encode("Password@1234"));
                userRepository.save(user);
                results.add("🔑 Reset password → " + user.getUsername() + " / Password@1234");
            }
        }

        results.add("─────────────────────────────────────");
        results.add("Login at /api/auth/login (need captcha first)");
        results.add("Admin:   username=admin      password=Admin@1234");
        results.add("Others:  username=<username> password=Password@1234");

        return ResponseEntity.ok(ApiResponse.Success.of("Setup complete", results));
    }
}
