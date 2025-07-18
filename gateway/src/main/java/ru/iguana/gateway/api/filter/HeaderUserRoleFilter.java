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
import ru.iguana.gateway.api.dto.RoleDto;
import ru.iguana.gateway.api.dto.UserResponseDto;
import ru.iguana.gateway.api.service.RequestToIntegrationRolesService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class HeaderUserRoleFilter extends OncePerRequestFilter {

    private final RequestToIntegrationRolesService service;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String loginHeader = request.getHeader("login");
        log.debug("HeaderUserRoleFilter invoked for URI: {}", request.getRequestURI());

        if (loginHeader == null || loginHeader.isBlank()) {
            log.warn("Missing or blank 'login' header. Skipping authentication for URI: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        Long userId;
        try {
            userId = Long.parseLong(loginHeader);
            log.debug("Parsed login header as userId: {}", userId);
        } catch (NumberFormatException e) {
            log.error("Invalid login header format: '{}'. Expected numeric ID.", loginHeader);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid login header: must be numeric user ID");
            return;
        }

        try {
            log.debug("Calling service.getUsersRolesByIds with userId: {}", userId);
            Map<String, UserResponseDto> userMap = service.getUsersRolesByIds(List.of(userId));

            log.debug("Received user roles map: {}", userMap);

            UserResponseDto userDto = userMap.get(loginHeader);
            if (userDto == null) {
                log.warn("No user found for ID: {}", loginHeader);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("User not found");
                return;
            }

            log.debug("User DTO retrieved: {}", userDto);

            if (userDto.getRoles() == null || userDto.getRoles().isEmpty()) {
                log.warn("User {} has no roles assigned", loginHeader);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("User has no roles");
                return;
            }

            boolean hasValidRole = userDto.getRoles().stream()
                    .map(RoleDto::getName)
                    .anyMatch(role -> role.equalsIgnoreCase("base_user") || role.equalsIgnoreCase("admin"));

            if (!hasValidRole) {
                log.warn("User {} does not have required roles. Roles: {}", loginHeader, userDto.getRoles());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("User does not have required roles");
                return;
            }

            List<GrantedAuthority> authorities = userDto.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .collect(Collectors.toList());

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDto.getUserKey().getSub(), null, authorities
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            log.debug("At end of filter, SecurityContext holds auth: {}", currentAuth);

            log.info("Authentication successful for user ID {} (login: {}) with roles: {}", userId,
                    userDto.getUserKey().getSub(), authorities);




        } catch (Exception e) {
            log.error("Exception during authentication for login header '{}': {}", loginHeader, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Authentication failed: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }
}

