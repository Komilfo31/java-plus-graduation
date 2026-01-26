package ru.practicum.events.comment.service;

import ru.practicum.events.comment.dto.CommentDto;
import ru.practicum.events.comment.dto.DeleteCommentDto;
import ru.practicum.events.comment.dto.CommonCommentDto;

import java.util.List;

public interface CommentService {
    CommentDto createComment(Integer userId, Integer eventId, CommonCommentDto newCommentDto);

    CommentDto updateComment(Integer userId, Integer commentId, CommonCommentDto updateCommentDto);

    void deleteCommentByUser(Integer userId, Integer commentId);

    List<CommentDto> getEventComments(Integer eventId, Integer from, Integer size);

    List<CommentDto> getCommentsByUserId(Integer userId);

    void deleteCommentByAdmin(DeleteCommentDto deleteCommentsDto);

    void deleteSingleCommentByAdmin(Integer commentId);

    List<CommentDto> getUserEventComments(Integer userId, Integer eventId);

    List<CommentDto> getComments(String content, Integer userId, Integer eventId,
                                 String rangeStart, String rangeEnd, Integer from, Integer size);
}
