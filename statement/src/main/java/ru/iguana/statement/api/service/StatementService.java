package ru.iguana.statement.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.iguana.statement.api.dto.LoanStatementRequestDto;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@AllArgsConstructor
@Slf4j
public class StatementService {

    private final WebClient webClient;

    public Mono<List<JsonNode>> getLoanOfferList(LoanStatementRequestDto request) {
        log.info("Received request for getLoanOfferList");
        log.debug("Received request for getLoanOfferList: {}", request);
        return fetchLoanOffers(request)
                .doOnSuccess(response -> log.info("Response for getLoanOfferList: {}", response))
                .doOnError(error -> log.error("Error in getLoanOfferList: {}", error.getMessage(), error));
    }

    private Mono<List<JsonNode>> fetchLoanOffers(LoanStatementRequestDto request) {
        log.info("Fetching loan offers with request");
        log.debug("Fetching loan offers with request: {}", request);
        return webClient.post()
                .uri("/deal/statement")
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

