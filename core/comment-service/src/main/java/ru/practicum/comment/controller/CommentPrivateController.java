package ru.practicum.comment.controller;

import interaction.model.comment.CommentDto;
import interaction.model.comment.CommonCommentDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentPrivateController {
    private final CommentService commentService;

    @PostMapping("/events/{eventId}/users/{userId}/comment")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(
            @PathVariable @Positive Integer eventId,
            @PathVariable @Positive Integer userId,
            @Valid @RequestBody CommonCommentDto newCommentDto) {
        return commentService.createComment(userId, eventId, newCommentDto);
    }

    @PatchMapping("/users/{userId}/comment/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto updateComment(
            @PathVariable @Positive Integer userId,
            @PathVariable @Positive Integer commentId,
            @Valid @RequestBody CommonCommentDto commonCommentDto) {
        return commentService.updateComment(userId, commentId, commonCommentDto);
    }

    @DeleteMapping("/users/{userId}/comment/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable @Positive Integer userId,
            @PathVariable @Positive Integer commentId) {
        commentService.deleteCommentByUser(userId, commentId);
    }

    @GetMapping("/users/{userId}/comment")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentDto> getComments(@PathVariable @Positive Integer userId) {
        return commentService.getCommentsByUserId(userId);
    }

    @GetMapping("/events/{eventId}/users/{userId}/comment")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentDto> getEventComments(
            @PathVariable @Positive Integer eventId,
            @PathVariable @Positive Integer userId) {
        return commentService.getUserEventComments(userId, eventId);
    }
}
