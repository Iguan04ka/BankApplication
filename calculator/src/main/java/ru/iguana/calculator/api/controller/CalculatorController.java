package ru.iguana.calculator.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.calculator.api.dto.*;
import ru.iguana.calculator.api.service.CalculateCreditService;
import ru.iguana.calculator.api.service.LoanOfferService;

import java.util.List;

@Slf4j
@RestController
public class CalculatorController {
    @Autowired
    private LoanOfferService loanOfferService;

    @Autowired
    private CalculateCreditService calculateCreditService;

    public static final String OFFERS = "/calculator/offers";

    public static final String CALCULATION  = "/calculator/calc";

    @PostMapping(OFFERS)
    public List<LoanOfferDto> calculateLoanOffers(@RequestBody LoanStatementRequestDto requestDto){
        log.info("Received request to calculate loan offers: {}", requestDto);

        try {
            List<LoanOfferDto> offers = loanOfferService.getOffers(requestDto);
            log.info("Successfully calculated loan offers: {}", offers);

            return offers;
        } catch (Exception e) {
            log.error("Error occurred while calculating loan offers for request: {}", requestDto, e);
            throw e;
        }
    }

    @PostMapping(CALCULATION)
    public CreditDto calculateCredit(@RequestBody ScoringDataDto scoringDataDto){
        log.info("Received request to calculate credit: {}", scoringDataDto);

        try {
            CreditDto credit = calculateCreditService.calculateCredit(scoringDataDto);
            log.info("Successfully calculated credit: {}", credit);
            return credit;

        } catch (Exception e) {
            log.error("Error occurred while calculating credit for request: {}", scoringDataDto, e);
            throw e;
        }
    }
}
