package ru.iguana.gateway.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.gateway.api.dto.ForgotPasswordRequestDto;
import ru.iguana.gateway.api.dto.LoginRequestDto;
import ru.iguana.gateway.api.dto.RefreshRequestDto;
import ru.iguana.gateway.api.dto.RegisterRequestDto;
import ru.iguana.gateway.api.dto.ResetPasswordRequestDto;
import ru.iguana.gateway.api.dto.UserResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body("Email is required");
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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequestDto request) {
        // Always return 200 — never reveal whether the email is registered
        try {
            rolesService.requestPasswordReset(request);
        } catch (Exception e) {
            // Swallow all errors so we don't leak info about email existence
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reset-password/{token}/valid")
    public ResponseEntity<?> verifyResetToken(@PathVariable String token) {
        boolean valid = false;
        try {
            valid = rolesService.verifyResetToken(token);
        } catch (Exception e) {
            valid = false;
        }
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDto request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            return ResponseEntity.badRequest().body("Token is required");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            return ResponseEntity.badRequest().body("Password must be at least 6 characters");
        }
        try {
            rolesService.resetPassword(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Reset failed: " + e.getMessage());
        }
    }
}

