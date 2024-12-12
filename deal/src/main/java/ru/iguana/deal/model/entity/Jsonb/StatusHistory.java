package ru.iguana.deal.model.entity.Jsonb;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.iguana.deal.model.entity.enums.ApplicationStatus;
import ru.iguana.deal.model.entity.enums.ChangeType;

import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatusHistory {
    ApplicationStatus status;

    Timestamp time;

    ChangeType changeType;
}
