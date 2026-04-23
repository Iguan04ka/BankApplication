package ru.iguana.integrationroles.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.iguana.integrationroles.api.dto.LoginRequestDto;
import ru.iguana.integrationroles.api.dto.UserResponseDto;
import ru.iguana.integrationroles.api.service.IntegrationRolesService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class IntegrationRolesController {

    private final IntegrationRolesService integrationRolesService;

    @PostMapping("/roles/usersRoles")
    public ResponseEntity<Map<String, UserResponseDto>> usersRoles(@RequestBody List<Long> ids) {
        log.info("POST /roles/usersRoles called with ids: {}", ids);
        var response = integrationRolesService.getUsersWithRolesByIds(ids);
        log.info("Response for /roles/usersRoles: {}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/roles/usersByRole")
    public ResponseEntity<List<String>> getUsersByRole(@RequestParam String roleName) {
        log.info("GET /roles/usersByRole called with roleName: {}", roleName);
        var result = integrationRolesService.getUserLoginsByRole(roleName);
        log.info("Response for /roles/usersByRole: {}", result);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/roles/userBySub")
    public ResponseEntity<UserResponseDto> getUserBySub (@RequestBody LoginRequestDto loginRequestDto){
        var result = integrationRolesService.getUserResponseDtoBySub(loginRequestDto.getSub());
        return ResponseEntity.ok(result);
    }
}
