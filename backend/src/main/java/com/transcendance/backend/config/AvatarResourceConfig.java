package com.transcendance.backend.config;

import java.nio.file.Path;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AvatarResourceConfig implements WebMvcConfigurer {

    private final AvatarProperties avatarProperties;

    public AvatarResourceConfig(AvatarProperties avatarProperties) {
        this.avatarProperties = avatarProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = Path.of(avatarProperties.getStorageDir())
                .toAbsolutePath()
                .normalize()
                .toString();

        String location = "file:" + (dir.endsWith("/") ? dir : dir + "/");

        registry.addResourceHandler(avatarProperties.getPublicPath() + "/**")
                .addResourceLocations(location);
    }
}
