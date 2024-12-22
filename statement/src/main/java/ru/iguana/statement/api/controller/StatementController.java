package ru.iguana.statement.api.controller;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class StatementController {
    private final StatementService statementService;

    private final StatementOfferService statementOfferService;

    @PostMapping("/statement")
    public Mono<ResponseEntity<List<JsonNode>>> getLoanOffers(@RequestBody @Validated
                                                              LoanStatementRequestDto request) {
        log.info("Received request for getLoanOffers");
        log.debug("Received request for getLoanOffers: {}", request);
        return statementService.getLoanOfferList(request)
                .doOnNext(response -> {
                    log.debug("Response for getLoanOffers: {}", response);
                    log.info("Response for getLoanOffers");
                })
                .map(ResponseEntity::ok);
    }
    @PostMapping("/statement/offer")
    public ResponseEntity<Void> selectLoanOffer(@RequestBody JsonNode request){
        log.info("Received request for selectLoanOffer");
        log.debug("Received request for selectLoanOffer: {}", request);
        statementOfferService.selectOffer(request);
        log.info("Completed processing selectLoanOffer");
        return ResponseEntity.ok().build();
    }
}
