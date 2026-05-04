package ru.iguana.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound from frontend during settings-driven 2FA enable/disable flows.
 * The sub is taken from the authenticated JWT, not the body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorCodeRequestDto {
    private String code;
}
