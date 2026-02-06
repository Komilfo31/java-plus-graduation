package ru.practicum.events.controller;

import interaction.exceptions.exception.BadRequestException;
import interaction.model.event.dto.EventFullDto;
import interaction.model.event.dto.EventShortDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.events.params.PublicEventParams;
import ru.practicum.events.service.EventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class EventPublicController {
    private final EventService eventService;

    @GetMapping("/{eventId}")
    public EventFullDto getEvent(@PathVariable Integer eventId, HttpServletRequest request) {
        return eventService.getPublicEventById(eventId, request);
    }

    @GetMapping
    public ResponseEntity<List<EventShortDto>> searchEvents(@ModelAttribute PublicEventParams params,
                                                            HttpServletRequest request) {

        try {
            if (params.getRangeStart() != null && params.getRangeEnd() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime start = LocalDateTime.parse(params.getRangeStart(), formatter);
                LocalDateTime end = LocalDateTime.parse(params.getRangeEnd(), formatter);

                if (start.isAfter(end)) {
                    throw new BadRequestException("Дата начала диапазона не может быть позже даты окончания");
                }
            }
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Неверный формат даты. Используйте: yyyy-MM-dd HH:mm:ss");
        }

        List<EventShortDto> events = eventService.searchPublicEvents(params, request);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}/status")
    public EventFullDto getEventStatus(@PathVariable Integer id) {
        return eventService.getEventFullDtoById(id);
    }

    @GetMapping("/full-event-by-id")
    public EventFullDto getEventFullDtoById(@RequestParam Integer eventId) {
        log.info("Запрос от микросервиса request-service события с ID = {}", eventId);
        return eventService.getEventFullDtoById(eventId);
    }

    @GetMapping("/short-event-by-id")
    public EventShortDto getEventShortDtoById(@RequestParam Integer eventId) {
        log.info("Запрос от микросервиса comment-service события с ID = {}", eventId);
        return eventService.getEventShortDtoById(eventId);
    }

    @GetMapping("/by-ids")
    public List<EventShortDto> getByIds(@RequestParam List<Integer> eventIds) {
        log.info("Запрос микросервисом событий с ID {}", eventIds);
        return eventService.getByIds(eventIds);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Integer userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("Запрос рекомендаций для пользователя с ID {}, from={}, size={}", userId, from, size);
        return eventService.getRecommendations(userId, from, size);
    }

    @PutMapping("/{eventId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likeEvent(
            @PathVariable Integer eventId,
            @RequestHeader("X-EWM-USER-ID") Integer userId) {
        log.info("Пользователь {} лайкает событие {}", userId, eventId);
        eventService.likeEvent(eventId, userId);
    }
}

