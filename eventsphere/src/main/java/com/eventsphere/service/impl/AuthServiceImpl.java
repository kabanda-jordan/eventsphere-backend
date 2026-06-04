package com.eventsphere.service.impl;

import com.eventsphere.dto.request.AuthRequest;
import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.entity.Student;
import com.eventsphere.entity.User;
import com.eventsphere.enums.Role;
import com.eventsphere.exception.GlobalExceptionHandler.BadRequestException;
import com.eventsphere.exception.GlobalExceptionHandler.ConflictException;
import com.eventsphere.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.eventsphere.repository.StudentRepository;
import com.eventsphere.repository.UserRepository;
import com.eventsphere.security.JwtTokenProvider;
import com.eventsphere.service.CaptchaService;
import com.eventsphere.service.OtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthServiceImpl {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final CaptchaService captchaService;
    private final OtpService otpService;

    public AuthServiceImpl(UserRepository userRepository, StudentRepository studentRepository, PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider, AuthenticationManager authenticationManager,
                           CaptchaService captchaService, OtpService otpService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.captchaService = captchaService;
        this.otpService = otpService;
    }

    @Transactional
    public ApiResponse.AuthData register(AuthRequest.Register request) {
        captchaService.validateCaptchaOrThrow(request.getCaptchaToken(), request.getCaptchaAnswer());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }
        if (studentRepository.existsByStudentId(request.getStudentId())) {
            throw new ConflictException("Student ID already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.STUDENT);
        user.setEnabled(true);
        user.setTwoFactorEnabled(false);
        user = userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentId(request.getStudentId());
        student.setDepartment(request.getDepartment());
        student.setYearOfStudy(request.getYearOfStudy());
        student.setPhone(request.getPhone());
        studentRepository.save(student);

        return issueTokens(user);
    }

    @Transactional
    public ApiResponse.AuthData loginDev(AuthRequest.LoginDev request) {
        // Authenticate without captcha — for Postman/API testing
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(), request.getPassword())
        );
        User user = userRepository.findByUsernameOrEmail(
                        request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return issueTokens(user);
    }

    @Transactional
    public ApiResponse.AuthData login(AuthRequest.Login request) {
        captchaService.validateCaptchaOrThrow(request.getCaptchaToken(), request.getCaptchaAnswer());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!authentication.isAuthenticated()) {
            throw new BadRequestException("Authentication failed");
        }

        if (user.isTwoFactorEnabled()) {
            otpService.generateAndSend(user);
            ApiResponse.AuthData authData = new ApiResponse.AuthData();
            authData.setRequiresTwoFactor(true);
            authData.setUser(toUserData(user));
            return authData;
        }

        return issueTokens(user);
    }

    @Transactional
    public ApiResponse.AuthData verify2FA(AuthRequest.Verify2FA request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        otpService.validateOrThrow(user, request.getOtpCode());
        return issueTokens(user);
    }

    @Transactional
    public ApiResponse.AuthData refreshToken(AuthRequest.RefreshToken request) {
        User user = userRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new BadRequestException("Refresh token expired or invalid");
        }

        String tokenUsername = jwtTokenProvider.extractUsername(request.getRefreshToken());
        if (!user.getUsername().equals(tokenUsername)) {
            throw new BadRequestException("Refresh token does not match user");
        }

        return issueTokens(user);
    }

    @Transactional
    public void changePassword(String username, AuthRequest.ChangePassword request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void toggleTwoFactor(String username, boolean enabled) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setTwoFactorEnabled(enabled);
        if (!enabled) {
            user.setTwoFactorCode(null);
            user.setTwoFactorExpiry(null);
        }
        userRepository.save(user);
    }

    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRefreshToken(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public ApiResponse.UserData currentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserData(user);
    }

    private ApiResponse.AuthData issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        ApiResponse.AuthData authData = new ApiResponse.AuthData();
        authData.setAccessToken(accessToken);
        authData.setRefreshToken(refreshToken);
        authData.setRequiresTwoFactor(false);
        authData.setUser(toUserData(user));
        return authData;
    }

    private ApiResponse.UserData toUserData(User user) {
        ApiResponse.UserData data = new ApiResponse.UserData();
        data.setId(user.getId());
        data.setUsername(user.getUsername());
        data.setEmail(user.getEmail());
        data.setFullName(user.getFullName());
        data.setRole(user.getRole());
        data.setEnabled(user.isEnabled());
        data.setTwoFactorEnabled(user.isTwoFactorEnabled());
        return data;
    }
}
