package ru.iguana.gateway.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.iguana.gateway.api.dto.LoanStatementRequestDto;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class RequestToStatementService {

    private final RestClient restClient;

    public RequestToStatementService(@Qualifier("statementRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<JsonNode> getLoanOffer(LoanStatementRequestDto request) {
        log.info("Fetching loan offers with request: {}", request);
        JsonNode response = restClient.post()
                .uri("/statement")
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        if (response != null && response.isArray()) {
            List<JsonNode> offers = StreamSupport.stream(response.spliterator(), false).toList();
            log.info("Successfully fetched loan offers: {}", offers);
            return offers;
        } else {
            log.error("Invalid response structure: expected array, got: {}", response);
            throw new IllegalStateException("Invalid response: expected array");
        }
    }

    public void selectOffer(JsonNode request) {
        log.info("Sending selected offer: {}", request);
        restClient.post()
                .uri("/statement/offer")
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.info("Successfully sent selected offer.");
    }
}
