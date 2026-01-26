package ru.practicum.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.StatService;
import ru.practicum.statistics.dto.EndpointHitDto;
import ru.practicum.statistics.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Validated
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class StatController {
    private final StatService statService;
    private static final String DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";


    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointHitDto hit(@RequestBody EndpointHitDto request) {
        return statService.hit(request);
    }


    @GetMapping("/stats")
    public Collection<ViewStatsDto> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique) {

        if (start.isAfter(end)) {
            // Spring автоматически маппирует IllegalArgumentException в 400 Bad Request
            throw new IllegalArgumentException("Дата начала (" + start +
                    ") не может быть позже даты окончания (" + end + ").");
        }
        // Передача параметров в сервис, как обычно
        return statService.getStats(start, end, uris, unique);
    }


}


