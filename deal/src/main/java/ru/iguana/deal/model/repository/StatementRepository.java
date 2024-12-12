package ru.iguana.deal.model.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.iguana.deal.model.entity.Statement;

import java.util.UUID;

public interface StatementRepository extends CrudRepository<Statement, UUID> {
    @Query(value = "SELECT applied_offer->>'requestedAmount' AS amount FROM statement WHERE client_id = :clientId", nativeQuery = true)
    String findAmountByClientId(@Param("clientId") UUID clientId);
    @Query(value = "SELECT applied_offer->>'term' AS term FROM statement WHERE client_id = :clientId", nativeQuery = true)
    String findTermByClientId(@Param("clientId") UUID clientId);

    @Query(value = "SELECT applied_offer->>'isInsuranceEnabled' AS isInsuranceEnabled FROM statement WHERE client_id = :clientId", nativeQuery = true)
    String findIsInsuranceEnabledByClientId(@Param("clientId") UUID clientId);

    @Query(value = "SELECT applied_offer->>'isSalaryClient' AS isSalaryClient FROM statement WHERE client_id = :clientId", nativeQuery = true)
    String findIsSalaryClientByClientId(@Param("clientId") UUID clientId);
}
