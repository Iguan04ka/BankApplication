package ru.iguana.deal.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.iguana.deal.api.dto.UserDocumentDto;
import ru.iguana.deal.api.service.UserDocumentService;
import ru.iguana.deal.model.entity.UserDocument;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * REST API для работы с пользовательскими документами (PDF). Маршруты
 * разделены на две группы — операции от лица пользователя
 * ({@code /deal/client/me/documents/...}) и операции от лица администратора
 * ({@code /deal/admin/...}).
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class UserDocumentController {

    private final UserDocumentService userDocumentService;

    // ════════════════════════════════════════════════════════════════════════
    //  Эндпоинты пользователя
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/deal/client/me/documents")
    public ResponseEntity<List<UserDocumentDto>> listOwnDocuments(
            @RequestHeader("X-User-Sub") String userSub) {
        log.info("GET /deal/client/me/documents (userSub={})", userSub);
        return ResponseEntity.ok(userDocumentService.listOwnDocuments(userSub));
    }

    @PostMapping(value = "/deal/client/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDocumentDto> uploadOwnDocument(
            @RequestHeader("X-User-Sub") String userSub,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {
        log.info("POST /deal/client/me/documents (userSub={}, type={}, name={}, size={})",
                userSub, documentType, file.getOriginalFilename(), file.getSize());
        return ResponseEntity.ok(userDocumentService.uploadOwnDocument(userSub, documentType, file));
    }

    @GetMapping("/deal/client/me/documents/{documentId}/content")
    public ResponseEntity<Resource> downloadOwnDocument(
            @RequestHeader("X-User-Sub") String userSub,
            @PathVariable UUID documentId,
            @RequestParam(name = "download", required = false, defaultValue = "false") boolean download) {
        log.info("GET /deal/client/me/documents/{}/content (userSub={}, download={})",
                documentId, userSub, download);
        UserDocument doc = userDocumentService.getOwnDocumentForDownload(userSub, documentId);
        return buildPdfResponse(doc, download);
    }

    @PutMapping(value = "/deal/client/me/documents/{documentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDocumentDto> replaceOwnDocument(
            @RequestHeader("X-User-Sub") String userSub,
            @PathVariable UUID documentId,
            @RequestParam("file") MultipartFile file) {
        log.info("PUT /deal/client/me/documents/{} (userSub={}, name={}, size={})",
                documentId, userSub, file.getOriginalFilename(), file.getSize());
        return ResponseEntity.ok(userDocumentService.replaceOwnDocument(userSub, documentId, file));
    }

    @DeleteMapping("/deal/client/me/documents/{documentId}")
    public ResponseEntity<Void> deleteOwnDocument(
            @RequestHeader("X-User-Sub") String userSub,
            @PathVariable UUID documentId) {
        log.info("DELETE /deal/client/me/documents/{} (userSub={})", documentId, userSub);
        userDocumentService.deleteOwnDocument(userSub, documentId);
        return ResponseEntity.noContent().build();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Эндпоинты администратора
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/deal/admin/clients/{clientId}/documents")
    public ResponseEntity<List<UserDocumentDto>> listClientDocuments(@PathVariable UUID clientId) {
        log.info("GET /deal/admin/clients/{}/documents", clientId);
        return ResponseEntity.ok(userDocumentService.listClientDocuments(clientId));
    }

    @GetMapping("/deal/admin/documents/{documentId}/content")
    public ResponseEntity<Resource> adminDownloadDocument(
            @PathVariable UUID documentId,
            @RequestParam(name = "download", required = false, defaultValue = "false") boolean download) {
        log.info("GET /deal/admin/documents/{}/content (download={})", documentId, download);
        UserDocument doc = userDocumentService.getAnyDocumentForDownload(documentId);
        return buildPdfResponse(doc, download);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Утилиты
    // ════════════════════════════════════════════════════════════════════════

    private ResponseEntity<Resource> buildPdfResponse(UserDocument doc, boolean download) {
        ByteArrayResource body = new ByteArrayResource(doc.getContent());
        String filename = doc.getOriginalFileName() != null
                ? doc.getOriginalFileName()
                : "document.pdf";
        ContentDisposition disposition = ContentDisposition
                .builder(download ? "attachment" : "inline")
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposition);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(doc.getFileSize() != null ? doc.getFileSize() : doc.getContent().length)
                .headers(headers)
                .body(body);
    }
}
