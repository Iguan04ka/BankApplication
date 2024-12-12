package ru.iguana.deal.api.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.iguana.deal.api.dto.ClientDto;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Jsonb.Passport;

import java.time.LocalDate;
@Component
public class ClientMapper {

    public ClientDto jsonToClientDto(JsonNode jsonRequest){

        ClientDto clientDto = new ClientDto();
        clientDto
                .setFirstName(jsonRequest.path("firstName").asText())

                .setLastName(jsonRequest.path("lastName").asText())

                .setMiddleName(jsonRequest.path("middleName").asText())

                .setEmail(jsonRequest.path("email").asText())

                .setBirthDate(LocalDate.parse(jsonRequest.path("birthdate").asText()))

                .setPassport(new Passport()
                                .setSeries(jsonRequest.path("passportSeries").asText())
                                .setNumber(jsonRequest.path("passportNumber").asText())
                );

        return clientDto;
    }

    public Client clientDtoToClientEntity(ClientDto clientDto){
        Client entity = new Client();
        entity
                .setFirstName(clientDto.getFirstName())

                .setLastName(clientDto.getLastName())

                .setMiddleName(clientDto.getMiddleName())

                .setEmail(clientDto.getEmail())

                .setBirthDate(clientDto.getBirthDate())

                .setPassport(clientDto.getPassport());

        return entity;

    }

}
