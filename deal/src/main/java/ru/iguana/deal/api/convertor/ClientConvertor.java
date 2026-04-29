package ru.iguana.deal.api.convertor;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.iguana.deal.api.dto.ClientDto;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Jsonb.Employment;
import ru.iguana.deal.model.entity.Jsonb.Passport;

import java.math.BigDecimal;
import java.time.LocalDate;
@Component
@Slf4j
public class ClientConvertor {

    public ClientDto jsonToClientDto(JsonNode jsonRequest){

        ClientDto clientDto = new ClientDto();
        clientDto
                .setFirstName(jsonRequest.path("firstName").asText())

                .setLastName(jsonRequest.path("lastName").asText())

                .setMiddleName(jsonRequest.path("middleName").asText())

                .setUserSub(jsonRequest.path("userSub").asText())

                .setEmail(jsonRequest.path("email").asText())

                .setBirthDate(LocalDate.parse(jsonRequest.path("birthdate").asText()))

                .setPassport(new Passport()
                                .setSeries(jsonRequest.path("passportSeries").asText())
                                .setNumber(jsonRequest.path("passportNumber").asText())
                );
        log.info("из джсона в дто: {}", clientDto);
        return clientDto;
    }

    public Client clientDtoToClientEntity(ClientDto clientDto){
        Client entity = new Client();
        entity
                .setFirstName(clientDto.getFirstName())

                .setLastName(clientDto.getLastName())

                .setMiddleName(clientDto.getMiddleName())

                .setUserSub(clientDto.getUserSub())

                .setEmail(clientDto.getEmail())

                .setBirthDate(clientDto.getBirthDate())

                .setPassport(clientDto.getPassport());
        log.info("из дто в ентити: {}", entity);
        return entity;

    }
    public Employment employmentJsonToDto(JsonNode json){
        Employment employment = new Employment();
        employment.setStatus(json.path("employmentStatus").asText());

        employment.setEmployer_inn(json.path("employerINN").asText());

        BigDecimal salary = new BigDecimal(json.path("salary").asText());
        employment.setSalary(salary);

        employment.setPosition(json.path("position").asText());

        employment.setWorkExperienceCurrent(json.path("salary").asInt());

        employment.setWorkExperienceTotal(json.path("salary").asInt());

        return employment;

    }

}
