package interaction.model.category.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import interaction.validation.FieldDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static interaction.constants.Constants.LENGTH_NAME_CATEGORY_MAX;
import static interaction.constants.Constants.LENGTH_NAME_CATEGORY_MIN;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCategoryDto {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = LENGTH_NAME_CATEGORY_MIN, max = LENGTH_NAME_CATEGORY_MAX)

    @NotBlank(message = "При создании категории должно быть указано ее наименование.")
    @FieldDescription("Наименование категории")
    @NotNull(message = "Наименование категории не может быть NULL.")
    String name;
}
