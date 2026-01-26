package ru.practicum.events.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommonCommentDto {
    @NotBlank(message = "Содержание комментария не может быть пустым")
    @Size(min = 3, max = 3000, message = "Комментарий должен содержать от 3 до 3000 символов")
    private String text;
}
