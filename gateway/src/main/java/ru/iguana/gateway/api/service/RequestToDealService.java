package ru.iguana.gateway.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import ru.iguana.gateway.api.dto.FinishRegistrationRequestDto;

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

    private void sendRequest(String uri, String statementId) {
        log.info("Sending request to: {}", uri);
        restClient.post()
                .uri(uri, statementId)
                .retrieve()
                .toBodilessEntity();
        log.info("Successfully sent request: {}", uri);
    }
}

