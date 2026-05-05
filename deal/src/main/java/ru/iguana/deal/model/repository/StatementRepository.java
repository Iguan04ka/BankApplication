package ru.iguana.deal.model.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.iguana.deal.model.entity.Statement;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatementRepository extends CrudRepository<Statement, UUID> {
    List<Statement> findAllByClientId(UUID clientId);

    @Query("""
        SELECT s FROM Statement s
        WHERE (CAST(:status AS string) IS NULL OR s.status = :status)
          AND (CAST(:from AS timestamp) IS NULL OR s.creationDate >= :from)
          AND (CAST(:to AS timestamp) IS NULL OR s.creationDate <= :to)
        ORDER BY s.creationDate DESC
        """)
    List<Statement> findFiltered(@Param("status") String status,
                                 @Param("from") Timestamp from,
                                 @Param("to") Timestamp to);

    @Query("SELECT s.status, COUNT(s) FROM Statement s GROUP BY s.status")
    List<Object[]> countByStatus();

    Optional<Statement> findByCredit(UUID creditId);

    @Query(value = "SELECT applied_offer->>'requestedAmount' FROM statement WHERE statement_id = :statementId", nativeQuery = true)
    String findAmountByStatementId(@Param("statementId") UUID statementId);

    @Query(value = "SELECT applied_offer->>'term' FROM statement WHERE statement_id = :statementId", nativeQuery = true)
    String findTermByStatementId(@Param("statementId") UUID statementId);

    @Query(value = "SELECT applied_offer->>'isInsuranceEnabled' FROM statement WHERE statement_id = :statementId", nativeQuery = true)
    String findIsInsuranceEnabledByStatementId(@Param("statementId") UUID statementId);

    @Query(value = "SELECT applied_offer->>'isSalaryClient' FROM statement WHERE statement_id = :statementId", nativeQuery = true)
    String findIsSalaryClientByStatementId(@Param("statementId") UUID statementId);
}
