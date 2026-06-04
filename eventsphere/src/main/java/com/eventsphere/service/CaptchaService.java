package com.eventsphere.service;

import com.eventsphere.exception.GlobalExceptionHandler.InvalidCaptchaException;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Service
public class CaptchaService {

    private final DefaultKaptcha defaultKaptcha;
    private final CacheManager cacheManager;

    public CaptchaService(DefaultKaptcha defaultKaptcha, CacheManager cacheManager) {
        this.defaultKaptcha = defaultKaptcha;
        this.cacheManager = cacheManager;
    }

    public CaptchaData generateCaptcha() {
        String answer = defaultKaptcha.createText();
        BufferedImage image = defaultKaptcha.createImage(answer);
        String token = UUID.randomUUID().toString();
        getCaptchaCache().put(token, answer);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return new CaptchaData(token, outputStream.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate CAPTCHA image", ex);
        }
    }

    public void validateCaptchaOrThrow(String token, String answer) {
        if (!validate(token, answer)) {
            throw new InvalidCaptchaException();
        }
    }

    public boolean validate(String token, String answer) {
        Cache.ValueWrapper valueWrapper = getCaptchaCache().get(token);
        getCaptchaCache().evict(token);
        if (valueWrapper == null) {
            return false;
        }
        String expected = Objects.toString(valueWrapper.get(), "");
        return expected.equalsIgnoreCase(answer);
    }

    private Cache getCaptchaCache() {
        Cache cache = cacheManager.getCache("captchaCache");
        if (cache == null) {
            throw new IllegalStateException("captchaCache is not configured");
        }
        return cache;
    }

    public static class CaptchaData {
        private String token;
        private byte[] imageBytes;

        public CaptchaData(String token, byte[] imageBytes) {
            this.token = token;
            this.imageBytes = imageBytes;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public byte[] getImageBytes() {
            return imageBytes;
        }

        public void setImageBytes(byte[] imageBytes) {
            this.imageBytes = imageBytes;
        }
    }
}
