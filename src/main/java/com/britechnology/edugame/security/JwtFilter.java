package com.britechnology.edugame.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /** Ne pas appliquer le filtre JWT sur les routes publiques (évite tout blocage 403). */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null) path = "";
        String uri = request.getRequestURI() != null ? request.getRequestURI() : "";
        return path.startsWith("/api/auth/")
                || path.startsWith("/auth/")
                || uri.contains("/api/auth/")
                || uri.contains("/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        String token;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // Repli sur un paramètre de requête `token` : nécessaire pour navigator.sendBeacon
            // (ex. signal d'abandon de partie à la fermeture de l'onglet), qui ne permet pas
            // d'ajouter d'en-tête Authorization personnalisé.
            String queryToken = request.getParameter("token");
            if (queryToken == null || queryToken.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }
            token = queryToken;
        }

        if (!jwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String email = jwtUtil.extractEmail(token);
        final String role = jwtUtil.extractRole(token); // "ADMIN" / "JOUEUR" ...

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // ✅ Spring standard: ROLE_ADMIN, ROLE_JOUEUR, ...
            List<SimpleGrantedAuthority> authorities =
                    (role == null) ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
