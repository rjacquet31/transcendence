package com.transcendance.backend.services;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import com.transcendance.backend.model.Role;
import com.transcendance.backend.model.User;
import com.transcendance.backend.model.UserRole;
import com.transcendance.backend.repository.RoleRepository;
import com.transcendance.backend.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(UserRoleService.class);

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    /**
     * Assign the default USER role to a user.
     *
     * @param user the user to assign the role to
     * @throws RuntimeException if the USER role does not exist
     */
    public void assignRoleUserToNewUser(User user) {
        // Fetch the USER role from the database
        Role role = roleRepository.findByRoleName("USER")
                .orElseThrow(() ->
                        new RuntimeException("Default role 'USER' not found."));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        userRoleRepository.save(userRole);
        log.info("Assigned role 'USER' to user with email: {}", user.getEmail());
    }

    /**
     * Get the roles assigned to a user.
     *
     * @param user the user for which to retrieve roles
     * @return a list of granted authorities representing the user's roles
     */
    public List<SimpleGrantedAuthority> getRoleUser(User user) {
    return userRoleRepository.findByUser(user)
        .stream()
        .map(userRole -> new SimpleGrantedAuthority(
            "ROLE_" + userRole.getRole().getRoleName()
        ))
        .toList();
    }

    /**
     * Replace the role assigned to a user.
     * <p>
     * The join table allows several roles per user, but the application assigns
     * exactly one: the existing row is updated rather than a second one added.
     *
     * @param user     the user whose role is being set
     * @param roleName the raw role name (e.g. "ADMIN"), without the "ROLE_" prefix
     * @return the saved {@link UserRole}
     * @throws IllegalArgumentException if no role matches the given name
     */
    public UserRole setRole(User user, String roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() ->
                        new IllegalArgumentException("Le rôle est invalide : " + roleName));

        UserRole userRole = userRoleRepository.findByUser(user)
                .orElseGet(() -> {
                    UserRole created = new UserRole();
                    created.setUser(user);
                    return created;
                });
        userRole.setRole(role);

        log.info("Assigned role '{}' to user with id: {}", roleName, user.getIdUser());
        return userRoleRepository.save(userRole);
    }

    /**
     * Get the raw role name assigned to a user, without the "ROLE_" prefix.
     *
     * @param user the user for which to retrieve the role name
     * @return the role name (e.g. "USER"), or null if the user has no role
     */
    public String getRoleName(User user) {
        return userRoleRepository.findByUser(user)
                .map(userRole -> userRole.getRole().getRoleName())
                .orElse(null);
    }
}
