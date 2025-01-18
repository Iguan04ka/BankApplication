package ru.iguana.deal.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.deal.api.dto.StatementDto;
import ru.iguana.deal.api.service.AdminService;


import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/deal/admin/statement/{statementId}")
    public StatementDto getStatement(@PathVariable String statementId){
        return adminService.getStatement(statementId);
    }

    @GetMapping("/deal/admin/statement")
    public List<StatementDto> getStatement(){
        return adminService.getAllStatements();
    }
}
