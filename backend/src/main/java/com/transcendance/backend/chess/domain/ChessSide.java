package com.transcendance.backend.chess.domain;

import com.github.bhlangonijr.chesslib.Side;

public enum ChessSide {
    WHITE,
    BLACK;

    public static ChessSide from(Side side) {
        return side == Side.WHITE ? WHITE : BLACK;
    }

    public Side toLibSide() {
        return this == WHITE ? Side.WHITE : Side.BLACK;
    }

    public ChessSide opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}
