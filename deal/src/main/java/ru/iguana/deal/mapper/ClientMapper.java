package ru.iguana.deal.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.iguana.deal.dto.ClientDto;
import ru.iguana.deal.entity.Client;
import ru.iguana.deal.entity.Jsonb.Passport;

import java.time.LocalDate;
@Component
@AllArgsConstructor
public class ClientMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClientDto jsonToClientDto(String jsonRequest) throws JsonProcessingException {
        JsonNode node = objectMapper.readTree(jsonRequest);

        ClientDto clientDto = new ClientDto();
        clientDto
                .setFirstName(node.path("firstName").asText())

                .setLastName(node.path("lastName").asText())

                .setMiddleName(node.path("middleName").asText())

                .setEmail(node.path("email").asText())

                .setBirthDate(LocalDate.parse(node.path("birthdate").asText()))

                .setPassport(new Passport()
                                .setSeries(node.path("passportSeries").asText())
                                .setNumber(node.path("passportNumber").asText())
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
