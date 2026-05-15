package ru.iguana.deal.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.iguana.deal.model.entity.DocumentValidationResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentValidationResultRepository
        extends JpaRepository<DocumentValidationResult, UUID> {

    /** Полная история попыток валидации по заявке, новые сверху. */
    List<DocumentValidationResult> findAllByStatementIdOrderByValidatedAtDesc(UUID statementId);

    /** Последний (самый свежий) результат валидации для заявки. */
    Optional<DocumentValidationResult> findFirstByStatementIdOrderByValidatedAtDesc(UUID statementId);
}
