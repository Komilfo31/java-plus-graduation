package ru.practicum.category.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDto {

    private Integer id;

    @Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters")
    @NotNull
    private String name;
}