package ru.iguana.gateway.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.iguana.gateway.api.dto.LoginRequestDto;
import ru.iguana.gateway.api.dto.UserResponseDto;
import ru.iguana.gateway.api.service.JwtService;
import ru.iguana.gateway.api.service.RequestToIntegrationRolesService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RequestToIntegrationRolesService service;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        log.debug("JwtAuthenticationFilter invoked for URI: {}", request.getRequestURI());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        String sub;
        try {
            sub = jwtService.extractSub(token);
            log.debug("Extracted sub from token: {}", sub);
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid JWT token");
            return;
        }

        try {
            UserResponseDto userDto = service.getUserBySub(new LoginRequestDto(sub));

            if (userDto == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("User not found");
                return;
            }

            if (userDto.isBlocked()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("User is blocked");
                return;
            }

            List<GrantedAuthority> authorities = userDto.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .collect(Collectors.toList());

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDto.getUserKey().getSub(),
                    null,
                    authorities
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            log.info("JWT authentication successful for user: {} with roles: {}",
                    sub, authorities);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Authentication failed");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
