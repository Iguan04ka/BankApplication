package ru.iguana.calculator.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.calculator.api.config.LoanProperties;
import ru.iguana.calculator.api.dto.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class CalculatorController {
    @Autowired
    private LoanProperties loanProperties;

    public static final String OFFERS = "/calculator/offers";

    public static final String CALCULATION  = "/calculator/calc";

    @PostMapping(OFFERS)
    public List<LoanOfferDto> calculateLoanOffers(@RequestBody LoanStatementRequestDto requestDto){
        List<LoanOfferDto> offers = new ArrayList<>();
        BigDecimal baseRate = loanProperties.getBaseRate();

        for (boolean insurance : new boolean[]{false, true}) {
            for (boolean salaryClient : new boolean[]{false, true}) {

                LoanOfferDto offer = new LoanOfferDto();

                offer.setStatementId(UUID.randomUUID());
                offer.setRequestedAmount(requestDto.getAmount());
                offer.setTerm(requestDto.getTerm());

                BigDecimal currentRate = baseRate;

                if(salaryClient){
                    currentRate = currentRate.subtract(new BigDecimal("1.00"));
                }
                if (insurance){
                    currentRate = currentRate.subtract(new BigDecimal("3.00"));
                }
                offer.setRate(currentRate);

                offer.setMonthlyPayment(requestDto.getAmount()
                                        .multiply(getAnnuityCoefficient(requestDto.getTerm(), currentRate)));

                offer.setTotalAmount(offer.getMonthlyPayment()
                                    .multiply(BigDecimal.valueOf(requestDto.getTerm())));


                offer.setIsInsuranceEnabled(insurance);
                offer.setIsSalaryClient(salaryClient);

                offers.add(offer);
            }
        }
        return offers;
    }

    @PostMapping(CALCULATION)
    public CreditDto calculateCredit(@RequestBody ScoringDataDto scoringDataDto){
        //TODO implementation
        return null;
    }

    private static BigDecimal getAnnuityCoefficient(Integer term, BigDecimal rate) {
        int scale = 10;

        /*
       Формула: (monthlyRate * (1 + monthlyRate)^term )) / (1 + monthlyRate)^term - 1
        */

        //рассчет месячной процентной ставки: годовая ставка / 12
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12), scale, RoundingMode.HALF_UP);

        //преобразую процентную ставку в десятичный формат
        monthlyRate = monthlyRate.divide(BigDecimal.valueOf(100), scale, RoundingMode.HALF_UP);

        //(1 + monthlyRate) ^ term
        BigDecimal onePlusRatePowTerm = BigDecimal.ONE.add(monthlyRate).pow(term, new MathContext(scale, RoundingMode.HALF_UP));

        //monthlyRate * (1 + monthlyRate) ^ term
        BigDecimal numerator = monthlyRate.multiply(onePlusRatePowTerm);

        //(1 + monthlyRate) ^ term - 1
        BigDecimal denominator = onePlusRatePowTerm.subtract(BigDecimal.ONE);

        //numerator / denominator
        BigDecimal annuityCoefficient = numerator.divide(denominator, scale, RoundingMode.HALF_UP);

        //Округляем результат до 6 знаков после запятой
        return annuityCoefficient.setScale(6, RoundingMode.HALF_UP);
    }
}
