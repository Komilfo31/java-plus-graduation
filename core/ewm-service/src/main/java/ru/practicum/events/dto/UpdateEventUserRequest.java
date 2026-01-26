package ru.practicum.events.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.practicum.events.convertors.StringToEventStateConverter;
import ru.practicum.events.enums.EventState;
import ru.practicum.validation.EventDate;
import ru.practicum.validation.Marker;

import java.time.LocalDateTime;

import static ru.practicum.util.Constants.*;

@Data
public class UpdateEventUserRequest {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = LENGTH_ANNOTATION_EVENT_MIN, max = LENGTH_ANNOTATION_EVENT_MAX, message = "Длина краткого описания события не прошла валидацию.", groups = Marker.OnUpdate.class)
    private String annotation;

    @JsonProperty(value = "category", access = JsonProperty.Access.WRITE_ONLY)
    private Integer categoryId;  // категория к которой относится событие

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = LENGTH_DESCRIPTION_EVENT_MIN, max = LENGTH_DESCRIPTION_EVENT_MAX, message = "Длина описания события не прошла валидацию.", groups = Marker.OnUpdate.class)
    private String description;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonFormat(pattern = PATTERN_FORMATE_DATE)
    @EventDate
    private LocalDateTime eventDate;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private LocationDto location;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Boolean paid;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @PositiveOrZero(message = "Количество участников не может быть отрицательным.", groups = Marker.OnUpdate.class)
    private Integer participantLimit;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Boolean requestModeration;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonDeserialize(converter = StringToEventStateConverter.class)
    private EventState stateAction;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = LENGTH_TITLE_EVENT_MIN, max = LENGTH_TITLE_EVENT_MAX, message = "Длина заголовка события не прошла валидацию.", groups = Marker.OnUpdate.class)
    private String title;
}
