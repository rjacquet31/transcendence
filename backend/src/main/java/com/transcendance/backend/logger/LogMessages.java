package com.transcendance.backend.logger;

/**
 * Utility class containing log message templates for authentication and validation events.
 */
public class LogMessages {
    public static final String INVALID_EMAIL = "The email provided is invalid. {}";
    public static final String EMAIL_ALREADY_USED = "The email is already used. {}";
    public static final String PASSWORDS_DO_NOT_MATCH = "Passwords do not match for email: {}";
    public static final String EMPTY_EMAIL = "Email is empty";
    public static final String EMPTY_FIRSTNAME = "First name is empty for email: {}";
    public static final String EMPTY_LASTNAME = "Last name is empty for email: {}";
    public static final String INVALID_USERNAME = "The username provided is invalid. {}";


    // Private constructor to prevent instantiation
    private LogMessages() {
    }
    
}
