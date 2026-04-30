package ru.iguana.gateway.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.gateway.api.service.UserClientProxyService;

@RestController
@RequestMapping("/deal/client")
@RequiredArgsConstructor
@Slf4j
public class UserClientController {

    private final UserClientProxyService userClientProxyService;

    @PreAuthorize("hasAuthority('base_user')")
    @GetMapping("/me")
    public ResponseEntity<JsonNode> getProfile() {
        log.info("Received GET /deal/client/me");
        return ResponseEntity.ok(userClientProxyService.getProfile());
    }

    @PreAuthorize("hasAuthority('base_user')")
    @PatchMapping("/me")
    public ResponseEntity<JsonNode> updateProfile(@RequestBody JsonNode update) {
        log.info("Received PATCH /deal/client/me");
        return ResponseEntity.ok(userClientProxyService.updateProfile(update));
    }

    @PreAuthorize("hasAuthority('base_user')")
    @GetMapping("/me/statements")
    public ResponseEntity<JsonNode> getStatements() {
        log.info("Received GET /deal/client/me/statements");
        return ResponseEntity.ok(userClientProxyService.getStatements());
    }

    @PreAuthorize("hasAuthority('base_user')")
    @GetMapping("/me/statements/{statementId}")
    public ResponseEntity<JsonNode> getStatement(@PathVariable String statementId) {
        log.info("Received GET /deal/client/me/statements/{}", statementId);
        return ResponseEntity.ok(userClientProxyService.getStatement(statementId));
    }

    @PreAuthorize("hasAuthority('base_user')")
    @PostMapping("/me/statements/{statementId}/deny")
    public ResponseEntity<Void> denyStatement(@PathVariable String statementId) {
        log.info("Received POST /deal/client/me/statements/{}/deny", statementId);
        userClientProxyService.denyStatement(statementId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('base_user')")
    @GetMapping("/me/credits")
    public ResponseEntity<JsonNode> getCredits() {
        log.info("Received GET /deal/client/me/credits");
        return ResponseEntity.ok(userClientProxyService.getCredits());
    }

    @PreAuthorize("hasAuthority('base_user')")
    @GetMapping("/me/credits/{creditId}")
    public ResponseEntity<JsonNode> getCredit(@PathVariable String creditId) {
        log.info("Received GET /deal/client/me/credits/{}", creditId);
        return ResponseEntity.ok(userClientProxyService.getCredit(creditId));
    }

    @PreAuthorize("hasAuthority('base_user')")
    @GetMapping("/me/statements/{statementId}/offers")
    public ResponseEntity<JsonNode> getOffersForStatement(
            @PathVariable String statementId,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String term) {
        log.info("Received GET /deal/client/me/statements/{}/offers", statementId);
        return ResponseEntity.ok(userClientProxyService.getOffersForStatement(statementId, amount, term));
    }
}
