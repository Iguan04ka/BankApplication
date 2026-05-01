package ru.iguana.integrationroles.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeSubRequestDto {
    private String currentSub;
    private String newSub;
    private String currentPassword;
}
