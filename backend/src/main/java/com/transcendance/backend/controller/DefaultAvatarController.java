package com.transcendance.backend.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DefaultAvatarController {

    @GetMapping("/api/default-avatar.png")
    public ResponseEntity<Resource> getDefaultAvatar() {
        ClassPathResource avatar = new ClassPathResource("static/default-avatar.png");

        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(avatar);
    }
}
