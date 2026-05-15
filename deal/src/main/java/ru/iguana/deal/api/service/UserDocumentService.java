package ru.iguana.deal.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.iguana.deal.api.dto.UserDocumentDto;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.UserDocument;
import ru.iguana.deal.model.entity.enums.UserDocumentType;
import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.UserDocumentRepository;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDocumentService {

    /** Максимальный размер одного документа — 10 МБ. */
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final String PDF_MIME = "application/pdf";
    private static final String PDF_EXT = ".pdf";

    private final UserDocumentRepository documentRepository;
    private final ClientRepository clientRepository;

    // ── Операции от лица пользователя ────────────────────────────────────────

    public List<UserDocumentDto> listOwnDocuments(String userSub) {
        Client client = findClientByUserSub(userSub);
        return documentRepository.findAllByClientId(client.getClientId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public UserDocumentDto uploadOwnDocument(String userSub, String documentType, MultipartFile file) {
        Client client = findClientByUserSub(userSub);
        return saveOrReplace(client.getClientId(), documentType, file);
    }

    public UserDocument getOwnDocumentForDownload(String userSub, UUID documentId) {
        Client client = findClientByUserSub(userSub);
        UserDocument doc = findDocumentOrThrow(documentId);
        if (!doc.getClientId().equals(client.getClientId())) {
            log.warn("Access denied: document {} does not belong to userSub: {}", documentId, userSub);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return doc;
    }

    public UserDocumentDto replaceOwnDocument(String userSub, UUID documentId, MultipartFile file) {
        Client client = findClientByUserSub(userSub);
        UserDocument doc = findDocumentOrThrow(documentId);
        if (!doc.getClientId().equals(client.getClientId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        validateFile(file);
        try {
            doc.setOriginalFileName(file.getOriginalFilename())
               .setMimeType(file.getContentType())
               .setFileSize(file.getSize())
               .setContent(file.getBytes())
               .setUploadedAt(Timestamp.from(Instant.now()));
        } catch (IOException ex) {
            log.error("Failed to read uploaded file for replacement: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot read uploaded file");
        }
        documentRepository.save(doc);
        log.info("Document {} replaced by userSub: {}", documentId, userSub);
        return toDto(doc);
    }

    public void deleteOwnDocument(String userSub, UUID documentId) {
        Client client = findClientByUserSub(userSub);
        UserDocument doc = findDocumentOrThrow(documentId);
        if (!doc.getClientId().equals(client.getClientId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        documentRepository.delete(doc);
        log.info("Document {} deleted by userSub: {}", documentId, userSub);
    }

    // ── Операции от лица администратора ─────────────────────────────────────

    public List<UserDocumentDto> listClientDocuments(UUID clientId) {
        return documentRepository.findAllByClientId(clientId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public UserDocument getAnyDocumentForDownload(UUID documentId) {
        return findDocumentOrThrow(documentId);
    }

    // ── Внутренние помощники ────────────────────────────────────────────────

    private UserDocumentDto saveOrReplace(UUID clientId, String documentTypeRaw, MultipartFile file) {
        String documentType = normalizeType(documentTypeRaw);
        validateFile(file);

        UserDocument doc = documentRepository
                .findByClientIdAndDocumentType(clientId, documentType)
                .orElseGet(UserDocument::new);

        try {
            doc.setClientId(clientId)
               .setDocumentType(documentType)
               .setOriginalFileName(file.getOriginalFilename())
               .setMimeType(file.getContentType())
               .setFileSize(file.getSize())
               .setContent(file.getBytes())
               .setUploadedAt(Timestamp.from(Instant.now()));
        } catch (IOException ex) {
            log.error("Failed to read uploaded file: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot read uploaded file");
        }
        documentRepository.save(doc);
        log.info("Document type={} stored for clientId={}, size={} bytes",
                documentType, clientId, doc.getFileSize());
        return toDto(doc);
    }

    private String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "documentType is required");
        }
        try {
            return UserDocumentType.valueOf(raw.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown documentType: " + raw + ". Allowed: NDFL_2, EMPLOYMENT_RECORD");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "File is too large. Max allowed: " + (MAX_FILE_SIZE / (1024 * 1024)) + " MB");
        }
        String mime = file.getContentType();
        if (mime == null || !mime.equalsIgnoreCase(PDF_MIME)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only PDF files are allowed (mime=" + mime + ")");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(PDF_EXT)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only files with .pdf extension are allowed");
        }
    }

    private Client findClientByUserSub(String userSub) {
        return clientRepository.findByUserSub(userSub)
                .orElseThrow(() -> {
                    log.warn("Client not found for userSub: {}", userSub);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Client profile not found");
                });
    }

    private UserDocument findDocumentOrThrow(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    private UserDocumentDto toDto(UserDocument d) {
        return new UserDocumentDto()
                .setDocumentId(d.getDocumentId())
                .setClientId(d.getClientId())
                .setDocumentType(d.getDocumentType())
                .setOriginalFileName(d.getOriginalFileName())
                .setMimeType(d.getMimeType())
                .setFileSize(d.getFileSize())
                .setUploadedAt(d.getUploadedAt());
    }
}
