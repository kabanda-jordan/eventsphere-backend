package com.eventsphere.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public final class AuthRequest {

    private AuthRequest() {
    }

    @Getter
    @Setter
    public static class Login {
        @NotBlank(message = "usernameOrEmail: must not be blank")
        private String usernameOrEmail;
        @NotBlank(message = "password: must not be blank")
        private String password;
        @NotBlank(message = "captchaToken: must not be blank")
        private String captchaToken;
        @NotBlank(message = "captchaAnswer: must not be blank")
        private String captchaAnswer;

        public Login() {
        }

        public Login(String usernameOrEmail, String password, String captchaToken, String captchaAnswer) {
            this.usernameOrEmail = usernameOrEmail;
            this.password = password;
            this.captchaToken = captchaToken;
            this.captchaAnswer = captchaAnswer;
        }
    }

    @Getter
    @Setter
    public static class Register {
        @NotBlank(message = "username: must not be blank")
        @Size(min = 3, max = 50, message = "username: size must be between 3 and 50 characters")
        private String username;
        @NotBlank(message = "email: must not be blank")
        @Email(message = "email: must be a well-formed email address")
        private String email;
        @NotBlank(message = "fullName: must not be blank")
        private String fullName;
        @NotBlank(message = "password: must not be blank")
        @Size(min = 8, message = "password: must be at least 8 characters")
        private String password;
        @NotBlank(message = "captchaToken: must not be blank")
        private String captchaToken;
        @NotBlank(message = "captchaAnswer: must not be blank")
        private String captchaAnswer;
        @NotBlank(message = "studentId: must not be blank")
        private String studentId;
        private String department;
        private Integer yearOfStudy;
        private String phone;

        public Register() {
        }

        public Register(String username, String email, String fullName, String password, String captchaToken,
                        String captchaAnswer, String studentId, String department, Integer yearOfStudy, String phone) {
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.password = password;
            this.captchaToken = captchaToken;
            this.captchaAnswer = captchaAnswer;
            this.studentId = studentId;
            this.department = department;
            this.yearOfStudy = yearOfStudy;
            this.phone = phone;
        }
    }

    @Getter
    @Setter
    public static class Verify2FA {
        @NotBlank(message = "username: must not be blank")
        private String username;
        @NotBlank(message = "otpCode: must not be blank")
        private String otpCode;

        public Verify2FA() {
        }

        public Verify2FA(String username, String otpCode) {
            this.username = username;
            this.otpCode = otpCode;
        }
    }

    @Getter
    @Setter
    public static class RefreshToken {
        @NotBlank(message = "refreshToken: must not be blank")
        private String refreshToken;

        public RefreshToken() {
        }

        public RefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    @Getter
    @Setter
    public static class ChangePassword {
        @NotBlank(message = "currentPassword: must not be blank")
        private String currentPassword;
        @NotBlank(message = "newPassword: must not be blank")
        @Size(min = 8, message = "newPassword: must be at least 8 characters")
        private String newPassword;

        public ChangePassword() {
        }

        public ChangePassword(String currentPassword, String newPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }
    }

    /**
     * Captcha-free login — for Postman / API testing only.
     * Just username/email + password, no CAPTCHA required.
     */
    @Getter
    @Setter
    public static class LoginDev {
        @NotBlank(message = "usernameOrEmail: must not be blank")
        private String usernameOrEmail;
        @NotBlank(message = "password: must not be blank")
        private String password;

        public LoginDev() {}

        public LoginDev(String usernameOrEmail, String password) {
            this.usernameOrEmail = usernameOrEmail;
            this.password = password;
        }
    }
}
