package com.transcendance.backend.services;

import java.time.Instant;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transcendance.backend.model.User;
import com.transcendance.backend.repository.PasswordResetTokenRepository;
import com.transcendance.backend.repository.RefreshTokenRepository;
import com.transcendance.backend.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteUserService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DeleteUserService.class);

    private final UserService userService;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AvatarStorageService avatarStorageService;

    /**
     * Soft-deletes a user: keeps the row (chat history, friend list and
     * chess match history all reference it by id or foreign key) but
     * scrubs every piece of personally identifiable info, and revokes
     * sessions/permissions. The whole thing runs in one transaction, so a
     * failure halfway through leaves the account untouched.
     * <p>
     * Side effect worth knowing: since the real email gets overwritten,
     * the original address becomes free again for a future signup
     * (RegisterService and Oauth42Service both look a user up by email) —
     * that's intentional, not a bug.
     *
     * @param user the user to soft-delete
     */
    @Transactional
    public void deleteUser(User user) {
        log.info("Deleting user id: {}", user.getIdUser());

        refreshTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);
        userRoleRepository.deleteByUser(user);

        user.setUserName("Utilisateur supprimé " + user.getIdUser().toString().substring(0, 8));

        user.setEmail("deleted-" + user.getIdUser() + "@deleted.local");

        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            String fileName = avatarUrl.substring(avatarUrl.lastIndexOf('/') + 1);
            avatarStorageService.delete(fileName);
        }
        user.setAvatarUrl(null);

        String randomId = UUID.randomUUID().toString();
        String newPasswd = userService.encodePassword(randomId);
        user.setPassword(newPasswd);

        user.setDeletedAt(Instant.now());

        userService.saveUser(user);

        log.info("User deleted: {}", user.getIdUser());
    }
}
