package ru.iguana.statement.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
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
public class StatementService {

    private final WebClient webClient;

    public Mono<List<JsonNode>> getLoanOfferList(LoanStatementRequestDto request) {
        return fetchLoanOffers(request); // Возвращаем реактивный поток данных
    }

    private Mono<List<JsonNode>> fetchLoanOffers(LoanStatementRequestDto request) {
        return webClient.post()
                .uri("/deal/statement")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class) // Получаем реактивный Mono<JsonNode>
                .flatMap(response -> { // Работаем с ответом реактивно
                    if (response != null && response.isArray()) {
                        // Преобразуем JSON-ответ в список
                        List<JsonNode> list = StreamSupport.stream(response.spliterator(), false)
                                .collect(Collectors.toList());
                        return Mono.just(list);
                    } else {
                        return Mono.error(new IllegalStateException("Invalid response structure: expected JSON array"));
                    }
                });
    }
}

