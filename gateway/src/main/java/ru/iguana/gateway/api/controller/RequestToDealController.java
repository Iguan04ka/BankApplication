package ru.iguana.gateway.api.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.gateway.api.dto.FinishRegistrationRequestDto;
import ru.iguana.gateway.api.service.RequestToDealService;

@RestController
@AllArgsConstructor
public class RequestToDealController {
    private final RequestToDealService requestToDealService;

    @PostMapping("statement/registration/{statementId}")
    public void finishRegistration(@RequestBody FinishRegistrationRequestDto finishRegistrationRequestDto,
                                   @PathVariable String statementId){
        requestToDealService.finishRegistration(
                finishRegistrationRequestDto,
                statementId);
    }

}
