package ru.iguana.gateway.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.gateway.api.dto.RoleDto;
import ru.iguana.gateway.api.dto.UserResponseDto;
import ru.iguana.gateway.api.service.RequestToDealService;
import ru.iguana.gateway.api.service.RequestToIntegrationRolesService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single entry-point for all admin operations.
 * Path-level security in {@link ru.iguana.gateway.api.config.SecurityConfig}
 * already restricts {@code /admin/**} to {@code hasAuthority("admin")},
 * but we duplicate it here with {@code @PreAuthorize} for defense-in-depth.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('admin')")
public class AdminController {

    private final RequestToIntegrationRolesService rolesService;
    private final RequestToDealService dealService;

    // ── Dashboard ────────────────────────────────────────────────────────

    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> dashboardStats() {
        return ResponseEntity.ok(dealService.adminDashboardStats());
    }

    // ── Users ────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDto>> listUsers() {
        return ResponseEntity.ok(rolesService.adminListUsers());
    }

    @PutMapping("/users/{sub}/blocked")
    public ResponseEntity<UserResponseDto> setBlocked(@PathVariable String sub,
                                                      @RequestBody Map<String, Boolean> body) {
        Boolean blocked = body == null ? null : body.get("blocked");
        if (blocked == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(rolesService.adminSetBlocked(sub, blocked));
    }

    @PutMapping("/users/{sub}/roles")
    public ResponseEntity<UserResponseDto> setRoles(@PathVariable String sub,
                                                    @RequestBody Map<String, Set<String>> body) {
        Set<String> roleNames = body == null ? null : body.get("roleNames");
        if (roleNames == null || roleNames.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(rolesService.adminSetRoles(sub, roleNames));
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleDto>> listRoles() {
        return ResponseEntity.ok(rolesService.adminListRoles());
    }

    // ── Statements ───────────────────────────────────────────────────────

    @GetMapping("/statements")
    public ResponseEntity<?> listStatements(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) Long fromMillis,
                                            @RequestParam(required = false) Long toMillis) {
        return ResponseEntity.ok(dealService.adminListStatements(status, fromMillis, toMillis));
    }

    @GetMapping("/statements/{statementId}")
    public ResponseEntity<?> getStatementDetails(@PathVariable String statementId) {
        return ResponseEntity.ok(dealService.adminGetStatementDetails(statementId));
    }

    @PutMapping("/statements/{statementId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String statementId,
                                          @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(dealService.adminUpdateStatementStatus(statementId, body));
    }

    // ── Credits ──────────────────────────────────────────────────────────

    @GetMapping("/credits")
    public ResponseEntity<?> listCredits() {
        return ResponseEntity.ok(dealService.adminListCredits());
    }

    @GetMapping("/credits/{creditId}")
    public ResponseEntity<?> getCredit(@PathVariable String creditId) {
        return ResponseEntity.ok(dealService.adminGetCredit(creditId));
    }

    @GetMapping("/credits/{creditId}/statement")
    public ResponseEntity<?> getStatementByCreditId(@PathVariable String creditId) {
        return ResponseEntity.ok(dealService.adminGetStatementByCreditId(creditId));
    }
}
