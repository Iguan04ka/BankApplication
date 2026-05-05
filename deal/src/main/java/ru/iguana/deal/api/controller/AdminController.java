package ru.iguana.deal.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.deal.api.dto.AdminDashboardStatsDto;
import ru.iguana.deal.api.dto.AdminStatementDetailDto;
import ru.iguana.deal.api.dto.AdminStatementSummaryDto;
import ru.iguana.deal.api.dto.AdminUpdateStatusRequestDto;
import ru.iguana.deal.api.dto.CreditResponseDto;
import ru.iguana.deal.api.dto.StatementDto;
import ru.iguana.deal.api.service.AdminService;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    // ── statements ────────────────────────────────────────────────────────

    @GetMapping("/deal/admin/statement/{statementId}")
    public StatementDto getStatement(@PathVariable String statementId) {
        return adminService.getStatement(statementId);
    }

    @GetMapping("/deal/admin/statement/{statementId}/details")
    public AdminStatementDetailDto getStatementDetails(@PathVariable String statementId) {
        return adminService.getStatementDetail(statementId);
    }

    /** List statements with optional filters. */
    @GetMapping("/deal/admin/statement")
    public List<AdminStatementSummaryDto> listStatements(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long fromMillis,
            @RequestParam(required = false) Long toMillis) {
        Timestamp from = fromMillis == null ? null : new Timestamp(fromMillis);
        Timestamp to = toMillis == null ? null : new Timestamp(toMillis);
        return adminService.listStatements(status, from, to);
    }

    @PutMapping("/deal/admin/statement/{statementId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String statementId,
                                          @RequestBody AdminUpdateStatusRequestDto request) {
        try {
            return ResponseEntity.ok(adminService.updateStatementStatus(statementId, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // ── credits ───────────────────────────────────────────────────────────

    @GetMapping("/deal/admin/credit")
    public List<CreditResponseDto> listCredits() {
        return adminService.listCredits();
    }

    @GetMapping("/deal/admin/credit/{creditId}")
    public CreditResponseDto getCredit(@PathVariable String creditId) {
        return adminService.getCredit(creditId);
    }

    @GetMapping("/deal/admin/credit/{creditId}/statement")
    public ResponseEntity<?> getStatementByCreditId(@PathVariable String creditId) {
        try {
            return ResponseEntity.ok(adminService.getStatementByCreditId(creditId));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    // ── dashboard ─────────────────────────────────────────────────────────

    @GetMapping("/deal/admin/dashboard/stats")
    public AdminDashboardStatsDto dashboardStats() {
        return adminService.dashboardStats();
    }
}
