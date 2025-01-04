package ru.iguana.deal.api.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import ru.iguana.deal.model.entity.enums.EmailTheme;

import java.util.UUID;

@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailMessageDto {

    String address;

    EmailTheme theme;

    UUID statementId;

    String text;
}
