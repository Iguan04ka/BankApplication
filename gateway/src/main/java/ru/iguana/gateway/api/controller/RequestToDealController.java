package ru.iguana.gateway.api.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.gateway.api.dto.FinishRegistrationRequestDto;
import ru.iguana.gateway.api.service.RequestToDealService;

@RestController
@AllArgsConstructor
@Slf4j
public class RequestToDealController {
    private final RequestToDealService requestToDealService;

    @PostMapping("statement/registration/{statementId}")
    public void finishRegistration(@RequestBody FinishRegistrationRequestDto finishRegistrationRequestDto,
                                   @PathVariable String statementId){
        log.info("A request has arrived for finishRegistration");
        log.debug("A request has arrived for finishRegistration: {}, {}", finishRegistrationRequestDto, statementId);
        requestToDealService.finishRegistration(
                finishRegistrationRequestDto,
                statementId);
        log.info("the request was sent successfully");
    }

    @PostMapping("document/{statementId}")
    public void sendDocuments(@PathVariable String statementId){
        log.info("A request has arrived for sendDocuments: {}", statementId);
        requestToDealService.sendDocuments(statementId);
        log.info("The request was sent successfully");
    }

    @PostMapping("document/{statementId}/sign")
    public void signDocuments(@PathVariable String statementId){
        log.info("A request has arrived for signDocuments: {}", statementId);
        requestToDealService.signDocuments(statementId);
        log.info("The request was sent successfully");
    }

    @PostMapping("document/{statementId}/code")
    public void codeDocuments(@PathVariable String statementId){
        log.info("A request has arrived for codeDocuments: {}", statementId);
        requestToDealService.codeDocuments(statementId);
        log.info("The request was sent successfully");
    }

}
