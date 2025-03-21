package ru.iguana.gateway.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.iguana.gateway.api.dto.LoanStatementRequestDto;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class RequestToStatementService {
    private final WebClient webClient;

    public RequestToStatementService(@Autowired
                                     @Qualifier("statementWebClient")
                                     WebClient webClient){
        this.webClient = webClient;
    }

    public Mono<List<JsonNode>> getLoanOffer(LoanStatementRequestDto request) {
        log.info("Received request for getLoanOfferList");
        log.debug("Received request for getLoanOfferList: {}", request);
        return fetchLoanOffers(request)
                .doOnSuccess(response -> log.info("Response for getLoanOfferList: {}", response))
                .doOnError(error -> log.error("Error in getLoanOfferList: {}", error.getMessage(), error));
    }

    public ResponseEntity<Void> selectOffer(JsonNode request) {
        log.info("Received request for selectOffer: {}", request);
        sendOffer(request);
        log.info("Completed processing selectOffer");
        return ResponseEntity.ok().build();
    }

    private void sendOffer(JsonNode request) {
        log.info("Sending offer request to external service: {}", request);
        webClient.post()
                .uri("/statement/offer")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(response -> log.info("Successfully sent offer request: {}", request))
                .doOnError(error -> log.error("Error occurred while sending offer request: {}", error.getMessage(), error))
                .block();
    }

    private Mono<List<JsonNode>> fetchLoanOffers(LoanStatementRequestDto request) {
        log.info("Fetching loan offers with request");
        log.debug("Fetching loan offers with request: {}", request);
        return webClient.post()
                .uri("/statement")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(response -> {
                    if (response != null && response.isArray()) {
                        List<JsonNode> list = StreamSupport.stream(response.spliterator(), false)
                                .collect(Collectors.toList());
                        log.info("Successfully fetched and processed loan offers: {}", list);
                        return Mono.just(list);
                    } else {
                        log.error("Invalid response structure: expected JSON array, got: {}", response);
                        return Mono.error(new IllegalStateException("Invalid response structure: expected JSON array"));
                    }
                });
    }
}
