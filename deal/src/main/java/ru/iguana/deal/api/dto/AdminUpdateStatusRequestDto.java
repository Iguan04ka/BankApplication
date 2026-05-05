package ru.iguana.deal.api.dto;

import lombok.Data;

@Data
public class AdminUpdateStatusRequestDto {
    private String status;
    private String reason;
}
