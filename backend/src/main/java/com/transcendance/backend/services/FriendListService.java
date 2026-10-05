package com.transcendance.backend.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.transcendance.backend.model.FriendshipStatus;
import com.transcendance.backend.model.User;
import com.transcendance.backend.dto.FriendListDto;
import com.transcendance.backend.model.FriendList;
import com.transcendance.backend.repository.UserRepository;
import com.transcendance.backend.repository.FriendListRepository;

import jakarta.transaction.Transactional;

@Service
public class FriendListService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(FriendListService.class);

    private UserRepository userRepository;
    private UserService userService;
    private FriendListRepository friendListRepository;
    private PresenceService presenceService;

    public FriendListService(UserRepository userRepository, UserService userService,
            FriendListRepository friendListRepository, PresenceService presenceService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.friendListRepository = friendListRepository;
        this.presenceService = presenceService;
    }

    /**
     * Sends a friend request from one user to another.
     *
     * @param user     The user sending the request.
     * @param username The username of the user receiving the request.
     * @return The user receiving the request, so the caller can notify him.
     */
    @Transactional
    public User sendFriendRequest(User user, String username) {

        log.info("Sending friend request from {} to {}", user.getUserName(), username);

        User target = userRepository.findByUserName(username)
                .orElseThrow(() -> {
                    log.info("User not found: {}", username);
                    return new IllegalArgumentException("Utilisateur introuvable");
                });

        if (target.getIdUser().equals(user.getIdUser())) {
            throw new IllegalArgumentException("Tu ne peux pas t'ajouter toi-même en ami");
        }

        List<UUID> ids = compareUUIDs(user, target);
        UUID id1 = ids.get(0);
        UUID id2 = ids.get(1);

        var existing = friendListRepository.findByUserId1AndUserId2(id1, id2);

        if (existing.isPresent()) {
            switch (existing.get().getStatus()) {
                case PENDING:
                    throw new IllegalArgumentException("Une demande d'ami est déjà en attente");

                case ACCEPTED:
                    throw new IllegalArgumentException("Vous êtes déjà amis");

                case BLOCKED:
                    throw new IllegalArgumentException("Une demande d'ami est déjà en attente");

                case DECLINED:
                    existing.get().setStatus(FriendshipStatus.PENDING);
                    existing.get().setCreatedAt(LocalDateTime.now());
                    // The new request may come from the one who declined the old one.
                    existing.get().setSenderId(user.getIdUser());
                    friendListRepository.save(existing.get());
                    log.info("Friend request sent from {} to {}", user.getUserName(), username);
                    return target;
            }
        }
        ;

        FriendList friendList = new FriendList();
        friendList.setUserId1(id1);
        friendList.setUserId2(id2);
        friendList.setStatus(FriendshipStatus.PENDING);
        friendList.setCreatedAt(LocalDateTime.now());
        friendList.setSenderId(user.getIdUser());
        friendListRepository.save(friendList);

        log.info("Friend request sent from {} to {}", user.getUserName(), username);
        return target;
    }

    /**
     * Accepts a friend request from one user to another.
     *
     * @param user   The user accepting the request.
     * @param idUser The ID of the user sending the request.
     */
    @Transactional
    public void acceptFriendRequest(User user, UUID idUser) {

        if (user == null) {
            throw new IllegalArgumentException("Tu dois être connecté");
        }

        log.info("Accepting friend request from {} to {}", user.getUserName(), idUser);

        User userTarget = userRepository.findById(idUser)
                .orElseThrow(() -> {
                    log.info("User not found: {}", idUser);
                    return new IllegalArgumentException("Utilisateur introuvable");
                });

        if (user.equals(userTarget)) {
            log.info("You cannot accept your own friend request");
            throw new IllegalArgumentException("Tu ne peux pas accepter ta propre demande d'ami");
        }
        List<UUID> ids = compareUUIDs(user, userTarget);
        UUID id1 = ids.get(0);
        UUID id2 = ids.get(1);

        var existing = friendListRepository.findByUserId1AndUserId2(id1, id2);
        if (existing.isPresent()) {
            if (existing.get().getSenderId().equals(user.getIdUser())) {
                log.warn("Friend request from {} to {} is already pending", user.getUserName(),
                        userTarget.getUserName());
                throw new IllegalArgumentException("La demande d'ami est déjà en attente");
            }
            if (existing.get().getStatus() == FriendshipStatus.PENDING) {
                existing.get().setStatus(FriendshipStatus.ACCEPTED);
                existing.get().setCreatedAt(LocalDateTime.now());
                friendListRepository.save(existing.get());
                log.info("Friend request accepted from {} to {}", user.getUserName(), userTarget.getUserName());
                return;
            }
            if (existing.get().getStatus() == FriendshipStatus.ACCEPTED) {
                log.info("Users {} and {} are already friends", user.getUserName(), userTarget.getUserName());
                throw new IllegalArgumentException("Vous êtes déjà amis");
            }
        }
        log.warn("No friend request from {} to {} is pending", user.getUserName(), userTarget.getUserName());
        throw new IllegalArgumentException("Aucune demande d'ami en attente");
    }

    /**
     * Declines a friend request from one user to another.
     *
     * @param user   The user declining the request.
     * @param idUser The ID of the user sending the request.
     */
    @Transactional
    public void declineFriendRequest(User user, UUID idUser) {

        if (user == null) {
            throw new IllegalArgumentException("Tu dois être connecté");
        }

        log.info("Declining friend request from {} to {}", user.getUserName(), idUser);

        User userTarget = userRepository.findById(idUser)
                .orElseThrow(() -> {
                    log.info("User not found: {}", idUser);
                    return new IllegalArgumentException("Utilisateur introuvable");
                });

        if (user.equals(userTarget)) {
            log.info("You cannot decline yourself");
            throw new IllegalArgumentException("Tu ne peux pas te refuser toi-même");
        }
        List<UUID> ids = compareUUIDs(user, userTarget);
        UUID id1 = ids.get(0);
        UUID id2 = ids.get(1);

        var existing = friendListRepository.findByUserId1AndUserId2(id1, id2);
        if (existing.isPresent()) {
            if (existing.get().getStatus() == FriendshipStatus.PENDING) {
                existing.get().setStatus(FriendshipStatus.DECLINED);
                existing.get().setCreatedAt(LocalDateTime.now());
                friendListRepository.save(existing.get());
                log.info("Friend request declined from {} to {}", user.getUserName(), userTarget.getUserName());
                return;
            }
        }
        log.warn("No friend request from {} to {} is pending", user.getUserName(), userTarget.getUserName());
        throw new IllegalArgumentException("Aucune demande d'ami en attente");
    }

    /**
     * Deletes a friend from the friend list.
     *
     * @param user   The user deleting the friend.
     * @param idUser The ID of the user to delete as a friend.
     */
    @Transactional
    public void deleteFriend(User user, UUID idUser) {

        if (user == null) {
            throw new IllegalArgumentException("Tu dois être connecté");
        }

        log.info("Deleting friend from {} to {}", user.getUserName(), idUser);

        User userTarget = userRepository.findById(idUser)
                .orElseThrow(() -> {
                    log.info("User not found: {}", idUser);
                    return new IllegalArgumentException("Utilisateur introuvable");
                });

        if (user.equals(userTarget)) {
            log.info("You cannot delete yourself");
            throw new IllegalArgumentException("Tu ne peux pas te supprimer toi-même");
        }
        List<UUID> ids = compareUUIDs(user, userTarget);
        UUID id1 = ids.get(0);
        UUID id2 = ids.get(1);

        var existing = friendListRepository.findByUserId1AndUserId2(id1, id2);
        if (existing.isPresent()) {
            if (existing.get().getStatus() == FriendshipStatus.ACCEPTED) {
                friendListRepository.delete(existing.get());
                log.info("Friend deleted from {} to {}", user.getUserName(), userTarget.getUserName());
                return;
            }
        }
        log.warn("No friend relationship found between {} and {}", user.getUserName(), userTarget.getUserName());
        throw new IllegalArgumentException("Aucune relation d'amitié établie");
    }

    /**
     * Blocks a user from the friend list.
     *
     * @param user   The user blocking another user.
     * @param idUser The ID of the user to block.
     */
    @Transactional
    public void blockFriend(User user, UUID idUser) {

        if (user == null) {
            throw new IllegalArgumentException("Tu dois être connecté");
        }

        log.info("Blocking user from {} to {}", user.getUserName(), idUser);

        User userTarget = userRepository.findById(idUser)
                .orElseThrow(() -> {
                    log.info("User not found: {}", idUser);
                    return new IllegalArgumentException("Utilisateur introuvable");
                });

        if (user.equals(userTarget)) {
            log.info("You cannot block yourself");
            throw new IllegalArgumentException("Tu ne peux pas te bloquer toi-même");
        }
        List<UUID> ids = compareUUIDs(user, userTarget);
        UUID id1 = ids.get(0);
        UUID id2 = ids.get(1);

        var existing = friendListRepository.findByUserId1AndUserId2(id1, id2);
        if (existing.isPresent()) {
            if (existing.get().getStatus() == FriendshipStatus.ACCEPTED
                    || existing.get().getStatus() == FriendshipStatus.PENDING
                    || existing.get().getStatus() == FriendshipStatus.DECLINED) {
                existing.get().setStatus(FriendshipStatus.BLOCKED);
                existing.get().setCreatedAt(LocalDateTime.now());
                existing.get().setBlockedBy(user.getIdUser());
                friendListRepository.save(existing.get());
                log.info("User blocked from {} to {}", user.getUserName(), userTarget.getUserName());
                return;
            } else {
                if (existing.get().getBlockedBy().equals(user.getIdUser())) {
                    throw new IllegalArgumentException("Tu as déjà bloqué cet utilisateur");
                }
                throw new IllegalArgumentException("Statut d'amitié invalide");
            }
        } else {
            FriendList friendList = new FriendList();
            friendList.setCreatedAt(LocalDateTime.now());
            friendList.setStatus(FriendshipStatus.BLOCKED);
            friendList.setUserId1(id1);
            friendList.setUserId2(id2);
            friendList.setSenderId(user.getIdUser());
            friendList.setBlockedBy(user.getIdUser());
            friendListRepository.save(friendList);
            log.info("User blocked from {} to {}", user.getUserName(), userTarget.getUserName());
            return;
        }

    }

    /**
     * Unblocks a user from the friend list.
     *
     * @param user   The user unblocking another user.
     * @param idUser The ID of the user to unblock.
     */
    @Transactional
    public void unblockFriend(User user, UUID idUser) {
        if (user == null) {
            throw new IllegalArgumentException("Tu dois être connecté");
        }

        log.info("Unblocking user from {} to {}", user.getUserName(), idUser);

        User userTarget = userRepository.findById(idUser)
                .orElseThrow(() -> {
                    log.info("User not found: {}", idUser);
                    return new IllegalArgumentException("Utilisateur introuvable");
                });

        if (user.equals(userTarget)) {
            log.info("You cannot unblock yourself");
            throw new IllegalArgumentException("Tu ne peux pas te débloquer toi-même");
        }

        List<UUID> ids = compareUUIDs(user, userTarget);
        UUID id1 = ids.get(0);
        UUID id2 = ids.get(1);

        var existing = friendListRepository.findByUserId1AndUserId2(id1, id2);
        if (existing.isPresent()) {
            if (existing.get().getStatus() == FriendshipStatus.BLOCKED) {
                if (existing.get().getBlockedBy() != null && existing.get().getBlockedBy().equals(user.getIdUser())) {
                    friendListRepository.delete(existing.get());
                    log.info("Friend unblocked from {} to {}", user.getUserName(), userTarget.getUserName());
                    return;
                }
            }
        }
        log.warn("No friend relationship found between {} and {}", user.getUserName(), userTarget.getUserName());
        throw new IllegalArgumentException("Aucune relation d'amitié établie");
    }

    /**
     * Retrieves the friend list for a given user.
     *
     * @param user The user for whom to retrieve the friend list.
     * @return A list of friend details.
     */
    @Transactional
    public List<FriendListDto> getFriendList(User user) {
        List<FriendList> friendLists = friendListRepository.findByUserId1OrUserId2(user.getIdUser(), user.getIdUser());

        List<FriendListDto> friendListDtos = new ArrayList<>();
        for (FriendList friendList : friendLists) {

            if (friendList.getStatus() == FriendshipStatus.BLOCKED
                 && !user.getIdUser().equals(friendList.getBlockedBy()))
                continue;

            UUID targetId;
            if (friendList.getUserId1().equals(user.getIdUser())) {
                targetId = friendList.getUserId2();
            } else {
                targetId = friendList.getUserId1();
            }

            Optional<User> targetUser = userService.getUserById(targetId);
            if (targetUser.isEmpty()) {
                continue;
            }

            // Presence is told to accepted friends only, exactly like the
            // real-time broadcast: this list also carries pending requests and
            // blocked users, and neither is owed the other's whereabouts.
            boolean accepted = friendList.getStatus() == FriendshipStatus.ACCEPTED;

            friendListDtos.add(new FriendListDto(targetId, targetUser.get().getUserName(),
                    friendList.getCreatedAt(), friendList.getStatus(), friendList.getSenderId(),
                    accepted && presenceService.isOnline(targetId),
                    accepted ? targetUser.get().getOnline() : null,
                    friendList.getBlockedBy()));
        }
        return friendListDtos;
    }

    /**
     * Compares the UUIDs of two users and returns them in a sorted list.
     *
     * @param user       The first user.
     * @param userTarget The second user.
     * @return A list containing the sorted UUIDs.
     */
    public List<UUID> compareUUIDs(User user, User userTarget) {
        List<UUID> ids = new ArrayList<>();
        ids.add(user.getIdUser());
        ids.add(userTarget.getIdUser());

        Collections.sort(ids);

        return ids;
    }

    /**
     * Checks if two users are friends.
     *
     * @param user1 The first user.
     * @param user2 The second user.
     * @return True if the users are friends, false otherwise.
     */
    public boolean checkFriendship(User user1, User user2) {
        List<UUID> ids = compareUUIDs(user1, user2);
        UUID id1 = ids.get(0);
        UUID id2 = ids.get(1);

        var existing = friendListRepository.findByUserId1AndUserId2(id1, id2);
        return existing.isPresent() && existing.get().getStatus() == FriendshipStatus.ACCEPTED;
    }
}
