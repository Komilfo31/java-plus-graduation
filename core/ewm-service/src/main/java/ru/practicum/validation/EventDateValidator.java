package ru.practicum.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

import static ru.practicum.util.Constants.OFFSET_EVENT_DATE;

public class EventDateValidator implements ConstraintValidator<EventDate, LocalDateTime> {
    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) return true; //для этого случая, считаю валидацию успешной

        return value.isAfter(LocalDateTime.now().plusSeconds(OFFSET_EVENT_DATE));
    }
}
