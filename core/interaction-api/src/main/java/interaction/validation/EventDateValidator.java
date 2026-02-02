package interaction.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static interaction.constants.Constants.OFFSET_EVENT_DATE;

public class EventDateValidator implements ConstraintValidator<EventDate, LocalDateTime> {
    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) return true; //для этого случая, считаю валидацию успешной

        Instant now = Instant.now();
        Instant eventInstant = value.atZone(ZoneId.systemDefault()).toInstant();

        return value.isAfter(LocalDateTime.now().plusSeconds(OFFSET_EVENT_DATE));
    }
}
