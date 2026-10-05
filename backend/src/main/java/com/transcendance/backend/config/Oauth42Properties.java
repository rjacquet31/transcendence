package com.transcendance.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.oauth42")
public class Oauth42Properties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authorizeUri;
    private String tokenUri;
    private String userInfoUri;
    private String frontendRedirectUri;

    public String getClientId () {
        return clientId;
    }

    public void setClientId (String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret () {
        return clientSecret;
    }

    public void setClientSecret (String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri () {
        return redirectUri;
    }

    public void setRedirectUri (String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getAuthorizeUri () {
        return authorizeUri;
    }

    public void setAuthorizeUri (String authorizeUri) {
        this.authorizeUri = authorizeUri;
    }

    public String getTokenUri () {
        return tokenUri;
    }

    public void setTokenUri (String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getUserInfoUri () {
        return userInfoUri;
    }

    public void setUserInfoUri (String userInfoUri) {
        this.userInfoUri = userInfoUri;
    }

    public String getFrontendRedirectUri () {
        return frontendRedirectUri;
    }

    public void setFrontendRedirectUri (String frontendRedirectUri) {
        this.frontendRedirectUri = frontendRedirectUri;
    }

}
