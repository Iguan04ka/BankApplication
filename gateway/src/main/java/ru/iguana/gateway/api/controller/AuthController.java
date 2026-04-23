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
import ru.iguana.gateway.api.dto.RegisterRequestDto;
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

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDto request) {

        if (request.getSub() == null || request.getSub().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Sub and password are required");
        }

        try {
            UserResponseDto userDto = rolesService.createUser(request);

            if (userDto == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("User creation failed");
            }

            return ResponseEntity.ok("User successfully registered");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {

        if (request.getSub() == null || request.getSub().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Sub and password are required");
        }

        UserResponseDto userDto;

        try {
            userDto = rolesService.authenticate(request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid credentials");
        }

        if (userDto == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid credentials");
        }

        if (userDto.isBlocked()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("User is blocked");
        }

        String sub = userDto.getUserKey().getSub();

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

