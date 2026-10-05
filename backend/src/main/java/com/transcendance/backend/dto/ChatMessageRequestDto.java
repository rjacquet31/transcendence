package com.transcendance.backend.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageRequestDto {

    @NotBlank
    @Size(max = 1000, message = "Le message doit faire moins de 1000 caractères")
    private String content;

    @NotNull
    private UUID receiverId;
}
