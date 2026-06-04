package com.eventsphere.service;

import com.eventsphere.dto.request.AuthRequest;
import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.entity.Student;
import com.eventsphere.entity.User;
import com.eventsphere.enums.Role;
import com.eventsphere.exception.GlobalExceptionHandler.ConflictException;
import com.eventsphere.repository.StudentRepository;
import com.eventsphere.repository.UserRepository;
import com.eventsphere.security.JwtTokenProvider;
import com.eventsphere.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthServiceImpl — covers registration, login, 2FA, and conflict scenarios.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CaptchaService captchaService;
    @Mock private OtpService otpService;

    @InjectMocks
    private AuthServiceImpl authService;

    private AuthRequest.Register validRegisterRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new AuthRequest.Register(
                "johndoe", "john@example.com", "John Doe",
                "Password1!", "token123", "ABCDE",
                "STU001", "Computer Science", 2, "+1234567890"
        );

        savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("johndoe");
        savedUser.setEmail("john@example.com");
        savedUser.setFullName("John Doe");
        savedUser.setRole(Role.STUDENT);
        savedUser.setEnabled(true);
        savedUser.setTwoFactorEnabled(false);
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setUpdatedAt(LocalDateTime.now());
    }

    // ─── Test 1: Successful registration ────────────────────────────────────────

    @Test
    @DisplayName("register() — success: creates user and student, returns tokens")
    void register_success() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(studentRepository.existsByStudentId("STU001")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("$2a$hashed");
        // save() is called twice: once for user creation, once for refresh token update
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(studentRepository.save(any(Student.class))).thenReturn(new Student());
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("access.token.here");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh.token.here");

        ApiResponse.AuthData result = authService.register(validRegisterRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access.token.here");
        assertThat(result.getRefreshToken()).isEqualTo("refresh.token.here");
        assertThat(result.isRequiresTwoFactor()).isFalse();
        assertThat(result.getUser().getUsername()).isEqualTo("johndoe");
        verify(userRepository).existsByUsername("johndoe");
        verify(studentRepository).existsByStudentId("STU001");
    }

    // ─── Test 2: Registration fails — duplicate username ────────────────────────

    @Test
    @DisplayName("register() — failure: throws ConflictException when username already exists")
    void register_duplicateUsername_throwsConflict() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    // ─── Test 3: Registration fails — duplicate email ───────────────────────────

    @Test
    @DisplayName("register() — failure: throws ConflictException when email already exists")
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    // ─── Test 4: Login triggers 2FA when enabled ────────────────────────────────

    @Test
    @DisplayName("login() — 2FA: returns requiresTwoFactor=true and sends OTP when 2FA is enabled")
    void login_twoFactorEnabled_returnsRequiresTwoFactor() {
        savedUser.setTwoFactorEnabled(true);

        AuthRequest.Login loginRequest = new AuthRequest.Login(
                "johndoe", "Password1!", "captchaToken", "ABCDE"
        );

        // Return a fully authenticated token (isAuthenticated() = true)
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "johndoe", "Password1!", List.of()
        );
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsernameOrEmail("johndoe", "johndoe"))
                .thenReturn(Optional.of(savedUser));

        ApiResponse.AuthData result = authService.login(loginRequest);

        assertThat(result.isRequiresTwoFactor()).isTrue();
        assertThat(result.getAccessToken()).isNull();
        assertThat(result.getUser().getUsername()).isEqualTo("johndoe");
        verify(otpService).generateAndSend(savedUser);
    }

    // ─── Test 5: Login without 2FA returns tokens directly ──────────────────────

    @Test
    @DisplayName("login() — success: returns tokens directly when 2FA is disabled")
    void login_noTwoFactor_returnsTokens() {
        savedUser.setTwoFactorEnabled(false);

        AuthRequest.Login loginRequest = new AuthRequest.Login(
                "johndoe", "Password1!", "captchaToken", "ABCDE"
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "johndoe", "Password1!", List.of()
        );
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsernameOrEmail("johndoe", "johndoe"))
                .thenReturn(Optional.of(savedUser));
        when(jwtTokenProvider.generateToken(savedUser)).thenReturn("access.jwt");
        when(jwtTokenProvider.generateRefreshToken(savedUser)).thenReturn("refresh.jwt");
        when(userRepository.save(any())).thenReturn(savedUser);

        ApiResponse.AuthData result = authService.login(loginRequest);

        assertThat(result.isRequiresTwoFactor()).isFalse();
        assertThat(result.getAccessToken()).isEqualTo("access.jwt");
        assertThat(result.getRefreshToken()).isEqualTo("refresh.jwt");
        verify(otpService, never()).generateAndSend(any());
    }
}
