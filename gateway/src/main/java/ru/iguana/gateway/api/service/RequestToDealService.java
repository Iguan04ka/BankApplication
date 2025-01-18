package ru.iguana.gateway.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.iguana.gateway.api.dto.FinishRegistrationRequestDto;

import java.util.UUID;

@Service
@Slf4j
public class RequestToDealService {
    private final WebClient webClient;

    public RequestToDealService(@Autowired
                                @Qualifier("dealWebClient")
                                WebClient webClient){
        this.webClient = webClient;
    }

    public void finishRegistration(FinishRegistrationRequestDto finishRegistrationRequestDto,
                                   String statementId){

        log.info("Sending FinishRegistrationInfo request to deal service");
        webClient.post()
                .uri("/deal/calculate/{statementId}", statementId)
                .bodyValue(finishRegistrationRequestDto)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(response -> log.info("Successfully sent offer request: {}", finishRegistrationRequestDto))
                .doOnError(error -> log.error("Error occurred while sending offer request: {}", error.getMessage(), error))
                .block();
    }

    public void sendDocuments(String statementId){
        sendRequestToDealDocument("deal/document/{statementId}/send", statementId);
    }

    public void signDocuments(String statementId){
        sendRequestToDealDocument("deal/document/{statementId}/sign", statementId);
    }

    public void codeDocuments(String statementId){
        sendRequestToDealDocument("deal/document/{statementId}/code", statementId);
    }

    private void sendRequestToDealDocument(String uri, String statementId){
        webClient.post()
                .uri(uri, statementId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
