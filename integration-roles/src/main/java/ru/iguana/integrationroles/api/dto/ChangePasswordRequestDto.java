package ru.iguana.integrationroles.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDto {
    private String currentSub;
    private String currentPassword;
    private String newPassword;
}
