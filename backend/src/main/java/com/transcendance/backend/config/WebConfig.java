package com.transcendance.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AvatarProperties avatarProperties;

    public WebConfig(AvatarProperties avatarProperties) {
        this.avatarProperties = avatarProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path storageDir = Path.of(avatarProperties.getStorageDir()).toAbsolutePath();
        String location = "file:" + storageDir + "/";

        registry.addResourceHandler(avatarProperties.getPublicPath() + "/**")
                .addResourceLocations(location);
    }
}
