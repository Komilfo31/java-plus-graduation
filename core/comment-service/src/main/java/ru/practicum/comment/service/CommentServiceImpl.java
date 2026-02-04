package ru.practicum.comment.service;

import interaction.client.EventFeignClient;
import interaction.client.UserFeignClient;
import interaction.exceptions.exception.BadRequestException;
import interaction.exceptions.exception.ConflictException;
import interaction.exceptions.exception.NotFoundException;
import interaction.model.comment.CommentDto;
import interaction.model.comment.CommonCommentDto;
import interaction.model.comment.DeleteCommentDto;
import interaction.model.event.dto.EventFullDto;
import interaction.model.event.dto.EventShortDto;
import interaction.model.user.UserShortDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.repository.CommentRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserFeignClient userFeignClient;
    private final EventFeignClient eventFeignClient;
    private final CommentMapper commentMapper;

    @Override
    public CommentDto createComment(Integer userId, Integer eventId, CommonCommentDto newCommentDto) {
        if (!checkEventExistsAndPublished(eventId)) {
            throw new ConflictException("Нельзя добавить комментарий если событие не опубликовано");
        }

        checkUserExists(userId);

        Comment comment = Comment.builder()
                .created(LocalDateTime.now())
                .text(newCommentDto.getText())
                .eventId(eventId)
                .userId(userId)
                .build();

        return commentMapper.commentToDto(commentRepository.save(comment));
    }

    @Override
    public CommentDto updateComment(Integer userId, Integer commentId, CommonCommentDto updateCommentDto) {
        Comment comment = getComment(Long.valueOf(commentId));
        getCommentByUserId(userId, commentId);

        comment.setText(updateCommentDto.getText());

        return commentMapper.commentToDto(commentRepository.save(comment));
    }

    @Override
    public void deleteCommentByUser(Integer userId, Integer commentId) {
        checkUserExists(userId);
        getCommentByUserId(userId, commentId);
        commentRepository.deleteById(Long.valueOf(commentId));
    }

    @Override
    public List<CommentDto> getComments(String text, Integer userId, Integer eventId,
                                        String rangeStart, String rangeEnd, Integer from, Integer size) {

        if (userId != null) {
            checkUserExists(userId);
        }

        if (eventId != null) {
            checkEventExists(eventId);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

        LocalDateTime start;
        if (rangeStart == null || rangeStart.isBlank()) {
            start = LocalDateTime.of(1900, 1, 1, 0, 0); // Начало времен
        } else {
            start = LocalDateTime.parse(rangeStart, formatter);
        }

        LocalDateTime end;
        if (rangeEnd == null || rangeEnd.isBlank()) {
            end = LocalDateTime.now();
        } else {
            end = LocalDateTime.parse(rangeEnd, formatter);
        }

        if (start.isAfter(end)) {
            throw new BadRequestException("Начало времени поиска не может быть позднее его окончания");
        }

        int safeFrom = (from != null) ? Math.max(from, 0) : 0;
        int safeSize = (size != null) ? Math.max(size, 1) : 100;

        PageRequest pageRequest = PageRequest.of(safeFrom / safeSize, safeSize);

        Page<Comment> commentsPage = commentRepository.getComments(
                text, userId, eventId, start, end, pageRequest
        );

        return commentsPage.getContent()
                .stream()
                .map(commentMapper::commentToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getEventComments(Integer eventId, Integer from, Integer size) {
        checkEventExists(eventId);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("created").descending());

        Page<Comment> commentPage = commentRepository.findByEventId(eventId, pageable);

        return commentMapper.commentsToDtos(commentPage.getContent());
    }

    @Override
    public List<CommentDto> getCommentsByUserId(Integer userId) {
        checkUserExists(userId);
        List<Comment> commentsList = commentRepository.findByUserId(userId);
        return commentsList.stream()
                .map(commentMapper::commentToDto)
                .toList();
    }

    @Override
    public List<CommentDto> getUserEventComments(Integer userId, Integer eventId) {
        checkUserExists(userId);
        checkEventExists(eventId);

        List<Comment> comments = commentRepository.findByUserIdAndEventIdOrderByCreatedDesc(userId, eventId);

        return commentMapper.commentsToDtos(comments);
    }

    @Transactional
    @Override
    public void deleteCommentByAdmin(DeleteCommentDto deleteCommentsDto) {
        List<Comment> existingComments = commentRepository.findByIdIn(deleteCommentsDto.getCommentsIds());

        List<Integer> existingCommentIds = existingComments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        List<Integer> commentIdsNotExist = deleteCommentsDto.getCommentsIds().stream()
                .filter(commentId -> !existingCommentIds.contains(commentId))
                .collect(Collectors.toList());

        if (!commentIdsNotExist.isEmpty()) {
            throw new NotFoundException("Комментарии с id: " + commentIdsNotExist + " не найдены");
        }

        commentRepository.deleteByIdIn(deleteCommentsDto.getCommentsIds());
    }

    @Transactional
    @Override
    public void deleteSingleCommentByAdmin(Integer commentId) {
        DeleteCommentDto dto = new DeleteCommentDto();
        dto.setCommentsIds(List.of(commentId));
        deleteCommentByAdmin(dto);
    }

    private void checkUserExists(Integer userId) {
        try {
            UserShortDto userDto = userFeignClient.getById(userId);
            if (userDto == null) {
                throw new NotFoundException("Пользователь с id: " + userId + " не существует");
            }
        } catch (Exception e) {
            throw new NotFoundException("Пользователь с id: " + userId + " не существует");
        }
    }

    private boolean checkEventExistsAndPublished(Integer eventId) {
        try {
            EventFullDto eventStatus = eventFeignClient.getEventStatus(eventId);
            if (eventStatus == null) {
                throw new NotFoundException("События с id: " + eventId + " не существует");
            }
            return "PUBLISHED".equalsIgnoreCase(eventStatus.getState());
        } catch (Exception e) {
            throw new NotFoundException("События с id: " + eventId + " не существует");
        }
    }

    private void checkEventExists(Integer eventId) {
        try {
            EventShortDto eventDto = eventFeignClient.getEventById(eventId);
            if (eventDto == null) {
                throw new NotFoundException("Событие с id=" + eventId + " не найдено");
            }
        } catch (Exception e) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }
    }

    private Comment getCommentByUserId(Integer userId, Integer commentId) {
        return commentRepository.findByUserIdAndId(userId, commentId).orElseThrow(
                () -> new ConflictException(
                        "Пользователю id: " + userId + " не принадлежит комментарий с id: " + commentId)
        );
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException("Комментария с id: " + commentId + " не существует")
        );
    }
}