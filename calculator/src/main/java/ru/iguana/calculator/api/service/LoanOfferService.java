package ru.iguana.calculator.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.iguana.calculator.api.config.LoanProperties;
import ru.iguana.calculator.api.dto.LoanOfferDto;
import ru.iguana.calculator.api.dto.LoanStatementRequestDto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class LoanOfferService {
    private final LoanProperties loanProperties;

    private final CalculateCreditService calculateCreditService;

    public List<LoanOfferDto> getOffers(LoanStatementRequestDto requestDto){
        return List.of(
                calculateOffer(false, false, requestDto),
                calculateOffer(false, true, requestDto),
                calculateOffer(true, false, requestDto),
                calculateOffer(true, true, requestDto)
        );
    }

    private LoanOfferDto calculateOffer(Boolean isInsuranceEnabled,
                           Boolean isSalaryClient,
                           LoanStatementRequestDto requestDto){

        BigDecimal currentRate = loanProperties.getBaseRate();

        LoanOfferDto offer = new LoanOfferDto();

        offer.setStatementId(UUID.randomUUID());
        offer.setRequestedAmount(requestDto.getAmount());
        offer.setTerm(requestDto.getTerm());

        BigDecimal insurance = new BigDecimal("0.00");

        if (isSalaryClient){
            currentRate = currentRate.subtract(new BigDecimal("1.00"));
        }
        if (isInsuranceEnabled){
            currentRate = currentRate.subtract(new BigDecimal("3.00"));

            insurance = insurance.add(calculateCreditService.calculateInsurance(requestDto.getAmount(),
                                                                    requestDto.getTerm()));
        }

        offer.setRate(currentRate);
        offer.setMonthlyPayment(calculateCreditService.calculateMonthlyPaymentAnnuity(requestDto.getAmount(),
                                                        requestDto.getTerm(),
                                                        currentRate));
        offer.setTotalAmount(insurance.add(offer.getMonthlyPayment()
                                  .multiply(BigDecimal.valueOf(requestDto.getTerm()))));
        offer.setIsSalaryClient(isSalaryClient);
        offer.setIsInsuranceEnabled(isInsuranceEnabled);

        return offer;
    }


}
