package ru.iguana.gateway.api.controller;

import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Finish Registration", description = "Completes the registration process for a given statement.")
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
    @Operation(summary = "Send Documents", description = "Sends documents associated with a given statement.")
    public void sendDocuments(@PathVariable String statementId){
        log.info("A request has arrived for sendDocuments: {}", statementId);
        requestToDealService.sendDocuments(statementId);
        log.info("The request was sent successfully");
    }

    @PostMapping("document/{statementId}/sign")
    @Operation(summary = "Sign Documents", description = "Signs documents associated with a given statement.")
    public void signDocuments(@PathVariable String statementId){
        log.info("A request has arrived for signDocuments: {}", statementId);
        requestToDealService.signDocuments(statementId);
        log.info("The request was sent successfully");
    }

    @PostMapping("document/{statementId}/code")
    @Operation(summary = "Code Documents", description = "Codes documents associated with a given statement.")
    public void codeDocuments(@PathVariable String statementId){
        log.info("A request has arrived for codeDocuments: {}", statementId);
        requestToDealService.codeDocuments(statementId);
        log.info("The request was sent successfully");
    }
}
