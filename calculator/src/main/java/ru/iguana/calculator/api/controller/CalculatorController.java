package ru.iguana.calculator.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.calculator.api.dto.*;
import ru.iguana.calculator.api.service.CalculateCreditService;
import ru.iguana.calculator.api.service.LoanOfferService;

import java.util.List;

@Tag(name = "Main methods")
@Slf4j
@RestController
public class CalculatorController {
    @Autowired
    private LoanOfferService loanOfferService;

    @Autowired
    private CalculateCreditService calculateCreditService;

    public static final String OFFERS = "/calculator/offers";

    public static final String CALCULATION  = "/calculator/calc";

    @Operation(
            summary = "Generates a list of 4 loan offers",
            description = "Receives LoanStatementRequestDto as input and returns a list of 4 loan offers"
    )
    @PostMapping(OFFERS)
    public List<LoanOfferDto> calculateLoanOffers(@Validated @RequestBody LoanStatementRequestDto requestDto){
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

    @Operation(
            summary = "Calculates loan parameters",
            description = "Receives ScoringDataDto as input and, based on this data, calculates the loan parameters"
    )
    @PostMapping(CALCULATION)
    public CreditDto calculateCredit(@Validated @RequestBody ScoringDataDto scoringDataDto){
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
