package ru.iguana.deal.api.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.iguana.deal.model.entity.Jsonb.StatusHistory;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.enums.ApplicationStatus;
import ru.iguana.deal.model.entity.enums.ChangeType;
import ru.iguana.deal.model.repository.StatementRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class SesCodeService {

    private final StatementRepository statementRepository;

    @Transactional
    public void verifyCode(UUID statementId, String code) {
        log.info("Verifying SES code for statementId: {}", statementId);

        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code is required");
        }

        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));

        String storedCode = statement.getSesCode();
        Timestamp expiresAt = statement.getSesCodeExpiresAt();

        if (storedCode == null || expiresAt == null) {
            log.warn("No SES code issued for statementId: {}", statementId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No code was issued for this statement");
        }

        if (Instant.now().isAfter(expiresAt.toInstant())) {
            log.warn("SES code expired for statementId: {}", statementId);
            throw new ResponseStatusException(HttpStatus.GONE, "Confirmation code has expired");
        }

        if (!storedCode.equals(code.trim())) {
            log.warn("SES code mismatch for statementId: {}", statementId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid confirmation code");
        }

        statement.setStatus(String.valueOf(ApplicationStatus.DOCUMENT_SIGNED));
        statement.getStatusHistory().add(new StatusHistory(
                ApplicationStatus.DOCUMENT_SIGNED,
                Timestamp.from(Instant.now()),
                ChangeType.AUTOMATIC));
        statement.setSignDate(Timestamp.from(Instant.now()));
        // Invalidate the code so it cannot be reused
        statement.setSesCode(null);
        statement.setSesCodeExpiresAt(null);

        statementRepository.save(statement);
        log.info("SES code verified, statement marked DOCUMENT_SIGNED for id: {}", statementId);
    }
}
