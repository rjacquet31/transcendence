package com.transcendance.backend.config;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, JWTAuthenticationFilter jwtAuthFilter)
                        throws Exception {
                http
                                .cors(cors -> {
                                })
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
                                .authorizeHttpRequests(auth -> auth
                                                // Laisse passer le forward interne vers /error. Sans cette ligne
                                                // le anyRequest().denyAll() plus bas refuse aussi ce forward, et
                                                // toute erreur levee APRES l autorisation - un 400 de conversion,
                                                // un 500 d exception non geree - arrive au client en 403, ce qui
                                                // fait chercher un probleme de droits la ou il n y en a pas.
                                                // Seul le dispatch ERROR du conteneur est autorise : /error reste
                                                // injoignable par une requete directe.
                                                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                                                // --- Public : aucune authentification requise ---
                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/auth/register",
                                                                "/api/auth/login",
                                                                "/api/auth/refresh-token",
                                                                "/api/auth/logout",
                                                                "/api/auth/2fa/login",
                                                                "/api/auth/forgot-password",
                                                                "/api/auth/reset-password")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/default-avatar.png",
                                                                "/api/avatars/**",
                                                                "/api/auth/42/login",
                                                                "/api/auth/42/callback")
                                                .permitAll()
                                                // --- WebSocket handshake: authenticated by JwtHandshakeInterceptor ---
                                                .requestMatchers("/ws/**").permitAll()

                                                // --- USER et ADMIN ---
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/auth/42/link",
                                                                "/api/users/me",
                                                                "/api/users/{idUser}",
                                                                "/api/friend-list/me/list")
                                                .hasAnyRole("USER", "ADMIN")

                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/auth/42/unlink",
                                                                "/api/friend-list/send-request",
                                                                "/api/friend-list/{idUser}/accept",
                                                                "/api/friend-list/{idUser}/decline",
                                                                "/api/friend-list/{idUser}/delete",
                                                                "/api/friend-list/{idUser}/block",
                                                                "/api/friend-list/{idUser}/unblock",
                                                                "/api/users/me/avatar",
                                                                "/api/users/me/delete",
                                                                "/api/auth/2fa/setup",
                                                                "/api/auth/2fa/enable",
                                                                "/api/auth/2fa/disable")
                                                .hasAnyRole("USER", "ADMIN")

                                                .requestMatchers(HttpMethod.PUT,
                                                                "/api/users/{idUser}")
                                                .hasAnyRole("USER", "ADMIN")

                                                // --- CHESS ---
                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/chess/invitations",
                                                                "/api/chess/invitations/accept",
                                                                "/api/chess/invitations/decline",
                                                                "/api/chess/matches/{matchId}/moves",
                                                                "/api/chess/matches/{matchId}/resign",
                                                                "/api/chess/bot/createBotMatch")
                                                .hasAnyRole("USER", "ADMIN")
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/chess/invitations/pending",
                                                                "/api/chess/matches/me",
                                                                "/api/chess/stats/me",
                                                                "/api/chess/stats/{userId}",
                                                                "/api/chess/historique/me",
                                                                "/api/chess/historique/{userId}")
                                                .hasAnyRole("USER", "ADMIN")
                                                // --- UNDERCOVER ---
                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/undercover/rooms",
                                                                "/api/undercover/rooms/join",
                                                                "/api/undercover/rooms/leave",
                                                                "/api/undercover/rooms/start",
                                                                "/api/undercover/matches/{matchId}/clues",
                                                                "/api/undercover/matches/{matchId}/votes",
                                                                "/api/undercover/matches/{matchId}/guess",
                                                                "/api/undercover/matches/{matchId}/leave",
                                                                "/api/undercover/matches/{matchId}/chat")
                                                .hasAnyRole("USER", "ADMIN")
                                                .requestMatchers(HttpMethod.PUT,
                                                                "/api/undercover/rooms/settings",
                                                                "/api/undercover/rooms/words")
                                                .hasAnyRole("USER", "ADMIN")
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/undercover/rooms/me",
                                                                "/api/undercover/rooms/categories",
                                                                "/api/undercover/matches/me",
                                                                "/api/undercover/stats/me")
                                                .hasAnyRole("USER", "ADMIN")
                                                // --- CHAT ---
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/chat/{userId}")
                                                .hasAnyRole("USER", "ADMIN")
                                                .requestMatchers(HttpMethod.PUT,
                                                                "/api/chat/{userId}/read")
                                                .hasAnyRole("USER", "ADMIN")

                                                // --- ADMIN uniquement ---
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/users")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/users/{idUser}",
                                                                "/api/users/delete/{idUser}")
                                                .hasRole("ADMIN")

                                                // --- Tout le reste est refuse ---
                                                .anyRequest().denyAll())
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public BCryptPasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

}
