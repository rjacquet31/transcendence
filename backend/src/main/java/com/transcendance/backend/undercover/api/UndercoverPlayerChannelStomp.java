package com.transcendance.backend.undercover.api;

import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.transcendance.backend.model.User;
import com.transcendance.backend.undercover.api.dto.UndercoverChatMessageDto;
import com.transcendance.backend.undercover.api.dto.UndercoverRoomDto;
import com.transcendance.backend.undercover.application.UndercoverChatMessage;
import com.transcendance.backend.undercover.application.UndercoverMatchSnapshot;
import com.transcendance.backend.undercover.application.UndercoverPlayerChannel;
import com.transcendance.backend.undercover.application.UndercoverQueryService;
import com.transcendance.backend.undercover.domain.UndercoverRoom;
import com.transcendance.backend.undercover.entity.UndercoverParticipantEntity;

import lombok.RequiredArgsConstructor;

/**
 * STOMP implementation of the UndercoverPlayerChannel port.
 *
 * Every payload goes to a /user destination: a match view carries its
 * recipient's word, and must never reach anyone else.
 */
@Component
@RequiredArgsConstructor
public class UndercoverPlayerChannelStomp implements UndercoverPlayerChannel {

    private static final String ROOM_DESTINATION = "/queue/undercover.rooms";
    private static final String MATCH_DESTINATION = "/queue/undercover.matches";
    private static final String CHAT_DESTINATION = "/queue/undercover.chat";

    private static final Logger log = LogManager.getLogger(UndercoverPlayerChannelStomp.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final UndercoverQueryService queryService;
    private final UndercoverViews views;

    @Override
    public void roomUpdated(UndercoverRoom.View room) {
        // The host's copy holds the custom word list, so one copy per member.
        Map<UUID, User> users = views.usersById(room.members());
        room.members().forEach(member -> push(member, ROOM_DESTINATION, UndercoverRoomDto.of(room, users, member)));
    }

    @Override
    public void matchUpdated(long matchId) {
        try {
            UndercoverMatchSnapshot snapshot = queryService.snapshot(matchId);
            Map<UUID, User> users = views.usersById(snapshot.match().getParticipants().stream()
                    .map(p -> p.getPlayer().getUserId())
                    .toList());

            // Read once for the whole table, then cut into one view per player.
            for (UndercoverParticipantEntity participant : snapshot.match().getParticipants()) {
                if (participant.isLeftMatch()) {
                    continue;
                }
                UUID userId = participant.getPlayer().getUserId();
                views.matchFor(snapshot, userId, users)
                        .ifPresent(view -> push(userId, MATCH_DESTINATION, view));
            }
        } catch (Exception e) {
            log.error("Undercover match {} could not be pushed: {}", matchId, e.getMessage());
        }
    }

    @Override
    public void chatMessage(UndercoverChatMessage message) {
        User author = views.usersById(java.util.List.of(message.authorId())).get(message.authorId());
        UndercoverChatMessageDto dto = UndercoverChatMessageDto.of(message,
                author == null ? null : author.getUserName());
        message.recipients().forEach(recipient -> push(recipient, CHAT_DESTINATION, dto));
    }

    private void push(UUID toUserId, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(toUserId.toString(), destination, payload);
        } catch (Exception e) {
            log.error("Undercover payload could not be pushed to {} on {}: {}",
                    toUserId, destination, e.getMessage());
        }
    }
}
