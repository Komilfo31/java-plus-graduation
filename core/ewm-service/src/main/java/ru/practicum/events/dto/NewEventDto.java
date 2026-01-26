package ru.practicum.events.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.practicum.validation.EventDate;
import ru.practicum.validation.Marker;

import java.time.LocalDateTime;

import static ru.practicum.util.Constants.*;

@Data
public class NewEventDto {
    public NewEventDto() {
        paid = false;
        participantLimit = 0;
        requestModeration = true;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = LENGTH_ANNOTATION_EVENT_MIN, max = LENGTH_ANNOTATION_EVENT_MAX, message = "Длина краткого описания события не прошла валидацию.")
    @NotBlank(message = "Краткое описание события не может быть пустым.", groups = Marker.OnCreate.class)
    private String annotation;

    @JsonProperty(value = "category", access = JsonProperty.Access.WRITE_ONLY)
    @NotNull(message = "Категория должна быть указана при создании события.", groups = Marker.OnCreate.class)
    private Integer categoryId;  // категория к которой относится событие

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = LENGTH_DESCRIPTION_EVENT_MIN, max = LENGTH_DESCRIPTION_EVENT_MAX, message = "Длина описания события не прошла валидацию.")
    @NotBlank(message = "Описание события не может быть пустым.", groups = Marker.OnCreate.class)
    private String description;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonFormat(pattern = PATTERN_FORMATE_DATE)
    @EventDate
    private LocalDateTime eventDate;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull(message = "Нужно указать координаты места события.", groups = Marker.OnCreate.class)
    private LocationDto location;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY, defaultValue = "false")
    private Boolean paid;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY, defaultValue = "0")
    @PositiveOrZero(message = "Количество участников не может быть отрицательным.", groups = Marker.OnCreate.class)
    private Integer participantLimit;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY, defaultValue = "true")
    private Boolean requestModeration;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = LENGTH_TITLE_EVENT_MIN, max = LENGTH_TITLE_EVENT_MAX, message = "Длина заголовка события не прошла валидацию.")
    private String title;
}
