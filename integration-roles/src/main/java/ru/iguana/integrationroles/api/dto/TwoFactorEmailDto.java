package ru.iguana.integrationroles.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TwoFactorEmailDto {
    private String address;
    private String theme;        // "TWO_FACTOR_CODE"
    private String purpose;      // LOGIN | ENABLE_2FA | DISABLE_2FA
    private String code;
    private Integer ttlMinutes;
    private String userSub;
}
