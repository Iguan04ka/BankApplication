package ru.iguana.deal.api.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.iguana.deal.api.dto.EmailMessageDto;
import ru.iguana.deal.api.dto.ValidationResultDto;
import ru.iguana.deal.api.service.validation.DocumentValidationService;
import ru.iguana.deal.kafka.KafkaProducer;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Jsonb.StatusHistory;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.enums.ApplicationStatus;
import ru.iguana.deal.model.entity.enums.ChangeType;
import ru.iguana.deal.model.entity.enums.EmailTheme;
import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.CreditRepository;
import ru.iguana.deal.model.repository.StatementRepository;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class SesCodeService {

    private static final int TTL_MINUTES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StatementRepository statementRepository;
    private final ClientRepository clientRepository;
    private final CreditRepository creditRepository;
    private final KafkaProducer kafkaProducer;
    private final DocumentValidationService documentValidationService;

    /**
     * Проверяет SES-код пользователя. При успешной проверке выполняет два шага:
     * <ol>
     *     <li>переводит заявку в статус {@code DOCUMENT_SIGNED} и фиксирует
     *     момент подписания;</li>
     *     <li>запускает автоматическую валидацию загруженных PDF-документов
     *     через {@link DocumentValidationService#validateAndApply(UUID)}.</li>
     * </ol>
     *
     * <p>Результат валидации определяет, переключится ли заявка дальше
     * в {@code CREDIT_ISSUED} автоматически, либо останется в
     * {@code DOCUMENT_SIGNED} и уйдёт на ручную проверку менеджером.
     * Возвращаемый {@link ValidationResultDto} используется фронтендом
     * для выбора одного из двух финальных окон.
     */
    @Transactional
    public ValidationResultDto verifyCode(UUID statementId, String code) {
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

        // Запуск автоматической валидации документов. Любая внутренняя ошибка
        // здесь не должна откатывать смену статуса DOCUMENT_SIGNED — поэтому
        // оборачиваем в try/catch и при поломке возвращаем «безопасный» отрицательный
        // результат, а заявка уйдёт на ручную проверку.
        try {
            ValidationResultDto result = documentValidationService.validateAndApply(statementId);
            log.info("Document validation completed for statementId={}: success={}, finalStatus={}, errors={}",
                    statementId, result.getSuccess(), result.getFinalStatus(),
                    result.getErrors() == null ? 0 : result.getErrors().size());
            return result;
        } catch (Exception ex) {
            log.error("Document validation failed unexpectedly for statementId={}: {}",
                    statementId, ex.getMessage(), ex);
            // Не пробрасываем исключение наружу: пользователь подписал документы,
            // заявка осталась в DOCUMENT_SIGNED, менеджер разберётся вручную.
            return new ValidationResultDto()
                    .setStatementId(statementId)
                    .setSuccess(false)
                    .setFinalStatus(ApplicationStatus.DOCUMENT_SIGNED.name())
                    .setValidatedAt(Timestamp.from(Instant.now()))
                    .setErrors(List.of());
        }
    }

    @Transactional
    public void resendCode(UUID statementId) {
        log.info("Resending SES code for statementId: {}", statementId);

        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));

        if (!ApplicationStatus.CC_APPROVED.name().equals(statement.getStatus())) {
            log.warn("Resend rejected: statement {} is in status {}, expected CC_APPROVED",
                    statementId, statement.getStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot resend code: statement is not awaiting confirmation");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(TTL_MINUTES, ChronoUnit.MINUTES));
        statement.setSesCode(code);
        statement.setSesCodeExpiresAt(expiresAt);
        statementRepository.save(statement);
        log.info("New SES code generated for statementId: {} (expires at {})", statementId, expiresAt);

        Client client = clientRepository.findById(statement.getClientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        EmailMessageDto message = new EmailMessageDto()
                .setAddress(client.getEmail())
                .setTheme(EmailTheme.SEND_SES)
                .setStatementId(statement.getStatementId())
                .setText("Код подтверждения оформления кредита")
                .setFirstName(client.getFirstName())
                .setMiddleName(client.getMiddleName())
                .setCode(code)
                .setTtlMinutes(TTL_MINUTES);

        if (statement.getCredit() != null) {
            creditRepository.findById(statement.getCredit()).ifPresent(credit ->
                    message.setAmount(credit.getAmount())
                           .setTerm(credit.getTerm())
                           .setMonthlyPayment(credit.getMonthlyPayment()));
        }

        kafkaProducer.sendMessageToSendSesTopic(message);
        log.info("SES code resent successfully for statementId: {}", statementId);
    }
}
