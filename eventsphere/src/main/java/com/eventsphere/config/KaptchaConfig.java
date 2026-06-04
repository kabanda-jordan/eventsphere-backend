package com.eventsphere.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KaptchaConfig {

    @Bean
    public DefaultKaptcha defaultKaptcha() {
        Properties properties = new Properties();
        properties.setProperty("kaptcha.border", "no");
        properties.setProperty("kaptcha.textproducer.font.color", "0,0,0");
        properties.setProperty("kaptcha.image.width", "160");
        properties.setProperty("kaptcha.image.height", "60");
        properties.setProperty("kaptcha.textproducer.font.size", "32");
        properties.setProperty("kaptcha.textproducer.char.length", "5");
        properties.setProperty("kaptcha.textproducer.font.names", "Arial,Courier");
        properties.setProperty("kaptcha.noise.color", "80,80,80");
        properties.setProperty("kaptcha.background.clear.from", "255,255,255");
        properties.setProperty("kaptcha.background.clear.to", "245,245,245");

        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        defaultKaptcha.setConfig(new Config(properties));
        return defaultKaptcha;
    }
}
