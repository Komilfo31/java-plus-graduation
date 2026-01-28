package ru.practicum.events.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.events.enums.EventStateAction;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventAdminRequest {
    @Size(min = 20, max = 2000, message = "Аннотация должна содержать от 20 до 2000 символов")
    @JsonProperty("annotation")
    private String annotation;

    @JsonProperty("category")
    private Integer category;

    @Size(min = 20, max = 7000, message = "Описание должно содержать от 20 до 7000 символов")
    @JsonProperty("description")
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("eventDate")
    private LocalDateTime eventDate;

    @JsonProperty("location")
    private LocationDto location;

    @JsonProperty("paid")
    private Boolean paid;

    @PositiveOrZero(message = "Лимит участников должен быть положительным или нулевым")
    @JsonProperty("participantLimit")
    private Integer participantLimit;

    @JsonProperty("requestModeration")
    private Boolean requestModeration;

    @Enumerated(EnumType.STRING)
    @JsonProperty("stateAction")
    private EventStateAction stateAction;

    @Size(min = 3, max = 120, message = "Заголовок должен содержать от 3 до 120 символов")
    @JsonProperty("title")
    private String title;
}
