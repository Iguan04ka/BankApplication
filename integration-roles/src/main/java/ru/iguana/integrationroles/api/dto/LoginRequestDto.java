package ru.iguana.integrationroles.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {
    private String sub;
    private String password;
}
