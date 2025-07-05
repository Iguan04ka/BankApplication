package ru.iguana.integrationroles.api.dto;

import lombok.Data;

@Data
public class UserKeyDto {
    private String sub;
    private String systemCode;
}
