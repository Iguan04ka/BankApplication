package ru.iguana.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outbound DTO sent from gateway to integration-roles. The currentSub is filled
 * from the JWT in the gateway controller.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDto {
    private String currentSub;
    private String currentPassword;
    private String newPassword;
}
