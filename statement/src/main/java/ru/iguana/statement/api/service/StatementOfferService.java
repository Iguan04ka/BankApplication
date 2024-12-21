package ru.iguana.statement.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


@Service
@AllArgsConstructor
public class StatementOfferService {
    private final WebClient webClient;

    public ResponseEntity<Void> selectOffer(JsonNode request){
        sendOffer(request);
        return ResponseEntity.ok().build();
    }

    private void sendOffer(JsonNode request){
        webClient.post()
                .uri("/deal/offer/select")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe();
    }
}
