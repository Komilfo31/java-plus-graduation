package ru.practicum.category.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.validation.FieldDescription;
import ru.practicum.validation.Marker;

import static ru.practicum.util.Constants.LENGTH_NAME_CATEGORY_MAX;
import static ru.practicum.util.Constants.LENGTH_NAME_CATEGORY_MIN;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCategoryDto {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = LENGTH_NAME_CATEGORY_MIN, max = LENGTH_NAME_CATEGORY_MAX, groups = {Marker.OnCreate.class, Marker.OnUpdate.class})

    @NotBlank(message = "При создании категории должно быть указано ее наименование.", groups = {Marker.OnCreate.class, Marker.OnUpdate.class})
    @FieldDescription("Наименование категории")
    @NotNull(message = "Наименование категории не может быть NULL.", groups = {Marker.OnCreate.class, Marker.OnUpdate.class}) // ⭐️ Убедитесь, что @NotNull в нужной группе
    String name;
}
