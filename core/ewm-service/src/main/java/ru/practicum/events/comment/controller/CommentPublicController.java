package ru.practicum.events.comment.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.events.comment.dto.CommentDto;
import ru.practicum.events.comment.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentPublicController {
    private final CommentService commentService;

    @GetMapping("/events/{eventId}/comment")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentDto> getEventComments(
            @PathVariable @Positive Integer eventId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        return commentService.getEventComments(eventId, from, size);
    }

    @GetMapping("/comment/search")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentDto> searchComments(
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer eventId,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        return commentService.getComments(content, userId, eventId, rangeStart, rangeEnd, from, size);
    }
}
