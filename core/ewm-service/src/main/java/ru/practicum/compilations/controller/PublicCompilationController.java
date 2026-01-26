package ru.practicum.compilations.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.params.PublicCompilationsParams;
import ru.practicum.compilations.service.CompilationService;

import java.util.List;

@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PublicCompilationController {

    private final CompilationService compilationService;

    @GetMapping
    public List<CompilationDto> getCompilations(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        PublicCompilationsParams params = PublicCompilationsParams.builder()
                .pinned(pinned)
                .from(from)
                .size(size)
                .build();

        log.info("Запрос на получение подборок: закрепленные={}, from={}, size={}",
                pinned, from, size);
        return compilationService.findCompilations(params);
    }

    @GetMapping("/{compId}")
    public CompilationDto getCompilation(@PathVariable @Positive Integer compId) {
        log.info("Запрос на получение подборки ID={}", compId);
        return compilationService.findCompilationById(compId);
    }
}