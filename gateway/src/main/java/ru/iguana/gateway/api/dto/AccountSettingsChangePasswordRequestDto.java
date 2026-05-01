package ru.iguana.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountSettingsChangePasswordRequestDto {
    private String currentPassword;
    private String newPassword;
}
