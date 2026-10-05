package com.transcendance.backend.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.transcendance.backend.config.SecurityConfig;
import com.transcendance.backend.model.User;
import com.transcendance.backend.repository.UserRepository;

@Service
public class UserService {

    private static final Logger Log = org.apache.logging.log4j.LogManager.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final SecurityConfig securityConfig;

    public UserService(UserRepository userRepository, SecurityConfig securityConfig) {
        this.userRepository = userRepository;
        this.securityConfig = securityConfig;
    }

    // Method to check if the email format is invalid
    public boolean isInvalidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return !email.matches(emailRegex);
    }

    // Method to check if the username format is invalid
    public boolean isInvalidUsername(String username) {
        String usernameRegex = "^[A-Za-z0-9_-]{3,20}$"; // Example: 3-20 characters, alphanumeric, underscores and hyphens
        return !username.matches(usernameRegex);
    }

    // Method to check if a user with the given email already exists
    public Optional<User> getUserByEmail(String email) {
        Log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    // Method to check if a 42 account is already linked to a local user
    public Optional<User> getUserByOauth42Id(Long oauth42Id) {
        Log.debug("Fetching user by 42 id: {}", oauth42Id);
        return userRepository.findByOauth42Id(oauth42Id);
    }

    // Method to fetch a user by their id
    public Optional<User> getUserById(UUID idUser) {
        Log.debug("Fetching user by id: {}", idUser);
        return userRepository.findById(idUser);
    }

    // Method to fetch every user stored in the database (SELECT * FROM users)
    public List<User> getAllUsers() {
        Log.debug("Fetching all users");
        return userRepository.findAll();
    }

    // Method to check if the provided password matches the stored password for the user
    public boolean checkPassword(User user, String rawPassword) {
        Log.debug("Checking password for user: {}", user.getEmail());
        return securityConfig.passwordEncoder().matches(rawPassword, user.getPassword());
    }

    // Method to check if a user with the given username already exists
    public Optional<User> getUserByUserName(String userName) {
        Log.debug("Fetching user by username: {}", userName);
        return userRepository.findByUserName(userName);
    }

    // Method to tell whether a username is already taken
    public boolean isUserNameTaken(String userName) {
        Log.debug("Checking if username is taken: {}", userName);
        return userRepository.existsByUserName(userName);
    }

    // Method to persist an already existing user (no password re-encoding)
    public User saveUser(User user) {
        Log.debug("Saving user with id: {}", user.getIdUser());
        return userRepository.save(user);
    }

    // Method to remove a user (DELETE FROM users WHERE id_user = ...)
    public void deleteUser(User user) {
        Log.debug("Deleting user with id: {}", user.getIdUser());
        userRepository.delete(user);
    }

    // Method to create and save a new user
    public User createUser(User user) {
        Log.debug("Saving user with email: {}", user.getEmail());
        user.setPassword(securityConfig.passwordEncoder().encode(user.getPassword()));
        return userRepository.save(user);
    }

    // Method to update the avatar URL of a user
    public User updateAvatarUrl(User user, String avatarUrl) {
        Log.debug("Updating avatar URL for user: {}", user.getIdUser());
        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }

    public String encodePassword(String raw) {
        return securityConfig.passwordEncoder().encode(raw);
    }
}
