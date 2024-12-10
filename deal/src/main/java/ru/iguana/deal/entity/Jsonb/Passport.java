package ru.iguana.deal.entity.Jsonb;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Passport {
    String series;

    String number;

    String issueBranch;

    Date issueDate;
}
