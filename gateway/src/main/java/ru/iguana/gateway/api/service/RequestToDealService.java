package ru.iguana.gateway.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import ru.iguana.gateway.api.dto.FinishRegistrationRequestDto;
import ru.iguana.gateway.api.dto.SesCodeRequestDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class RequestToDealService {

    private final RestClient restClient;

    public RequestToDealService(@Qualifier("dealRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void finishRegistration(FinishRegistrationRequestDto request, String statementId) {
        log.info("Sending finish registration for statement: {}", statementId);
        restClient.post()
                .uri("/deal/calculate/{statementId}", statementId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.info("Successfully finished registration.");
    }

    public void sendDocuments(String statementId) {
        sendRequest("/deal/document/{statementId}/send", statementId);
    }

    public void signDocuments(String statementId) {
        sendRequest("/deal/document/{statementId}/sign", statementId);
    }

    public void codeDocuments(String statementId) {
        sendRequest("/deal/document/{statementId}/code", statementId);
    }

    /**
     * Подтверждает SES-код и возвращает {@code ValidationResultDto} из deal-сервиса.
     * Deal автоматически запускает валидацию PDF-документов после проверки кода
     * и кладёт результат в тело ответа (поля: success, finalStatus, errors и т.д.).
     * Возвращаем тело как JsonNode, чтобы не вводить зависимость gateway от deal-DTO.
     */
    public com.fasterxml.jackson.databind.JsonNode verifySesCode(String statementId, SesCodeRequestDto request) {
        log.info("Sending SES code verification for statement: {}", statementId);
        com.fasterxml.jackson.databind.JsonNode result = restClient.post()
                .uri("/deal/document/{statementId}/verify", statementId)
                .body(request)
                .retrieve()
                .body(com.fasterxml.jackson.databind.JsonNode.class);
        log.info("Successfully verified SES code for statement: {}, validationSuccess={}",
                statementId,
                result != null && result.path("success").asBoolean(false));
        return result;
    }

    public void resendSesCode(String statementId) {
        sendRequest("/deal/document/{statementId}/resend-ses", statementId);
    }

    private void sendRequest(String uri, String statementId) {
        log.info("Sending request to: {}", uri);
        restClient.post()
                .uri(uri, statementId)
                .retrieve()
                .toBodilessEntity();
        log.info("Successfully sent request: {}", uri);
    }

    // ── Admin proxies ────────────────────────────────────────────────────

    public List<Map<String, Object>> adminListStatements(String status, Long fromMillis, Long toMillis) {
        return restClient.get()
                .uri(uriBuilder -> {
                    var ub = uriBuilder.path("/deal/admin/statement");
                    if (status != null && !status.isBlank()) ub.queryParam("status", status);
                    if (fromMillis != null) ub.queryParam("fromMillis", fromMillis);
                    if (toMillis != null) ub.queryParam("toMillis", toMillis);
                    return ub.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, Object> adminGetStatementDetails(String statementId) {
        return restClient.get()
                .uri("/deal/admin/statement/{id}/details", statementId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, Object> adminUpdateStatementStatus(String statementId, Map<String, Object> body) {
        return restClient.put()
                .uri("/deal/admin/statement/{id}/status", statementId)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<Map<String, Object>> adminListCredits() {
        return restClient.get()
                .uri("/deal/admin/credit")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, Object> adminGetCredit(String creditId) {
        return restClient.get()
                .uri("/deal/admin/credit/{id}", creditId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, Object> adminDashboardStats() {
        return restClient.get()
                .uri("/deal/admin/dashboard/stats")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, Object> adminGetStatementByCreditId(String creditId) {
        return restClient.get()
                .uri("/deal/admin/credit/{id}/statement", creditId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}

