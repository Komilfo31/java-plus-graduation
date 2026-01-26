package ru.practicum.events;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.params.PublicEventParams;
import ru.practicum.events.service.EventService;
import ru.practicum.handling.exception.BadRequestException;

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
}

