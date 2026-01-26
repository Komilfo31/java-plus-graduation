package ru.practicum.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.practicum.validation.Marker;

@Data
public class LocationDto {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull(message = "Нужно указать широту места события.", groups = Marker.OnCreate.class)
    private Float lat;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull(message = "Нужно указать долготу места события.", groups = Marker.OnCreate.class)
    private Float lon;
}
