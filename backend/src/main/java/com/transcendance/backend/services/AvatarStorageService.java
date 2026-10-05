package com.transcendance.backend.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.transcendance.backend.config.AvatarProperties;

@Service
public class AvatarStorageService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(AvatarStorageService.class);

    private static final Set<String> ALLOWED_FORMATS = Set.of("png", "jpeg", "jpg");

    private final AvatarProperties avatarProperties;

    public AvatarStorageService(AvatarProperties avatarProperties) {
        this.avatarProperties = avatarProperties;
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        if (file.getSize() > avatarProperties.getMaxSizeBytes()) {
            throw new IllegalArgumentException("Le fichier est trop volumineux");
        }

        String extension = detectImageFormat(file);
        String filename = UUID.randomUUID() + "." + extension;

        Path storageDir = Path.of(avatarProperties.getStorageDir());
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage directory", e);
        }

        Path target = storageDir.resolve(filename).normalize();

        if (!target.startsWith(storageDir.normalize())) {
            throw new IllegalStateException("Invalid destination path.");
        }

        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store the file.", e);
        }

        return filename;
    }

    public void delete(String filename) {
        if (filename == null || filename.isBlank()) return;

        Path target = Path.of(avatarProperties.getStorageDir()).resolve(filename).normalize();
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete avatar file: {}", filename, e);
        }
    }

    private String detectImageFormat(MultipartFile file) {
        try (InputStream rawIn = file.getInputStream();
             ImageInputStream iis = ImageIO.createImageInputStream(rawIn)) {

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Le fichier n'est pas une image valide.");
            }

            String format = readers.next().getFormatName().toLowerCase();

            if (!ALLOWED_FORMATS.contains(format)) {
                throw new IllegalArgumentException("Format d'image non autorisé. Formats acceptés : " + ALLOWED_FORMATS);
            }

            return format.equals("jpeg") ? "jpg" : format;

        } catch (IOException e) {
            throw new IllegalArgumentException("Impossible de lire l'image.", e);
        }
    }
}
