package ru.iguana.gateway.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;
import ru.iguana.gateway.api.dto.AccountSettingsChangeEmailRequestDto;
import ru.iguana.gateway.api.dto.AccountSettingsChangePasswordRequestDto;
import ru.iguana.gateway.api.dto.AccountSettingsChangeSubRequestDto;
import ru.iguana.gateway.api.dto.ChangeEmailRequestDto;
import ru.iguana.gateway.api.dto.ChangePasswordRequestDto;
import ru.iguana.gateway.api.dto.ChangeSubRequestDto;
import ru.iguana.gateway.api.dto.TwoFactorCodeRequestDto;
import ru.iguana.gateway.api.dto.TwoFactorVerifyRequestDto;
import ru.iguana.gateway.api.service.JwtService;
import ru.iguana.gateway.api.service.RefreshTokenStore;
import ru.iguana.gateway.api.service.RequestToIntegrationRolesService;

import java.util.Map;

@RestController
@RequestMapping("/account/settings")
@RequiredArgsConstructor
@Slf4j
public class AccountSettingsController {

    private final RequestToIntegrationRolesService rolesService;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;

    @PreAuthorize("hasAuthority('base_user')")
    @PostMapping("/change-sub")
    public ResponseEntity<?> changeSub(@RequestBody AccountSettingsChangeSubRequestDto request) {
        String currentSub = currentSub();
        log.info("Account settings: change sub for {}", currentSub);

        try {
            Map<String, String> result = rolesService.changeAccountSub(
                    new ChangeSubRequestDto(currentSub, request.getNewSub(), request.getCurrentPassword()));
            String newSub = result != null ? result.get("sub") : null;
            if (newSub == null || newSub.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Empty response from roles service"));
            }

            // Re-issue JWT with the new sub and rotate the refresh token
            refreshTokenStore.delete(currentSub);
            String newAccess = jwtService.generateAccessToken(newSub);
            String newRefresh = jwtService.generateRefreshToken(newSub);
            refreshTokenStore.save(newSub, newRefresh);

            return ResponseEntity.ok(Map.of(
                    "sub", newSub,
                    "accessToken", newAccess,
                    "refreshToken", newRefresh
            ));
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(parseError(e));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('base_user')")
    @PostMapping("/change-email")
    public ResponseEntity<?> changeEmail(@RequestBody AccountSettingsChangeEmailRequestDto request) {
        String currentSub = currentSub();
        log.info("Account settings: change email for {}", currentSub);
        try {
            Map<String, String> result = rolesService.changeAccountEmail(
                    new ChangeEmailRequestDto(currentSub, request.getNewEmail(), request.getCurrentPassword()));
            return ResponseEntity.ok(result);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(parseError(e));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('base_user')")
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody AccountSettingsChangePasswordRequestDto request) {
        String currentSub = currentSub();
        log.info("Account settings: change password for {}", currentSub);
        try {
            rolesService.changeAccountPassword(
                    new ChangePasswordRequestDto(currentSub, request.getCurrentPassword(), request.getNewPassword()));
            return ResponseEntity.ok().build();
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(parseError(e));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('base_user')")
    @GetMapping("/2fa/status")
    public ResponseEntity<?> twoFactorStatus() {
        String sub = currentSub();
        try {
            Map<String, Object> status = rolesService.getTwoFactorStatus(sub);
            return ResponseEntity.ok(status);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(parseError(e));
        }
    }

    @PreAuthorize("hasAuthority('base_user')")
    @PostMapping("/2fa/request-enable")
    public ResponseEntity<?> requestEnable2FA() {
        String sub = currentSub();
        try {
            rolesService.issueTwoFactorEnableCode(sub);
            return ResponseEntity.ok().build();
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(parseError(e));
        }
    }

    @PreAuthorize("hasAuthority('base_user')")
    @PostMapping("/2fa/confirm-enable")
    public ResponseEntity<?> confirmEnable2FA(@RequestBody TwoFactorCodeRequestDto request) {
        String sub = currentSub();
        try {
            rolesService.confirmTwoFactorEnable(new TwoFactorVerifyRequestDto(sub, request.getCode()));
            return ResponseEntity.ok().build();
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(parseError(e));
        }
    }

    @PreAuthorize("hasAuthority('base_user')")
    @PostMapping("/2fa/request-disable")
    public ResponseEntity<?> requestDisable2FA() {
        String sub = currentSub();
        try {
            rolesService.issueTwoFactorDisableCode(sub);
            return ResponseEntity.ok().build();
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(parseError(e));
        }
    }

    @PreAuthorize("hasAuthority('base_user')")
    @PostMapping("/2fa/confirm-disable")
    public ResponseEntity<?> confirmDisable2FA(@RequestBody TwoFactorCodeRequestDto request) {
        String sub = currentSub();
        try {
            rolesService.confirmTwoFactorDisable(new TwoFactorVerifyRequestDto(sub, request.getCode()));
            return ResponseEntity.ok().build();
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(parseError(e));
        }
    }

    private String currentSub() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Object parseError(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body == null || body.isBlank()) return Map.of("error", e.getStatusText());
        return body;
    }
}
