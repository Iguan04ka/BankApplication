package ru.iguana.integrationroles.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.iguana.integrationroles.api.dto.UserResponseDto;
import ru.iguana.integrationroles.api.dto.UserWithRolesDto;
import ru.iguana.integrationroles.api.service.IntegrationRolesService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class IntegrationRolesController {
    private final IntegrationRolesService integrationRolesService;

    @PostMapping("/roles/usersRoles")
    public ResponseEntity<Map<Long, UserResponseDto>> usersRoles(@RequestBody List<Long> ids){
        var response = integrationRolesService.getUsersWithRolesByIds(ids);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/roles/usersByRole")
    public ResponseEntity<List<String>> getUsersByRole(@RequestParam String roleName) {
        return ResponseEntity.ok(integrationRolesService.getUserLoginsByRole(roleName));
    }
}
