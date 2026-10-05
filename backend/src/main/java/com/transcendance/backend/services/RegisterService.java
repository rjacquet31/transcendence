package com.transcendance.backend.services;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transcendance.backend.dto.RegisterRequestDto;
import com.transcendance.backend.logger.LogMessages;
import com.transcendance.backend.model.User;

@Service
public class RegisterService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RegisterService.class);

    private static final String DEFAULT_AVATAR_URL = "/api/default-avatar.png";

    private final UserService userService;
    private final UserRoleService userRoleService;

    public RegisterService(UserService userService, UserRoleService userRoleService) {
        this.userService = userService;
        this.userRoleService = userRoleService;
    }
    
    /**
     * Register a new user with the provided information.
     *
     * @param userRegisterDto registration data provided by the user
     * @return the created User
     * @throws IllegalArgumentException if the registration data is invalid or the email is already used
     */
    @Transactional
    public User registerUser(RegisterRequestDto userRegisterDto) {
        log.info("Starting registration for email: {}", userRegisterDto.getEmail());
        
        // Check username format
        if (userService.isInvalidUsername(userRegisterDto.getUserName())) {
            log.error(LogMessages.INVALID_USERNAME, userRegisterDto.getUserName());
            throw new IllegalArgumentException("Le nom d'utilisateur est invalide : " + userRegisterDto.getUserName());
        }

        // Check if username is already taken
        if (userService.isUserNameTaken(userRegisterDto.getUserName())) {
            log.error("The username is already used: {}", userRegisterDto.getUserName());
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà utilisé : " + userRegisterDto.getUserName());
        }

        // Check if passwords match
        if (!userRegisterDto.getPassword().equals(userRegisterDto.getConfirmedPassword())) {
            log.error(LogMessages.PASSWORDS_DO_NOT_MATCH);
            throw new IllegalArgumentException("Les mots de passe ne correspondent pas");
        }
        // Check if email is empty
        if (userRegisterDto.getEmail() == null || userRegisterDto.getEmail().isEmpty()){
            log.error(LogMessages.EMPTY_EMAIL);
            throw new IllegalArgumentException("L'email ne peut pas être vide");
        }

        // Validate email format
       if (userService.isInvalidEmail(userRegisterDto.getEmail())) {
            log.error(LogMessages.INVALID_EMAIL, userRegisterDto.getEmail());
            throw new IllegalArgumentException("L'email est invalide : " + userRegisterDto.getEmail());
        }

        // Check if email is already used
        if (userService.getUserByEmail(userRegisterDto.getEmail()).isPresent()) {
            log.error(LogMessages.EMAIL_ALREADY_USED, userRegisterDto.getEmail());
            throw new IllegalArgumentException("Cet email est déjà utilisé : " + userRegisterDto.getEmail());
        } 

        // Create a new user
        User newUser = new User(
            userRegisterDto.getUserName(),
            userRegisterDto.getEmail(),
            userRegisterDto.getPassword()
        );

        newUser.setAvatarUrl(DEFAULT_AVATAR_URL);

        // Save the new user
        User savedUser = userService.createUser(newUser);
        log.info("User registered successfully with email: {}", savedUser.getEmail());
        // Assign the default USER role to the new user
        userRoleService.assignRoleUserToNewUser(savedUser);
        return savedUser;
    }
}
