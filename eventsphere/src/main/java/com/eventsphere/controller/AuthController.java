package com.eventsphere.controller;

import com.eventsphere.dto.request.AuthRequest;
import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.entity.User;
import com.eventsphere.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.eventsphere.repository.UserRepository;
import com.eventsphere.service.CaptchaService;
import com.eventsphere.service.OtpService;
import com.eventsphere.service.impl.AuthServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Authentication and account management endpoints")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CaptchaService captchaService;
    private final AuthServiceImpl authService;
    private final OtpService otpService;
    private final UserRepository userRepository;

    public AuthController(CaptchaService captchaService, AuthServiceImpl authService,
                          OtpService otpService, UserRepository userRepository) {
        this.captchaService = captchaService;
        this.authService = authService;
        this.otpService = otpService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get CAPTCHA image",
               description = "Returns a PNG CAPTCHA image. The captcha token is returned in the X-Captcha-Token response header. Use this token and the answer when calling /login or /register.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CAPTCHA image returned successfully")
    })
    @GetMapping(value = "/captcha", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getCaptcha() {
        CaptchaService.CaptchaData captcha = captchaService.generateCaptcha();
        return ResponseEntity.ok()
                .header("X-Captcha-Token", captcha.getToken())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("captcha.png").build().toString())
                .contentType(MediaType.IMAGE_PNG)
                .body(captcha.getImageBytes());
    }

    @Operation(summary = "Register a new student account",
               description = "Creates a new student user. Requires a valid CAPTCHA token and answer obtained from GET /api/auth/captcha.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registration successful, tokens returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid CAPTCHA",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username, email, or student ID already exists",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse.Success<ApiResponse.AuthData>> register(@Valid @RequestBody AuthRequest.Register request) {
        return ResponseEntity.ok(ApiResponse.Success.of("Registration successful", authService.register(request)));
    }

    @Operation(summary = "Login (no CAPTCHA — for Postman/API testing)",
               description = "Login with just username and password. No CAPTCHA required. " +
                             "Use this endpoint in Postman. Returns JWT accessToken.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @PostMapping("/login-dev")
    public ResponseEntity<ApiResponse.Success<ApiResponse.AuthData>> loginDev(
            @RequestBody AuthRequest.LoginDev request) {
        return ResponseEntity.ok(ApiResponse.Success.of("Login successful", authService.loginDev(request)));
    }

    @Operation(summary = "Login",
               description = "Authenticate with username/email and password. Requires a valid CAPTCHA. If 2FA is enabled, returns requiresTwoFactor=true and sends an OTP to the user's email — call /verify-2fa next.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful or 2FA required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid CAPTCHA",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse.Success<ApiResponse.AuthData>> login(@Valid @RequestBody AuthRequest.Login request) {
        ApiResponse.AuthData data = authService.login(request);
        String message = data.isRequiresTwoFactor() ? "OTP sent to your email" : "Login successful";
        return ResponseEntity.ok(ApiResponse.Success.of(message, data));
    }

    @Operation(summary = "Send OTP to email",
               description = "Generates a fresh 6-digit OTP and sends it to the email address of the given username or email. " +
                             "The code expires in 5 minutes. Check the Spring Boot console if email delivery fails (dev fallback logs the code there).")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error — usernameOrEmail is blank",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No account found for the given username/email",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Username or email of the account to send the OTP to",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = SendOtpRequest.class),
            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                name = "Send OTP to admin",
                value = "{\"usernameOrEmail\": \"admin\"}"
            )
        )
    )
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse.Success<Void>> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found for: " + request.getUsernameOrEmail()));
        otpService.generateAndSend(user);
        return ResponseEntity.ok(ApiResponse.Success.of(
                "OTP sent to " + maskEmail(user.getEmail()), null));
    }

    @Operation(summary = "Verify 2FA OTP",
               description = "Submit the OTP code received by email to complete 2FA login. Returns access and refresh tokens on success.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "2FA verified, tokens returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired OTP",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @PostMapping("/verify-2fa")
    public ResponseEntity<ApiResponse.Success<ApiResponse.AuthData>> verify2fa(@Valid @RequestBody AuthRequest.Verify2FA request) {
        return ResponseEntity.ok(ApiResponse.Success.of("2FA verification successful", authService.verify2FA(request)));
    }

    @Operation(summary = "Refresh access token",
               description = "Exchange a valid refresh token for a new access token and refresh token pair.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired refresh token",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse.Success<ApiResponse.AuthData>> refresh(@Valid @RequestBody AuthRequest.RefreshToken request) {
        return ResponseEntity.ok(ApiResponse.Success.of("Token refreshed successfully", authService.refreshToken(request)));
    }

    @Operation(summary = "Change password", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Change the authenticated user's password. Requires the current password for verification.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Current password incorrect or validation error",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse.Success<Void>> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                                                    @Valid @RequestBody AuthRequest.ChangePassword request) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.Success.of("Password changed successfully", null));
    }

    @Operation(summary = "Enable 2FA", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Enable two-factor authentication for the authenticated user.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "2FA enabled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @PatchMapping("/2fa/enable")
    public ResponseEntity<ApiResponse.Success<ApiResponse.UserData>> enableTwoFactor(@AuthenticationPrincipal UserDetails userDetails) {
        authService.toggleTwoFactor(userDetails.getUsername(), true);
        return ResponseEntity.ok(ApiResponse.Success.of("Two-factor authentication enabled",
                authService.currentUser(userDetails.getUsername())));
    }

    @Operation(summary = "Disable 2FA", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Disable two-factor authentication for the authenticated user.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "2FA disabled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @PatchMapping("/2fa/disable")
    public ResponseEntity<ApiResponse.Success<ApiResponse.UserData>> disableTwoFactor(@AuthenticationPrincipal UserDetails userDetails) {
        authService.toggleTwoFactor(userDetails.getUsername(), false);
        return ResponseEntity.ok(ApiResponse.Success.of("Two-factor authentication disabled",
                authService.currentUser(userDetails.getUsername())));
    }

    @Operation(summary = "Logout", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Invalidates the user's refresh token. The access token remains valid until it expires.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse.Success<Void>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            authService.logout(userDetails.getUsername());
        }
        return ResponseEntity.ok(ApiResponse.Success.of("Logout successful", null));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Masks an email for safe display: john@example.com → j***@example.com */
    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "your email";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String masked = local.charAt(0) + "***";
        return masked + "@" + parts[1];
    }

    // ── inner request DTO ─────────────────────────────────────────────────────

    /** Request body for POST /api/auth/send-otp */
    @Schema(description = "Request body for sending an OTP code to a user's email")
    public static class SendOtpRequest {

        @Schema(description = "Username or email address of the account", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "usernameOrEmail: must not be blank")
        private String usernameOrEmail;

        public SendOtpRequest() {}
        public SendOtpRequest(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }

        public String getUsernameOrEmail() { return usernameOrEmail; }
        public void setUsernameOrEmail(String v) { this.usernameOrEmail = v; }
    }
}
