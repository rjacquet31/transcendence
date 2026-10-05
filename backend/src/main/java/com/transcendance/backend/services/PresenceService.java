package com.transcendance.backend.services;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.transcendance.backend.dto.PresenceDto;
import com.transcendance.backend.model.FriendshipStatus;
import com.transcendance.backend.repository.FriendListRepository;
import com.transcendance.backend.repository.UserRepository;

@Service
public class PresenceService {

    private static final Logger log = LogManager.getLogger(PresenceService.class);

    private static final String PRESENCE_DESTINATION = "/queue/presence";

    /**
     * Open STOMP sessions per user. A user with no session left is removed
     * outright rather than left with an empty set, which is what lets isOnline be
     * a plain containsKey.
     */
    private final Map<UUID, Set<String>> sessionsByUser = new ConcurrentHashMap<>();

    private final FriendListRepository friendListRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceService(FriendListRepository friendListRepository, UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.friendListRepository = friendListRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Records a session that has just opened, and announces its owner if it is
     * his only one.
     *
     * The whole read-modify-decide runs inside compute, which ConcurrentHashMap
     * executes atomically for a given key: two tabs connecting at the same
     * instant would otherwise both read an empty set and both announce.
     *
     * @param userId    owner of the session, taken from the STOMP Principal
     * @param sessionId STOMP session id, one per tab
     */
    public void sessionOpened(UUID userId, String sessionId) {
        boolean[] cameOnline = { false };

        sessionsByUser.compute(userId, (id, sessions) -> {
            Set<String> updated = (sessions == null) ? new HashSet<>() : sessions;
            cameOnline[0] = updated.isEmpty();
            updated.add(sessionId);
            return updated;
        });

        if (cameOnline[0]) {
            log.info("User {} came online", userId);
            broadcast(userId, true, null);
        }
    }

    /**
     * Records a session that has just closed, and announces its owner as gone
     * once he has none left.
     *
     * A plain HashSet is enough inside the map: every mutation of it happens
     * within a compute block, so it is never touched by two threads at once.
     *
     * @param userId    owner of the session, taken from the STOMP Principal
     * @param sessionId STOMP session id, one per tab
     */
    public void sessionClosed(UUID userId, String sessionId) {
        boolean[] wentOffline = { false };

        sessionsByUser.computeIfPresent(userId, (id, sessions) -> {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                wentOffline[0] = true;
                // Returning null drops the entry: an empty set kept here would
                // grow the map by one dead key per user, for good.
                return null;
            }
            return sessions;
        });

        if (wentOffline[0]) {
            log.info("User {} went offline", userId);
            broadcast(userId, false, touchLastSeen(userId));
        }
    }

    /**
     * @return true if this user has at least one open real-time session
     */
    public boolean isOnline(UUID userId) {
        return sessionsByUser.containsKey(userId);
    }

    /**
     * Pushes a status change to the accepted friends of a user.
     *
     * Only accepted friendships are told: a pending request is not a
     * relationship yet, and a blocked user must not keep watching the other come
     * and go.
     */
    private void broadcast(UUID userId, boolean online, Instant lastSeen) {
        PresenceDto presence = new PresenceDto(userId, online, lastSeen);

        for (UUID friendId : friendListRepository.findFriendIdsByStatus(userId, FriendshipStatus.ACCEPTED)) {
            // The broker holds nothing: a message sent to a friend with no open
            // session is dropped on the spot, so it is not worth building.
            if (!isOnline(friendId)) {
                continue;
            }
            push(friendId, presence);
        }
    }

    /**
     * One delivery, isolated from the others: a friend whose session dies mid
     * broadcast must not keep the remaining friends from being told.
     */
    private void push(UUID toUserId, PresenceDto presence) {
        try {
            messagingTemplate.convertAndSendToUser(toUserId.toString(), PRESENCE_DESTINATION, presence);
        } catch (Exception e) {
            log.error("Presence of {} could not be pushed to {}: {}",
                    presence.getIdUser(), toUserId, e.getMessage());
        }
    }

    /**
     * Writes down when the user was last seen, and hands the same instant to the
     * broadcast so the row and the pushed event cannot disagree.
     *
     * The column is called "online" and holds an Instant: it was already there,
     * unwritten, and the front reads it under that name. Renaming it would mean
     * a migration that ddl-auto=update cannot perform - it would add last_seen
     * and leave online NOT NULL with no default, breaking every insert.
     *
     * A failure here must not cost the offline event: knowing someone has left
     * matters more, right now, than dating his departure.
     */
    private Instant touchLastSeen(UUID userId) {
        Instant now = Instant.now();

        try {
            userRepository.findById(userId).ifPresent(user -> {
                user.setOnline(now);
                userRepository.save(user);
            });
        } catch (Exception e) {
            log.error("Last seen of {} could not be stored: {}", userId, e.getMessage());
        }

        return now;
    }
}
