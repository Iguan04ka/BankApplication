package ru.iguana.calculator.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.iguana.calculator.api.dto.LoanOfferDto;
import ru.iguana.calculator.api.dto.LoanStatementRequestDto;
import ru.iguana.calculator.api.dto.OfferParameters;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class LoanOfferService {

    private final CalculateCreditService calculateCreditService;

    public List<LoanOfferDto> getOffers(LoanStatementRequestDto requestDto) {
        log.info("Received request for loan offers: {}", requestDto);
        try {
            List<OfferParameters> parameters = List.of(
                    new OfferParameters(false, false),
                    new OfferParameters(false, true),
                    new OfferParameters(true, false),
                    new OfferParameters(true, true)
            );

            List<LoanOfferDto> offers = parameters
                                .stream()
                                .map(params -> calculateOffer(params.getIsInsuranceEnabled(),
                                                              params.getIsSalaryClient(),
                                                              requestDto))
                                .toList();
            log.info("Generated loan offers: {}", offers);

            return offers;

        } catch (Exception e) {
            log.error("Error occurred while generating loan offers for request: {}", requestDto, e);
            throw e;
        }
    }

    private LoanOfferDto calculateOffer(Boolean isInsuranceEnabled,
                                        Boolean isSalaryClient,
                                        LoanStatementRequestDto requestDto) {

        log.debug("Calculating loan offer with isInsuranceEnabled={}, isSalaryClient={}, requestDto={}",
                isInsuranceEnabled, isSalaryClient, requestDto);

        LoanOfferDto offer = new LoanOfferDto();

        try {
            offer.setStatementId(UUID.randomUUID());
            offer.setTerm(requestDto.getTerm());

            BigDecimal finalRate = calculateCreditService.calculateFinalRate(isSalaryClient, isInsuranceEnabled);
            offer.setRate(finalRate);

            log.debug("Calculated final rate: {}", finalRate);

            BigDecimal insurance = new BigDecimal("0");

            if (isInsuranceEnabled) {
                insurance = insurance.add(calculateCreditService.calculateInsurance(
                        requestDto.getAmount(), requestDto.getTerm()));
                log.debug("Calculated insurance: {}", insurance);
            }

            BigDecimal requestedAmountWithInsurance = requestDto.getAmount().add(insurance);
            offer.setRequestedAmount(requestedAmountWithInsurance);

            offer.setMonthlyPayment(calculateCreditService.calculateMonthlyPaymentAnnuity(
                    requestedAmountWithInsurance,
                    requestDto.getTerm(),
                    finalRate));
            offer.setTotalAmount(offer.getMonthlyPayment().multiply(BigDecimal.valueOf(requestDto.getTerm())));

            offer.setIsSalaryClient(isSalaryClient);
            offer.setIsInsuranceEnabled(isInsuranceEnabled);

            log.info("Generated loan offer: {}", offer);
            return offer;

        } catch (Exception e) {
            log.error("Error occurred while calculating loan offer with isInsuranceEnabled={}, isSalaryClient={}, requestDto={}",
                    isInsuranceEnabled, isSalaryClient, requestDto, e);
            throw e;
        }
    }
}
