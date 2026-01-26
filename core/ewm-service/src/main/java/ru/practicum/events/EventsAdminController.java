package ru.practicum.events;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.dto.UpdateEventAdminRequest;
import ru.practicum.events.params.AdminEventParams;
import ru.practicum.events.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class EventsAdminController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventFullDto>> searchEvents(
            @RequestParam(required = false) List<Integer> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Integer> categories,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "0") @Min(0) Integer from,
            @RequestParam(defaultValue = "10") @Min(1) @Max(1000) Integer size) {

        AdminEventParams params = AdminEventParams.builder()
                .users(users)
                .states(states)
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .from(from)
                .size(size)
                .build();

        List<EventFullDto> events = eventService.search(params);
        return ResponseEntity.ok(events);
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateEvent(
            @PathVariable @Positive Integer eventId,
            @Valid @RequestBody UpdateEventAdminRequest updateRequest) {

        EventFullDto updatedEvent = eventService.updateEventByAdmin(eventId, updateRequest);
        return ResponseEntity.ok(updatedEvent);
    }
}
