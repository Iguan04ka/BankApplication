package ru.iguana.statement.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


@Service
@AllArgsConstructor
@Slf4j
public class StatementOfferService {
    private final WebClient webClient;

    public ResponseEntity<Void> selectOffer(JsonNode request) {
        log.info("Received request for selectOffer: {}", request);
        sendOffer(request);
        log.info("Completed processing selectOffer");
        return ResponseEntity.ok().build();
    }

    private void sendOffer(JsonNode request) {
        log.info("Sending offer request to external service: {}", request);
        webClient.post()
                .uri("/deal/offer/select")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(response -> log.info("Successfully sent offer request: {}", request))
                .doOnError(error -> log.error("Error occurred while sending offer request: {}", error.getMessage(), error))
                .subscribe();
    }

}
