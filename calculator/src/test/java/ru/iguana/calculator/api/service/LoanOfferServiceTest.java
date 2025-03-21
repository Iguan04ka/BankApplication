package ru.iguana.calculator.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.iguana.calculator.api.dto.LoanOfferDto;
import ru.iguana.calculator.api.dto.LoanStatementRequestDto;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoanOfferServiceTest {

    @Mock
    private CalculateCreditService calculateCreditService;

    @InjectMocks
    private LoanOfferService loanOfferService;

    @Test
    void testGetOffersReturnsFourOffers(){
        LoanStatementRequestDto requestDto = new LoanStatementRequestDto();
        requestDto.setAmount(new BigDecimal("300000"));
        requestDto.setTerm(12);

        when(calculateCreditService.calculateFinalRate(anyBoolean(), anyBoolean()))
                .thenReturn(new BigDecimal("5.5"));

        when(calculateCreditService.calculateInsurance(any(), anyInt()))
                .thenReturn(new BigDecimal("10000"));

        when(calculateCreditService.calculateMonthlyPaymentAnnuity(any(), anyInt(), any()))
                .thenReturn(new BigDecimal("50000"));

        List<LoanOfferDto> offers = loanOfferService.getOffers(requestDto);

        assertEquals(4, offers.size(), "The method should return 4 loan offers.");
    }

    @Test
    void testGetOffersCorrectFieldValues() {
        LoanStatementRequestDto requestDto = new LoanStatementRequestDto();
        requestDto.setAmount(new BigDecimal("300000"));
        requestDto.setTerm(12);

        when(calculateCreditService.calculateFinalRate(Boolean.TRUE, Boolean.TRUE))
                .thenReturn(new BigDecimal("17"));

        when(calculateCreditService.calculateInsurance(any(), anyInt()))
                .thenReturn(new BigDecimal("10000"));

        when(calculateCreditService.calculateMonthlyPaymentAnnuity(any(), anyInt(), any()))
                .thenReturn(new BigDecimal("42000"));

        List<LoanOfferDto> offers = loanOfferService.getOffers(requestDto);

        LoanOfferDto firstOffer = offers.get(2);



        assertNotNull(firstOffer.getStatementId(), "StatementId should not be null.");

        assertEquals(new BigDecimal("310000"), firstOffer.getRequestedAmount(),
                "Requested amount should include insurance.");

        assertEquals(new BigDecimal("42000"), firstOffer.getMonthlyPayment(),
                "Monthly payment calculation is incorrect.");

        assertEquals(new BigDecimal("504000"), firstOffer.getTotalAmount(),
                "Total amount should be calculated as monthlyPayment * term.");
    }

    @Test
    void testGetOffersWithLargeValues() {
        LoanStatementRequestDto requestDto = new LoanStatementRequestDto();
        requestDto.setAmount(new BigDecimal("1000000000"));
        requestDto.setTerm(360);

        when(calculateCreditService.calculateFinalRate(anyBoolean(), anyBoolean()))
                .thenReturn(new BigDecimal("30"));
        when(calculateCreditService.calculateInsurance(any(), anyInt()))
                .thenReturn(new BigDecimal("20000000"));
        when(calculateCreditService.calculateMonthlyPaymentAnnuity(any(), anyInt(), any()))
                .thenReturn(new BigDecimal("10000000"));

        List<LoanOfferDto> offers = loanOfferService.getOffers(requestDto);

        assertEquals(4, offers.size(), "The method should return 4 loan offers.");
        for (LoanOfferDto offer : offers) {

            if (offer.getIsInsuranceEnabled()) {
                assertTrue(offer.getRequestedAmount().compareTo(requestDto.getAmount()) > 0,
                        "Requested amount should include insurance if enabled.");
            }
            assertEquals(offer.getTotalAmount(),
                    offer.getMonthlyPayment().multiply(BigDecimal.valueOf(requestDto.getTerm())),
                    "Total amount should be correctly calculated based on monthly payment.");
        }
    }
}