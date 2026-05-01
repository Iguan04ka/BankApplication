package ru.iguana.deal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request body carrying the one-time SES confirmation code")
public class SesCodeRequestDto {
    @Schema(description = "6-digit one-time confirmation code", example = "493812")
    String code;
}
