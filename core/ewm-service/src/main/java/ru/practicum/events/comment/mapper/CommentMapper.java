package ru.practicum.events.comment.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.events.comment.dto.CommentDto;
import ru.practicum.events.comment.model.Comment;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommentMapper {

    public CommentDto commentToDto(Comment comment) {
        if (comment == null) {
            return null;
        }

        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setUserid(comment.getUser().getId());
        dto.setEventid(comment.getEvent().getId());

        return dto;
    }

    public List<CommentDto> commentsToDtos(List<Comment> comments) {
        if (comments == null) {
            return List.of();
        }

        return comments.stream()
                .map(this::commentToDto)
                .collect(Collectors.toList());
    }
}
