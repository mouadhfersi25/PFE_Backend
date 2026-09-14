package com.britechnology.edugame.config;

import com.britechnology.edugame.security.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // En-têtes de sécurité HTTP standards (défense en profondeur).
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/countriesnow/**",
                                "/api/ext-ads/**",
                                "/ws/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/error"
                        ).permitAll()

                        // ✅ bonne pratique Spring : ADMIN -> ROLE_ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Sponsor : gestion des campagnes/publicités/récompenses
                        .requestMatchers("/api/sponsor/**").hasAnyRole("SPONSOR", "ADMIN")

                        // Éducateur : gestion du contenu (questions, jeux, etc.)
                        // Admin peut aussi consulter, afin d'éviter 403 pour /api/educator pour les admins.
                        .requestMatchers("/api/educator/**").hasAnyRole("EDUCATEUR", "ADMIN")

                        // Joueur : atelier oral (module indépendant des jeux)
                        .requestMatchers("/api/player/voice/**").hasRole("JOUEUR")

                        // Joueur : publicités in-game
                        .requestMatchers("/api/player/ads/**").hasAnyRole("JOUEUR", "ADMIN")

                        // ➤ Utilisateurs authentifiés
                        .requestMatchers("/api/users/**").authenticated()
                        .anyRequest().authenticated()
                )

                // Par défaut Spring Security renvoie 403 aussi bien pour "non authentifié" (token
                // absent/expiré/invalide) que pour "authentifié mais rôle insuffisant", ce qui
                // empêche le front de distinguer "il faut se reconnecter" de "accès refusé".
                // On sépare explicitement : 401 = pas (ou plus) authentifié, 403 = rôle insuffisant.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"message\":\"Session expirée ou non authentifiée. Veuillez vous reconnecter.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"message\":\"Accès refusé : vous n'avez pas les droits nécessaires.\"}");
                        })
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Patterns (pas setAllowedOrigins) : le port du serveur dev (Vite) change dès qu'un port est
        // déjà occupé (3000 -> 3001 -> 3002...) ; un pattern évite de devoir mettre à jour cette liste
        // à chaque fois. setAllowedOriginPatterns reste compatible avec allowCredentials(true).
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }



}
