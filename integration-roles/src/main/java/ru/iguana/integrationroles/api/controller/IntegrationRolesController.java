package ru.iguana.integrationroles.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.iguana.integrationroles.api.dto.ChangeEmailRequestDto;
import ru.iguana.integrationroles.api.dto.ChangePasswordRequestDto;
import ru.iguana.integrationroles.api.dto.ChangeSubRequestDto;
import ru.iguana.integrationroles.api.dto.ForgotPasswordRequestDto;
import ru.iguana.integrationroles.api.dto.LoginRequestDto;
import ru.iguana.integrationroles.api.dto.RegisterRequestDto;
import ru.iguana.integrationroles.api.dto.ResetPasswordRequestDto;
import ru.iguana.integrationroles.api.dto.SubRequestDto;
import ru.iguana.integrationroles.api.dto.UserResponseDto;
import ru.iguana.integrationroles.api.service.AccountService;
import ru.iguana.integrationroles.api.service.IntegrationRolesService;
import ru.iguana.integrationroles.api.service.PasswordResetService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class IntegrationRolesController {

    private final IntegrationRolesService integrationRolesService;
    private final PasswordResetService passwordResetService;
    private final AccountService accountService;

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
    public ResponseEntity<UserResponseDto> getUserBySub (@RequestBody SubRequestDto subRequestDto){
        var result = integrationRolesService.getUserResponseDtoBySub(subRequestDto.getSub());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/roles/createUser")
    public ResponseEntity<UserResponseDto> createUser(@RequestBody RegisterRequestDto request) {
        var result = integrationRolesService.createUser(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/roles/authenticate")
    public ResponseEntity<UserResponseDto> authenticate(@RequestBody LoginRequestDto request) {
        var result = integrationRolesService.authenticate(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/roles/forgotPassword")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequestDto request) {
        log.info("POST /roles/forgotPassword");
        passwordResetService.requestPasswordReset(request.getEmail());
        // Always 200 — never reveal whether the email is registered
        return ResponseEntity.ok().build();
    }

    @GetMapping("/roles/verifyResetToken")
    public ResponseEntity<Map<String, Boolean>> verifyResetToken(@RequestParam String token) {
        boolean valid = passwordResetService.verifyToken(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @PostMapping("/roles/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDto request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Password reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/roles/account/changeSub")
    public ResponseEntity<?> changeSub(@RequestBody ChangeSubRequestDto request) {
        try {
            String newSub = accountService.changeSub(
                    request.getCurrentSub(),
                    request.getNewSub(),
                    request.getCurrentPassword());
            return ResponseEntity.ok(Map.of("sub", newSub));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/roles/account/changeEmail")
    public ResponseEntity<?> changeEmail(@RequestBody ChangeEmailRequestDto request) {
        try {
            String email = accountService.changeEmail(
                    request.getCurrentSub(),
                    request.getNewEmail(),
                    request.getCurrentPassword());
            return ResponseEntity.ok(Map.of("email", email));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/roles/account/changePassword")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequestDto request) {
        try {
            accountService.changePassword(
                    request.getCurrentSub(),
                    request.getCurrentPassword(),
                    request.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
