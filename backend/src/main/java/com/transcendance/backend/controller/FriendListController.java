package com.transcendance.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendance.backend.dto.FriendListDto;
import com.transcendance.backend.dto.UserNameRequestDto;
import com.transcendance.backend.model.User;
import com.transcendance.backend.services.FriendListService;
import com.transcendance.backend.services.NotificationService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/friend-list")
public class FriendListController {
    
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(FriendListController.class);

    private final FriendListService friendListService;
    private final NotificationService notificationService;

    public FriendListController(FriendListService friendListService, NotificationService notificationService) {
        this.friendListService = friendListService;
        this.notificationService = notificationService;
    }

    @PostMapping("/send-request")
    public ResponseEntity<?> sendFriendRequest(@AuthenticationPrincipal User user, 
            @Valid @RequestBody UserNameRequestDto Username,
            BindingResult bindingResult) {
        
        if (Username == null) {
            log.warn("Friend request failed: Username is null");
            return ResponseEntity.status(401).body(Map.of("error", "Nom d'utilisateur manquant"));
        }
        if (bindingResult.hasErrors()) {
            log.warn("Friend request failed: Invalid username format");
            return ResponseEntity.status(400).body(Map.of("error", "Format de nom d'utilisateur invalide"));
        }
        log.info("Sending friend request from {} to {}", user.getUserName(), Username.getUserName());
        try {
            User target = friendListService.sendFriendRequest(user, Username.getUserName());
            notificationService.friendRequestReceived(target.getIdUser(), user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Friend request failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Échec de l'envoi de la demande d'ami"));
        }
    }

    @PostMapping("/{idUser}/accept")
    public ResponseEntity<?> acceptFriendRequest(@AuthenticationPrincipal User user, @PathVariable UUID idUser) {

        if (idUser == null) {
            log.warn("Friend request failed: User ID is null");
            return ResponseEntity.status(400).body(Map.of("error", "Identifiant utilisateur manquant"));
        }
        try{
            friendListService.acceptFriendRequest(user, idUser);
            notificationService.friendRequestAccepted(idUser, user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Failed to accept friend request: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", "Échec de l'acceptation de la demande d'ami"));
        }
    }

    @PostMapping("/{idUser}/decline")
    public ResponseEntity<?> declineFriendRequest(@AuthenticationPrincipal User user, @PathVariable UUID idUser) {

        if (idUser == null) {
            log.warn("Friend request failed: User ID is null");
            return ResponseEntity.status(400).body(Map.of("error", "Identifiant utilisateur manquant"));
        }
        try{
            friendListService.declineFriendRequest(user, idUser);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Failed to decline friend request: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", "Échec du refus de la demande d'ami"));
        }
    }

    @PostMapping("/{idUser}/delete")
    public ResponseEntity<?> deleteFriend(@AuthenticationPrincipal User user, @PathVariable UUID idUser) {

        if (idUser == null) {
            log.warn("Friend request failed: User ID is null");
            return ResponseEntity.status(400).body(Map.of("error", "Identifiant utilisateur manquant"));
        }
        try{
            friendListService.deleteFriend(user, idUser);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Failed to delete friend: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", "Échec de la suppression de l'ami"));
        }
    }

    @PostMapping("/{idUser}/block")
    public ResponseEntity<?> blockUser(@AuthenticationPrincipal User user, @PathVariable UUID idUser) {

        if (idUser == null) {
            log.warn("Friend request failed: User ID is null");
            return ResponseEntity.status(400).body(Map.of("error", "Identifiant utilisateur manquant"));
        }
        try{
            friendListService.blockFriend(user, idUser);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Failed to block user: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", "Échec du blocage de l'utilisateur"));
        }
    }

    @PostMapping("/{idUser}/unblock")
    public ResponseEntity<?> unblockUser(@AuthenticationPrincipal User user, @PathVariable UUID idUser) {

        if (idUser == null) {
            log.warn("Friend request failed: User ID is null");
            return ResponseEntity.status(400).body(Map.of("error", "Identifiant utilisateur manquant"));
        }
        try{
            friendListService.unblockFriend(user, idUser);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Failed to unblock user: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", "Échec du déblocage de l'utilisateur"));
        }
    }

    @GetMapping("me/list")
    public ResponseEntity<?> getFriendList(@AuthenticationPrincipal User user) {

        if (user == null) {
            log.warn("Friend request failed: Username is null");
            return ResponseEntity.status(401).body(Map.of("error", "Nom d'utilisateur manquant"));
        }
    
        List<FriendListDto> friendList = friendListService.getFriendList(user);
        return ResponseEntity.ok(friendList);
    }
}