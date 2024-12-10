package ru.iguana.deal.entity.Jsonb;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatusHistory {
    String status;

    Timestamp time;

    String changeType;
}
