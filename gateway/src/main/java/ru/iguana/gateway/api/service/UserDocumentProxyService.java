package ru.iguana.gateway.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Прокси-сервис для работы с пользовательскими документами через deal-сервис.
 * Перенаправляет multipart-загрузку, скачивание бинарного содержимого и
 * операции над метаданными.
 */
@Service
@Slf4j
public class UserDocumentProxyService {

    private static final String USER_SUB_HEADER = "X-User-Sub";

    private final RestClient restClient;

    public UserDocumentProxyService(@Qualifier("dealRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    // ── От лица пользователя ────────────────────────────────────────────────

    public JsonNode listOwnDocuments() {
        return execute(() -> restClient.get()
                .uri("/deal/client/me/documents")
                .header(USER_SUB_HEADER, getUserSub())
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode uploadOwnDocument(String documentType, MultipartFile file) {
        return execute(() -> restClient.post()
                .uri("/deal/client/me/documents")
                .header(USER_SUB_HEADER, getUserSub())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(buildMultipartBody(file, documentType))
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode replaceOwnDocument(String documentId, MultipartFile file) {
        return execute(() -> restClient.put()
                .uri("/deal/client/me/documents/{id}", documentId)
                .header(USER_SUB_HEADER, getUserSub())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(buildMultipartBody(file, null))
                .retrieve()
                .body(JsonNode.class));
    }

    public void deleteOwnDocument(String documentId) {
        execute(() -> {
            restClient.delete()
                    .uri("/deal/client/me/documents/{id}", documentId)
                    .header(USER_SUB_HEADER, getUserSub())
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    public ResponseEntity<byte[]> downloadOwnDocument(String documentId, boolean asAttachment) {
        return downloadGeneric(
                "/deal/client/me/documents/{id}/content",
                documentId,
                asAttachment,
                true);
    }

    // ── От лица администратора ──────────────────────────────────────────────

    public JsonNode adminListClientDocuments(String clientId) {
        return execute(() -> restClient.get()
                .uri("/deal/admin/clients/{clientId}/documents", clientId)
                .retrieve()
                .body(JsonNode.class));
    }

    public ResponseEntity<byte[]> adminDownloadDocument(String documentId, boolean asAttachment) {
        return downloadGeneric(
                "/deal/admin/documents/{id}/content",
                documentId,
                asAttachment,
                false);
    }

    // ── Внутреннее ──────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> downloadGeneric(String uri,
                                                   String documentId,
                                                   boolean asAttachment,
                                                   boolean withUserSub) {
        return execute(() -> {
            var spec = restClient.get()
                    .uri(uri + "?download={d}", documentId, asAttachment);
            if (withUserSub) {
                spec = spec.header(USER_SUB_HEADER, getUserSub());
            }
            ResponseEntity<byte[]> response = spec.retrieve().toEntity(byte[].class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String cd = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            if (cd != null) {
                headers.set(HttpHeaders.CONTENT_DISPOSITION, cd);
            }
            return ResponseEntity.status(response.getStatusCode())
                    .headers(headers)
                    .body(response.getBody());
        });
    }

    private MultiValueMap<String, Object> buildMultipartBody(MultipartFile file, String documentType) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", resource);
        } catch (IOException ex) {
            log.error("Failed to read uploaded file for forwarding: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot read uploaded file");
        }
        if (documentType != null) {
            body.add("documentType", documentType);
        }
        return body;
    }

    private String getUserSub() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private <T> T execute(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            log.error("Deal service returned error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString());
        }
    }
}
