package ru.iguana.deal.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.iguana.deal.api.dto.ValidationResultDto;
import ru.iguana.deal.api.service.validation.DocumentValidationService;

import java.util.List;
import java.util.UUID;

/**
 * Административные эндпоинты для работы с автоматической валидацией
 * пользовательских PDF-документов.
 *
 * <p>Маршруты лежат под префиксом {@code /deal/admin/documents/}, который
 * на gateway-уровне доступен только пользователям с ролью {@code admin}.
 * Эндпоинты позволяют:
 * <ul>
 *     <li>посмотреть последний результат валидации заявки;</li>
 *     <li>получить полную историю попыток валидации;</li>
 *     <li>повторно запустить валидацию вручную (после загрузки исправленных
 *     документов или для отладки).</li>
 * </ul>
 *
 * <p>Сам автоматический запуск валидации в основном потоке выполняется
 * в {@code SesCodeService#verifyCode}; данный контроллер обслуживает
 * только админ-сценарии.
 */
@RestController
@RequestMapping("/deal/admin/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentValidationController {

    private final DocumentValidationService documentValidationService;

    /**
     * Повторный запуск автоматической валидации по заявке. Полезен, когда
     * клиент перезалил PDF-документы, а заявка по-прежнему находится в
     * статусе {@code DOCUMENT_SIGNED} и ожидает ручного рассмотрения.
     *
     * <p>Каждый вызов создаёт новую запись в истории валидации и при
     * успехе переводит заявку в {@code CREDIT_ISSUED} автоматически.
     */
    @PostMapping("/{statementId}/validate")
    @Operation(summary = "Re-run automatic document validation",
            description = "Перезапускает автоматическую проверку PDF-документов по заявке " +
                    "и обновляет её статус по результатам. Возвращает свежий ValidationResultDto.")
    public ResponseEntity<ValidationResultDto> revalidate(@PathVariable UUID statementId) {
        log.info("ADMIN: повторная валидация документов для statementId={}", statementId);
        ValidationResultDto result = documentValidationService.validateAndApply(statementId);
        return ResponseEntity.ok(result);
    }

    /**
     * Возвращает последний сохранённый результат валидации без перезапуска.
     * Если валидация ни разу не выполнялась — 404.
     */
    @GetMapping("/{statementId}/validation-result")
    @Operation(summary = "Last automatic validation result for a statement")
    public ResponseEntity<ValidationResultDto> getLast(@PathVariable UUID statementId) {
        log.info("ADMIN: запрос последнего результата валидации для statementId={}", statementId);
        return documentValidationService.getLastResult(statementId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "По данной заявке автоматическая валидация ещё не запускалась"));
    }

    /**
     * Полная история попыток автоматической валидации, новые сверху.
     * Если истории нет, возвращает пустой список (а не 404).
     */
    @GetMapping("/{statementId}/validation-history")
    @Operation(summary = "Full validation history for a statement (newest first)")
    public ResponseEntity<List<ValidationResultDto>> getHistory(@PathVariable UUID statementId) {
        log.info("ADMIN: запрос истории валидаций для statementId={}", statementId);
        return ResponseEntity.ok(documentValidationService.getHistory(statementId));
    }
}
