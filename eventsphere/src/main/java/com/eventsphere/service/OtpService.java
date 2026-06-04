package com.eventsphere.service;

import com.eventsphere.entity.User;
import com.eventsphere.exception.GlobalExceptionHandler.InvalidOtpException;
import com.eventsphere.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.logging.Logger;

@Service
public class OtpService {

    private static final Logger LOGGER = Logger.getLogger(OtpService.class.getName());

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    public OtpService(UserRepository userRepository, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    @Value("${otp.expiry-minutes:5}")
    private long expiryMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${spring.mail.username:noreply@eventsphere.com}")
    private String fromEmail;

    // ── Public API ────────────────────────────────────────────

    @Transactional
    public void generateAndSend(User user) {
        String code = generateOtp();
        user.setTwoFactorCode(code);
        user.setTwoFactorExpiry(LocalDateTime.now().plusMinutes(expiryMinutes));
        userRepository.save(user);
        sendOtpEmail(user.getEmail(), user.getFullName(), code);
    }

    @Transactional(readOnly = true)
    public void validateOrThrow(User user, String otpCode) {
        if (!validate(user, otpCode)) {
            throw new InvalidOtpException();
        }
    }

    @Transactional
    public boolean validate(User user, String otpCode) {
        boolean valid = user.getTwoFactorCode() != null
                && user.getTwoFactorExpiry() != null
                && user.getTwoFactorCode().equals(otpCode)
                && LocalDateTime.now().isBefore(user.getTwoFactorExpiry());

        if (valid) {
            user.setTwoFactorCode(null);
            user.setTwoFactorExpiry(null);
            userRepository.save(user);
        }
        return valid;
    }

    // ── OTP generation ────────────────────────────────────────

    private String generateOtp() {
        Random random = new Random();
        int upper = (int) Math.pow(10, otpLength);
        int lower = (int) Math.pow(10, otpLength - 1);
        return String.valueOf(lower + random.nextInt(upper - lower));
    }

    // ── Email sending ─────────────────────────────────────────

    @Async
    public void sendOtpEmail(String toEmail, String fullName, String otpCode) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setFrom(fromEmail, "EventSphere");
            helper.setTo(toEmail);
            helper.setSubject("Your EventSphere verification code: " + otpCode);
            helper.setText(buildPlainText(fullName, otpCode), buildHtml(fullName, otpCode));

            mailSender.send(mime);
            LOGGER.info("OTP email sent to: " + toEmail);

        } catch (Exception ex) {
            LOGGER.warning("Failed to send OTP email to " + toEmail + ": " + ex.getMessage());
            LOGGER.warning(">>> DEV FALLBACK — OTP for " + toEmail + " is: " + otpCode + " <<<");
        }
    }

    // ── Plain-text fallback ───────────────────────────────────

    private String buildPlainText(String fullName, String otpCode) {
        return "Hi " + fullName + ",\n\n"
             + "Your EventSphere verification code is: " + otpCode + "\n\n"
             + "This code expires in " + expiryMinutes + " minutes.\n"
             + "If you didn't request this, you can safely ignore this email.\n\n"
             + "— The EventSphere Team";
    }

    // ── HTML email template ───────────────────────────────────

    private String buildHtml(String fullName, String otpCode) {
        // Split the 6-digit code into individual characters for the big digit boxes
        String[] digits = otpCode.split("");
        StringBuilder digitBoxes = new StringBuilder();
        for (String d : digits) {
            digitBoxes.append(
                "<td style=\"padding:0 4px;\">" +
                "<div style=\"" +
                    "width:48px;height:56px;" +
                    "background:#f4f0ff;" +
                    "border:2px solid #7c3aed;" +
                    "border-radius:10px;" +
                    "font-size:28px;font-weight:800;" +
                    "color:#7c3aed;" +
                    "text-align:center;line-height:56px;" +
                    "font-family:'Courier New',monospace;" +
                "\">" + d + "</div></td>"
            );
        }

        return "<!DOCTYPE html>" +
        "<html lang=\"en\">" +
        "<head>" +
          "<meta charset=\"UTF-8\"/>" +
          "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>" +
          "<title>Your EventSphere Code</title>" +
        "</head>" +
        "<body style=\"margin:0;padding:0;background:#f8fafc;font-family:'Inter',Arial,sans-serif;\">" +

          // Outer wrapper
          "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f8fafc;padding:40px 16px;\">" +
          "<tr><td align=\"center\">" +

            // Card
            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"" +
              "max-width:520px;" +
              "background:#ffffff;" +
              "border-radius:16px;" +
              "border:1px solid #e4e4e7;" +
              "box-shadow:0 4px 24px rgba(0,0,0,0.08);" +
            "\">" +

              // Header bar
              "<tr><td style=\"" +
                "background:linear-gradient(135deg,#7c3aed 0%,#6d28d9 100%);" +
                "border-radius:16px 16px 0 0;" +
                "padding:32px 40px 28px;" +
                "text-align:center;" +
              "\">" +
                // Logo mark
                "<div style=\"" +
                  "display:inline-block;" +
                  "width:48px;height:48px;" +
                  "background:rgba(255,255,255,0.2);" +
                  "border-radius:12px;" +
                  "margin-bottom:16px;" +
                  "line-height:48px;font-size:24px;" +
                "\">&#128197;</div>" +
                "<div style=\"font-size:22px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;\">EventSphere</div>" +
                "<div style=\"font-size:13px;color:rgba(255,255,255,0.75);margin-top:4px;\">Verification Code</div>" +
              "</td></tr>" +

              // Body
              "<tr><td style=\"padding:36px 40px 32px;\">" +

                // Greeting
                "<p style=\"margin:0 0 8px;font-size:20px;font-weight:700;color:#09090b;letter-spacing:-0.3px;\">" +
                  "Hi " + escapeHtml(fullName) + " \uD83D\uDC4B" +
                "</p>" +
                "<p style=\"margin:0 0 28px;font-size:15px;color:#52525b;line-height:1.6;\">" +
                  "Use the verification code below to complete your sign-in. " +
                  "It expires in <strong style=\"color:#09090b;\">" + expiryMinutes + " minutes</strong>." +
                "</p>" +

                // Code label
                "<p style=\"margin:0 0 12px;font-size:11px;font-weight:700;color:#a1a1aa;" +
                  "text-transform:uppercase;letter-spacing:0.1em;\">Your verification code</p>" +

                // Digit boxes
                "<table cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 28px;\">" +
                "<tr>" + digitBoxes + "</tr>" +
                "</table>" +

                // Expiry note
                "<div style=\"" +
                  "background:#f4f0ff;" +
                  "border:1px solid #ddd6fe;" +
                  "border-radius:10px;" +
                  "padding:14px 18px;" +
                  "margin-bottom:28px;" +
                "\">" +
                  "<p style=\"margin:0;font-size:13px;color:#6d28d9;\">" +
                    "&#9203; This code is valid for <strong>" + expiryMinutes + " minutes</strong> " +
                    "and can only be used once." +
                  "</p>" +
                "</div>" +

                // Security note
                "<p style=\"margin:0;font-size:13px;color:#a1a1aa;line-height:1.6;\">" +
                  "If you didn't request this code, you can safely ignore this email. " +
                  "Someone may have entered your email by mistake." +
                "</p>" +

              "</td></tr>" +

              // Footer
              "<tr><td style=\"" +
                "border-top:1px solid #e4e4e7;" +
                "padding:20px 40px;" +
                "text-align:center;" +
              "\">" +
                "<p style=\"margin:0;font-size:12px;color:#a1a1aa;\">" +
                  "Sent by <strong style=\"color:#7c3aed;\">EventSphere</strong> &nbsp;&middot;&nbsp; " +
                  "Do not reply to this email" +
                "</p>" +
              "</td></tr>" +

            "</table>" + // end card

          "</td></tr>" +
          "</table>" + // end outer wrapper

        "</body></html>";
    }

    // ── Utility ───────────────────────────────────────────────

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
