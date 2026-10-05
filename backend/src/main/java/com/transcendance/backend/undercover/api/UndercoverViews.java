package com.transcendance.backend.undercover.api;

import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.transcendance.backend.model.User;
import com.transcendance.backend.repository.UserRepository;
import com.transcendance.backend.undercover.api.dto.UndercoverMatchViewDto;
import com.transcendance.backend.undercover.api.dto.UndercoverRoomDto;
import com.transcendance.backend.undercover.application.UndercoverMatchSnapshot;
import com.transcendance.backend.undercover.domain.UndercoverRoom;

import lombok.RequiredArgsConstructor;

/**
 * Builds what the api sends out, shared by the controllers and the STOMP
 * channel so a pushed view and a returned one never differ.
 */
@Component
@RequiredArgsConstructor
public class UndercoverViews {

    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * The accounts of these users, read in one query.
     */
    public Map<UUID, User> usersById(Collection<UUID> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getIdUser, Function.identity()));
    }

    /**
     * @param viewerId the member the room is shown to: only the host gets the
     *                 custom word list
     */
    public UndercoverRoomDto room(UndercoverRoom.View room, UUID viewerId) {
        return UndercoverRoomDto.of(room, usersById(room.members()), viewerId);
    }

    /**
     * @return the view of this user, empty if he holds no seat at the match
     */
    public Optional<UndercoverMatchViewDto> matchFor(UndercoverMatchSnapshot snapshot, UUID userId,
            Map<UUID, User> users) {
        return snapshot.match().getParticipants().stream()
                .filter(p -> p.getPlayer().getUserId().equals(userId))
                .findFirst()
                .map(viewer -> UndercoverMatchViewDto.of(snapshot, viewer, users, clock.instant()));
    }

    public Optional<UndercoverMatchViewDto> matchFor(UndercoverMatchSnapshot snapshot, UUID userId) {
        return matchFor(snapshot, userId, usersById(snapshot.match().getParticipants().stream()
                .map(p -> p.getPlayer().getUserId())
                .toList()));
    }
}
