package ru.iguana.calculator.api.service;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.iguana.calculator.api.config.LoanProperties;
import ru.iguana.calculator.api.dto.CreditDto;
import ru.iguana.calculator.api.dto.EmploymentDto;
import ru.iguana.calculator.api.dto.ScoringDataDto;
import ru.iguana.calculator.api.enums.EmploymentStatus;
import ru.iguana.calculator.api.enums.Gender;
import ru.iguana.calculator.api.enums.MaritalStatus;
import ru.iguana.calculator.api.enums.Positions;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@MockitoSettings(strictness = Strictness.LENIENT)
class CalculateCreditServiceTest {
    @Mock
    private LoanProperties loanProperties;

    @InjectMocks
    private CalculateCreditService calculateCreditService;

    @Test
    void calculateCredit() {
        ScoringDataDto scoringData = mock(ScoringDataDto.class);
        EmploymentDto employmentDto = mock(EmploymentDto.class);

        when(loanProperties.getBaseCostOfInsurance()).thenReturn(new BigDecimal("30000"));
        when(loanProperties.getBaseRate()).thenReturn(new BigDecimal("21"));

        when(scoringData.getAmount()).thenReturn(new BigDecimal("300000"));
        when(scoringData.getTerm()).thenReturn(12);
        when(scoringData.getIsInsuranceEnabled()).thenReturn(true);
        when(scoringData.getIsSalaryClient()).thenReturn(true);
        when(scoringData.getBirthdate()).thenReturn(LocalDate.of(2000, 1, 1));
        when(scoringData.getMaritalStatus()).thenReturn(MaritalStatus.MARRIED);
        when(scoringData.getGender()).thenReturn(Gender.MALE);

        when(scoringData.getEmployment()).thenReturn(employmentDto);
        when(employmentDto.getSalary()).thenReturn(new BigDecimal("100000"));
        when(employmentDto.getWorkExperienceTotal()).thenReturn(20);
        when(employmentDto.getWorkExperienceCurrent()).thenReturn(10);
        when(employmentDto.getEmploymentStatus()).thenReturn(EmploymentStatus.SELFEMPLOYED);
        when(employmentDto.getPosition()).thenReturn(Positions.MIDDLE);


        CreditDto result = calculateCreditService.calculateCredit(scoringData);

        assertNotNull(result);
        assertTrue(result.getAmount().compareTo(new BigDecimal("300000")) > 0,
                "The amount should include insurance.");

        assertNotNull(result.getPaymentSchedule(), "Payment schedule should not be null.");
        assertEquals(result.getPaymentSchedule().size(), scoringData.getTerm(), "The amount of funds corresponds to the loan term");

        assertNotNull(result.getRate());
        assertNotNull(result.getPsk());
        assertNotNull(result.getMonthlyPayment());

    }

    @Test
    void calculateFinalRate() {
        when(loanProperties.getBaseRate()).thenReturn(new BigDecimal("10"));

        BigDecimal finalRate = calculateCreditService.calculateFinalRate(true, true);

        assertEquals(new BigDecimal("6"), finalRate,
                "Final rate should be reduced by 4% (1% for salary client and 3% for insurance).");
    }

    @Test
    void calculateMonthlyPaymentAnnuity() {
        BigDecimal amount = new BigDecimal("100000");
        Integer term = 12;
        BigDecimal rate = new BigDecimal("21");

        BigDecimal monthlyPayment = calculateCreditService.calculateMonthlyPaymentAnnuity(amount, term, rate);

        assertEquals(new BigDecimal("9311.38"), monthlyPayment);
    }

    @Test
    void calculateInsurance() {
        BigDecimal amount = new BigDecimal("100000");
        Integer term = 12;
        when(loanProperties.getBaseCostOfInsurance()).thenReturn(new BigDecimal("30000"));

        BigDecimal insurance = calculateCreditService.calculateInsurance(amount, term);

        assertEquals(new BigDecimal("31200"), insurance);
    }
}