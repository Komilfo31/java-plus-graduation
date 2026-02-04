package interaction.model.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import interaction.model.category.dto.CategoryDto;
import interaction.model.user.UserShortDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDto {
    private Integer id;

    @NotBlank(message = "Аннотация не может быть пустой")
    @Size(min = 20, max = 2000, message = "Аннотация должна содержать от 20 до 2000 символов")
    private String annotation;

    @NotNull(message = "Категория не может быть пустой")
    private CategoryDto category;

    private Integer confirmedRequests;

    @NotNull(message = "Дата события не может быть пустой")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private String eventDate;

    @NotNull(message = "Инициатор не может быть равен null")
    private UserShortDto initiator;

    @NotNull(message = "Paid cannot be null")
    private Boolean paid;

    @NotBlank(message = "Заголовок не может быть пустым")
    @Size(min = 3, max = 120, message = "Заголовок должен содержать от 3 до 120 символов")
    private String title;

    private Integer views;
}
