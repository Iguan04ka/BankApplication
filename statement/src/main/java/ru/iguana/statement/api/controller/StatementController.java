package ru.iguana.statement.api.controller;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.iguana.statement.api.dto.LoanStatementRequestDto;
import ru.iguana.statement.api.service.StatementOfferService;
import ru.iguana.statement.api.service.StatementService;

import java.util.List;

@RestController
@AllArgsConstructor
public class StatementController {
    private final StatementService statementService;

    private final StatementOfferService statementOfferService;

    @PostMapping("/statement")
    public Mono<ResponseEntity<List<JsonNode>>> getLoanOffers(@RequestBody @Validated
                                                                  LoanStatementRequestDto request) {
        return statementService.getLoanOfferList(request)
                .map(ResponseEntity::ok);
    }
    @PostMapping("/statement/offer")
    public ResponseEntity<Void> selectLoanOffer(@RequestBody JsonNode request){
        statementOfferService.selectOffer(request);
        return ResponseEntity.ok().build();
    }
}
