package ru.iguana.gateway.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.iguana.gateway.api.service.UserDocumentProxyService;

/**
 * Точка входа для frontend-приложения: маршруты работы с пользовательскими
 * PDF-документами. JWT-аутентификация выполняется в
 * {@link ru.iguana.gateway.api.filter.JwtAuthenticationFilter}, права
 * проверяются на уровне SecurityConfig и через {@link PreAuthorize}.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class UserDocumentController {

    private final UserDocumentProxyService documentProxyService;

    // ── Эндпоинты пользователя ──────────────────────────────────────────────

    @PreAuthorize("hasAuthority('base_user')")
    @GetMapping("/deal/client/me/documents")
    public ResponseEntity<JsonNode> listOwnDocuments() {
        log.info("GET /deal/client/me/documents");
        return ResponseEntity.ok(documentProxyService.listOwnDocuments());
    }

    @PreAuthorize("hasAuthority('base_user')")
    @PostMapping(value = "/deal/client/me/documents",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JsonNode> uploadOwnDocument(
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {
        log.info("POST /deal/client/me/documents (type={}, name={}, size={})",
                documentType, file.getOriginalFilename(), file.getSize());
        return ResponseEntity.ok(documentProxyService.uploadOwnDocument(documentType, file));
    }

    @PreAuthorize("hasAuthority('base_user')")
    @PutMapping(value = "/deal/client/me/documents/{documentId}",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JsonNode> replaceOwnDocument(
            @PathVariable String documentId,
            @RequestParam("file") MultipartFile file) {
        log.info("PUT /deal/client/me/documents/{} (name={}, size={})",
                documentId, file.getOriginalFilename(), file.getSize());
        return ResponseEntity.ok(documentProxyService.replaceOwnDocument(documentId, file));
    }

    @PreAuthorize("hasAuthority('base_user')")
    @DeleteMapping("/deal/client/me/documents/{documentId}")
    public ResponseEntity<Void> deleteOwnDocument(@PathVariable String documentId) {
        log.info("DELETE /deal/client/me/documents/{}", documentId);
        documentProxyService.deleteOwnDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('base_user')")
    @GetMapping("/deal/client/me/documents/{documentId}/content")
    public ResponseEntity<byte[]> downloadOwnDocument(
            @PathVariable String documentId,
            @RequestParam(name = "download", required = false, defaultValue = "false") boolean download) {
        log.info("GET /deal/client/me/documents/{}/content (download={})", documentId, download);
        return documentProxyService.downloadOwnDocument(documentId, download);
    }

    // ── Эндпоинты администратора ────────────────────────────────────────────

    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/admin/clients/{clientId}/documents")
    public ResponseEntity<JsonNode> adminListClientDocuments(@PathVariable String clientId) {
        log.info("GET /admin/clients/{}/documents", clientId);
        return ResponseEntity.ok(documentProxyService.adminListClientDocuments(clientId));
    }

    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/admin/documents/{documentId}/content")
    public ResponseEntity<byte[]> adminDownloadDocument(
            @PathVariable String documentId,
            @RequestParam(name = "download", required = false, defaultValue = "false") boolean download) {
        log.info("GET /admin/documents/{}/content (download={})", documentId, download);
        return documentProxyService.adminDownloadDocument(documentId, download);
    }

    // ── Автоматическая валидация документов (admin) ─────────────────────────
    // Маршруты ниже не конфликтуют с `/admin/documents/{documentId}/content`,
    // потому что суффикс пути отличается (/validate, /validation-result,
    // /validation-history). PathVariable {statementId} семантически — это
    // statementId, тип всё тот же UUID-строка.

    @PreAuthorize("hasAuthority('admin')")
    @PostMapping("/admin/documents/{statementId}/validate")
    public ResponseEntity<JsonNode> adminRevalidateDocuments(@PathVariable String statementId) {
        log.info("POST /admin/documents/{}/validate", statementId);
        return ResponseEntity.ok(documentProxyService.adminRevalidateDocuments(statementId));
    }

    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/admin/documents/{statementId}/validation-result")
    public ResponseEntity<JsonNode> adminGetValidationResult(@PathVariable String statementId) {
        log.info("GET /admin/documents/{}/validation-result", statementId);
        return ResponseEntity.ok(documentProxyService.adminGetValidationResult(statementId));
    }

    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/admin/documents/{statementId}/validation-history")
    public ResponseEntity<JsonNode> adminGetValidationHistory(@PathVariable String statementId) {
        log.info("GET /admin/documents/{}/validation-history", statementId);
        return ResponseEntity.ok(documentProxyService.adminGetValidationHistory(statementId));
    }
}
