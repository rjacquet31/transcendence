package com.transcendance.backend.chess.domain;

import java.util.UUID;

/**
 * Everything the chess module can refuse to do, for the whole module: matches,
 * moves and invitations alike.
 *
 * One type per outcome, so the api layer can give each its own HTTP status
 * without parsing error messages - a match that does not exist and a move that
 * is not yours are not the same failure, and a client should not have to read
 * English to tell them apart.
 *
 * Deliberately unaware of HTTP: the domain states what went wrong,
 * ChessExceptionHandler decides how to phrase it on the wire.
 */
public abstract class ChessException extends RuntimeException {

    protected ChessException(String message) {
        super(message);
    }

    // ---------------------------------------------------------------- missing

    /**
     * No match carries this id.
     */
    public static class MatchNotFound extends ChessException {
        public MatchNotFound(long matchId) {
            super("Match " + matchId + " not found");
        }
    }

    /**
     * No match carries this id.
     */
    public static class MatchNotFoundByUserId extends ChessException {
        public MatchNotFoundByUserId(UUID userId) {
            super("Aucune partie trouvée pour l'utilisateur " + userId);
        }
    }
    /**
     * No user carries this username.
     */
    public static class OpponentNotFound extends ChessException {
        public OpponentNotFound(String username) {
            super("Adversaire introuvable : " + username);
        }
    }

    /**
     * This player never invited the caller, or the invitation is already gone.
     */
    public static class InvitationNotFound extends ChessException {
        public InvitationNotFound() {
            super("Aucune invitation en attente de ce joueur");
        }
    }

    // -------------------------------------------------------------- forbidden

    /**
     * The match exists, but this user is neither of its two players. Distinct
     * from NotYourTurn: this one is never going to become allowed.
     */
    public static class NotAPlayer extends ChessException {
        public NotAPlayer() {
            super("Tu ne participes pas à cette partie");
        }
    }

    /**
     * Invitations are limited to friends.
     */
    public static class NotFriends extends ChessException {
        public NotFriends() {
            super("Tu n'es pas ami avec cet utilisateur");
        }
    }

    // --------------------------------------------------------------- conflict

    /**
     * The match is already decided, so no further move can be recorded.
     */
    public static class MatchAlreadyOver extends ChessException {
        public MatchAlreadyOver(ChessMatchStatus status) {
            super("La partie est déjà terminée : " + status);
        }
    }

    /**
     * The user is a player of this match, but the other side has to move first.
     */
    public static class NotYourTurn extends ChessException {
        public NotYourTurn() {
            super("Ce n'est pas ton tour");
        }
    }

    /**
     * A player can only be in one match at a time. Carries its own wording,
     * since the blocker may be the caller or his opponent.
     */
    public static class AlreadyInMatch extends ChessException {
        public AlreadyInMatch(String message) {
            super(message);
        }
    }

    /**
     * Both players must hold a live session for a match to start.
     */
    public static class OpponentOffline extends ChessException {
        public OpponentOffline() {
            super("L'adversaire n'est pas en ligne");
        }
    }

    /**
     * Another call accepted or declined this invitation first.
     */
    public static class InvitationAlreadyHandled extends ChessException {
        public InvitationAlreadyHandled() {
            super("L'invitation a déjà été traitée");
        }
    }

    /**
     * The invitation timed out before it was answered.
     */
    public static class InvitationExpired extends ChessException {
        public InvitationExpired() {
            super("L'invitation a expiré");
        }
    }

    /**
     * One invitation at a time towards the same player.
     */
    public static class DuplicateInvitation extends ChessException {
        public DuplicateInvitation() {
            super("Tu as déjà une invitation en attente pour ce joueur");
        }
    }

    // ------------------------------------------------------------ bad request

    /**
     * The move is not even in UCI notation, so no position was consulted.
     */
    public static class MalformedMove extends ChessException {
        public MalformedMove(String move) {
            super("Le coup n'est pas en notation UCI valide : " + move);
        }
    }

    /**
     * The move is well formed, but the rules of chess reject it in the current
     * position - a pinned piece, castling through check, leaving the king en
     * prise, and so on.
     */
    public static class IllegalMove extends ChessException {
        public IllegalMove(String move) {
            super("Coup illégal dans cette position : " + move);
        }
    }

    /**
     * A player cannot face himself.
     */
    public static class SelfPlay extends ChessException {
        public SelfPlay() {
            super("Tu ne peux pas jouer contre toi-même");
        }
    }
}
