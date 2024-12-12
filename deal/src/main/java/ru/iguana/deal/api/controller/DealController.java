package ru.iguana.deal.api.controller;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.deal.api.service.StatementService;


import java.util.List;


@RestController
@AllArgsConstructor
public class DealController {
    private final StatementService statementService;

    @PostMapping("/deal/statement")
    public ResponseEntity<List<JsonNode>> mapClient(@RequestBody JsonNode json) {
        return statementService.getLoanOfferList(json);
    }
}
