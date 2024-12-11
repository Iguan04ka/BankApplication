package ru.iguana.deal.entity.Jsonb;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Passport {
    String series;

    String number;

    String issueBranch;

    Date issueDate;
}
