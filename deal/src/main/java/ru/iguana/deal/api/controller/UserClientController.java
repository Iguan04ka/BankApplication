package ru.iguana.deal.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.iguana.deal.api.dto.ClientProfileDto;
import ru.iguana.deal.api.dto.ClientUpdateRequestDto;
import ru.iguana.deal.api.dto.CreditResponseDto;
import ru.iguana.deal.api.dto.StatementDetailDto;
import ru.iguana.deal.api.dto.StatementShortDto;
import ru.iguana.deal.api.service.StatementService;
import ru.iguana.deal.api.service.UserClientService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/deal/client")
@RequiredArgsConstructor
@Slf4j
public class UserClientController {

    private final UserClientService userClientService;
    private final StatementService statementService;

    @GetMapping("/me")
    public ResponseEntity<ClientProfileDto> getProfile(
            @RequestHeader("X-User-Sub") String userSub) {
        log.info("GET /deal/client/me");
        return ResponseEntity.ok(userClientService.getProfile(userSub));
    }

    @PatchMapping("/me")
    public ResponseEntity<ClientProfileDto> updateProfile(
            @RequestHeader("X-User-Sub") String userSub,
            @RequestBody ClientUpdateRequestDto update) {
        log.info("PATCH /deal/client/me");
        return ResponseEntity.ok(userClientService.updateProfile(userSub, update));
    }

    @GetMapping("/me/statements")
    public ResponseEntity<List<StatementShortDto>> getStatements(
            @RequestHeader("X-User-Sub") String userSub) {
        log.info("GET /deal/client/me/statements");
        return ResponseEntity.ok(userClientService.getStatements(userSub));
    }

    @GetMapping("/me/statements/{statementId}")
    public ResponseEntity<StatementDetailDto> getStatement(
            @RequestHeader("X-User-Sub") String userSub,
            @PathVariable UUID statementId) {
        log.info("GET /deal/client/me/statements/{}", statementId);
        return ResponseEntity.ok(userClientService.getStatement(userSub, statementId));
    }

    @PostMapping("/me/statements/{statementId}/deny")
    public ResponseEntity<Void> denyStatement(
            @RequestHeader("X-User-Sub") String userSub,
            @PathVariable UUID statementId) {
        log.info("POST /deal/client/me/statements/{}/deny", statementId);
        userClientService.denyStatement(userSub, statementId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/credits")
    public ResponseEntity<List<CreditResponseDto>> getCredits(
            @RequestHeader("X-User-Sub") String userSub) {
        log.info("GET /deal/client/me/credits");
        return ResponseEntity.ok(userClientService.getCredits(userSub));
    }

    @GetMapping("/me/credits/{creditId}")
    public ResponseEntity<CreditResponseDto> getCredit(
            @RequestHeader("X-User-Sub") String userSub,
            @PathVariable UUID creditId) {
        log.info("GET /deal/client/me/credits/{}", creditId);
        return ResponseEntity.ok(userClientService.getCredit(userSub, creditId));
    }

    @GetMapping("/me/statements/{statementId}/offers")
    public ResponseEntity<List<JsonNode>> getOffersForStatement(
            @RequestHeader("X-User-Sub") String userSub,
            @PathVariable UUID statementId,
            @RequestParam(required = false) java.math.BigDecimal amount,
            @RequestParam(required = false) Integer term) {
        log.info("GET /deal/client/me/statements/{}/offers (amount={}, term={})", statementId, amount, term);
        return statementService.getOffersForPreapprovalStatement(statementId, userSub, amount, term);
    }
}
