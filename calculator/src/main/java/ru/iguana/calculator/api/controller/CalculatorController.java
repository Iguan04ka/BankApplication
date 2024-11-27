package ru.iguana.calculator.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.calculator.api.dto.*;
import ru.iguana.calculator.api.service.CalculateCreditService;
import ru.iguana.calculator.api.service.LoanOfferService;

import java.util.List;

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
        return loanOfferService.getOffers(requestDto);
    }

    @PostMapping(CALCULATION)
    public CreditDto calculateCredit(@RequestBody ScoringDataDto scoringDataDto){
        return calculateCreditService.calculateCredit(scoringDataDto);
    }


}
