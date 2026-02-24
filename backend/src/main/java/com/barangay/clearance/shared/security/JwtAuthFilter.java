package com.barangay.clearance.shared.security;

import com.barangay.clearance.identity.entity.User;
import com.barangay.clearance.identity.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            UUID userId = jwtService.extractUserId(token);
            User.Role role = jwtService.extractRole(token);
            String email = jwtService.extractEmail(token);

            UserPrincipal principal = new UserPrincipal(userId, email, role);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal,
                    null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
