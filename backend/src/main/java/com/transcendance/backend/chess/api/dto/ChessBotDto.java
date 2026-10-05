package com.transcendance.backend.chess.api.dto;

import com.transcendance.backend.chess.domain.ChessSide;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChessBotDto {

    @NotNull
    private ChessSide side;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer botLvl;
}
