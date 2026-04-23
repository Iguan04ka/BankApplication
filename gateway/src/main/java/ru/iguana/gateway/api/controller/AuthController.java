package ru.iguana.gateway.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.gateway.api.dto.LoginRequestDto;
import ru.iguana.gateway.api.dto.RefreshRequestDto;
import ru.iguana.gateway.api.dto.UserResponseDto;
import ru.iguana.gateway.api.service.JwtService;
import ru.iguana.gateway.api.service.RefreshTokenStore;
import ru.iguana.gateway.api.service.RequestToIntegrationRolesService;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final RequestToIntegrationRolesService rolesService;

    private final RefreshTokenStore refreshTokenStore;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {

        if (request.getSub() == null || request.getSub().isBlank()) {
            return ResponseEntity.badRequest().body("Sub is required");
        }

        UserResponseDto userDto;


        try {
            userDto = rolesService.getUserBySub(request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Failed to retrieve user: " + e.getMessage());
        }

        var sub = userDto.getUserKey().getSub();

        if (userDto == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User not found");
        }

        if (userDto.isBlocked()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("User is blocked");
        }

        if (userDto.getRoles() == null || userDto.getRoles().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User has no roles");
        }

        String accessToken = jwtService.generateAccessToken(sub);
        String refreshToken = jwtService.generateRefreshToken(sub);

        refreshTokenStore.save(sub, refreshToken);

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        ));
    }
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequestDto request) {

        if (request.getRefreshToken() == null) {
            return ResponseEntity.badRequest().body("Refresh token required");
        }

        String sub;
        try {
            sub = jwtService.extractSub(request.getRefreshToken());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid refresh token");
        }

        if (!refreshTokenStore.isValid(sub, request.getRefreshToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Refresh token not recognized");
        }

        String newAccessToken = jwtService.generateAccessToken(sub);

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken
        ));
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshRequestDto request) {

        String sub = jwtService.extractSub(request.getRefreshToken());
        refreshTokenStore.delete(sub);

        return ResponseEntity.ok("Logged out");
    }
}

