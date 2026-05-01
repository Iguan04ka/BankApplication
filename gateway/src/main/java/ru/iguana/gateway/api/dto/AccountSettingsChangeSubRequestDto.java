package ru.iguana.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound request body from the frontend — currentSub is taken from the JWT, not the body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountSettingsChangeSubRequestDto {
    private String newSub;
    private String currentPassword;
}
