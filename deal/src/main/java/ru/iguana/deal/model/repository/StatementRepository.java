package ru.iguana.deal.model.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.iguana.deal.model.entity.Statement;

import java.util.List;
import java.util.UUID;

public interface StatementRepository extends CrudRepository<Statement, UUID> {
    List<Statement> findAllByClientId(UUID clientId);

    @Query(value = "SELECT applied_offer->>'requestedAmount' FROM statement WHERE statement_id = :statementId", nativeQuery = true)
    String findAmountByStatementId(@Param("statementId") UUID statementId);

    @Query(value = "SELECT applied_offer->>'term' FROM statement WHERE statement_id = :statementId", nativeQuery = true)
    String findTermByStatementId(@Param("statementId") UUID statementId);

    @Query(value = "SELECT applied_offer->>'isInsuranceEnabled' FROM statement WHERE statement_id = :statementId", nativeQuery = true)
    String findIsInsuranceEnabledByStatementId(@Param("statementId") UUID statementId);

    @Query(value = "SELECT applied_offer->>'isSalaryClient' FROM statement WHERE statement_id = :statementId", nativeQuery = true)
    String findIsSalaryClientByStatementId(@Param("statementId") UUID statementId);
}
