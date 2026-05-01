package ru.iguana.integrationroles.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class PasswordResetEmailDto {
    private String address;
    private String theme;
    private String resetUrl;
    private String token;
    private Integer ttlMinutes;
    private String userSub;
}
