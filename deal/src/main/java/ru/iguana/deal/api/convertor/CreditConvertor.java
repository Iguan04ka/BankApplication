package ru.iguana.deal.api.convertor;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.iguana.deal.api.dto.CreditDto;
import ru.iguana.deal.model.entity.Credit;

import java.math.BigDecimal;

@Component
@AllArgsConstructor
public class CreditConvertor {

    public CreditDto jsonToCreditDto(JsonNode jsonNode){
        CreditDto creditDto = new CreditDto()
                .setAmount(new BigDecimal(jsonNode.path("amount").asText()))

                .setTerm(Integer.valueOf(jsonNode.path("term").asText()))

                .setMonthlyPayment(new BigDecimal(jsonNode.path("monthlyPayment").asText()))

                .setRate(new BigDecimal(jsonNode.path("rate").asText()))

                .setPsk(new BigDecimal(jsonNode.path("psk").asText()))

                .setIsInsuranceEnabled(jsonNode.path("isInsuranceEnabled").asBoolean())

                .setIsSalaryClient(jsonNode.path("isSalaryClient").asBoolean())

                .setPaymentSchedule(jsonNode.get("paymentSchedule"));

        return creditDto;
    }

    public Credit CreditDtoToCreditEntity(CreditDto creditDto){
        Credit entity = new Credit();

        entity
                .setAmount(creditDto.getAmount())

                .setTerm(creditDto.getTerm())

                .setMonthlyPayment(creditDto.getMonthlyPayment())

                .setRate(creditDto.getRate())

                .setPsk(creditDto.getPsk())

                .setIsInsuranceEnabled(creditDto.getIsInsuranceEnabled())

                .setIsSalaryClient(creditDto.getIsSalaryClient())

                .setPaymentSchedule(creditDto.getPaymentSchedule())

                .setCreditStatus(creditDto.getCreditStatus());
        return entity;

    }
}

















