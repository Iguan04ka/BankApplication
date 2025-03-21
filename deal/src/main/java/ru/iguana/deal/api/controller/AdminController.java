package ru.iguana.deal.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.deal.api.dto.StatementDto;
import ru.iguana.deal.api.service.AdminService;


import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/deal/admin/statement/{statementId}")
    public StatementDto getStatement(@PathVariable String statementId) {
        log.info("Received request to fetch statement with ID: {}", statementId);
        StatementDto statementDto = adminService.getStatement(statementId);
        log.info("Returning statement: {}", statementDto);
        return statementDto;
    }

    @GetMapping("/deal/admin/statement")
    public List<StatementDto> getAllStatements() {
        log.info("Received request to fetch all statements");
        List<StatementDto> statementDtos = adminService.getAllStatements();
        log.info("Returning {} statements", statementDtos.size());
        return statementDtos;
    }
}
