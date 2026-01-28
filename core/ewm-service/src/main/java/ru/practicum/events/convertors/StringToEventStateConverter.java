package ru.practicum.events.convertors;

import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.events.enums.EventState;
import ru.practicum.handling.exception.ForbiddenOperationException;

import static ru.practicum.util.Constants.VALUE_CANCEL_REVIEW;
import static ru.practicum.util.Constants.VALUE_SEND_TO_REVIEW;

@Component
@Slf4j
public class StringToEventStateConverter extends StdConverter<String, EventState> {

    @Override
    public EventState convert(String value) {
        if (value.equals(VALUE_SEND_TO_REVIEW)) {
            return EventState.PENDING;
        } else if (value.equals(VALUE_CANCEL_REVIEW)) {
            return EventState.CANCELED;
        } else {
            throw new ForbiddenOperationException("Only pending or canceled events can be changed.", log);
        }
    }
}
