package ru.practicum.events.comment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class DeleteCommentDto {
    @NotEmpty(message = "Список ID комментариев не может быть пустым")
    private List<@Positive Integer> commentsIds;
}
