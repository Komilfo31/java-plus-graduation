package ru.practicum.events.controller;

import interaction.exceptions.exception.ConflictException;
import interaction.model.event.dto.EventFullDto;
import interaction.model.event.dto.EventShortDto;
import interaction.model.event.dto.NewEventDto;
import interaction.model.event.dto.UpdateEventUserRequest;
import interaction.model.request.EventRequestStatusUpdateResult;
import interaction.model.request.ParticipationRequestDto;
import interaction.model.request.RequestStatusUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import interaction.validation.Marker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.events.service.EventService;
import java.util.Collection;
import java.util.List;

import static interaction.constants.Constants.DEFAULT_VALUE_0;
import static interaction.constants.Constants.DEFAULT_VALUE_REQUEST_PARAM_SIZE;
import static interaction.constants.Constants.PATH_VARIABLE_EVENT_ID;
import static interaction.constants.Constants.PATH_VARIABLE_USER_ID;
import static interaction.constants.Constants.REQUEST_PARAM_FROM;
import static interaction.constants.Constants.REQUEST_PARAM_SIZE;


@Slf4j
@Validated
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class EventsPrivateController {
    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto add(@PathVariable Integer userId,
                            @Valid @RequestBody NewEventDto newEventDto) {
        log.info("Получен запрос: Добавить новое событие {}", newEventDto.getAnnotation());
        return eventService.add(userId, newEventDto);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    EventFullDto update(@PathVariable(name = PATH_VARIABLE_USER_ID) @Positive(groups = Marker.OnUpdate.class) Integer userId,
                        @PathVariable(name = PATH_VARIABLE_EVENT_ID) @Positive(groups = Marker.OnUpdate.class) Integer eventId,
                        @Validated(Marker.OnUpdate.class)
                        @RequestBody @Valid UpdateEventUserRequest updateEventUserRequest) {
        log.info("Получен запрос: Обновить событие пользователем {}", updateEventUserRequest.getAnnotation());

        return eventService.update(userId, eventId, updateEventUserRequest);
    }

    @PatchMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult changeRequestsStatus(
            @PathVariable @Positive Integer userId,
            @PathVariable @Positive Integer eventId,
            @RequestBody(required = false) RequestStatusUpdateRequest updateRequest) {

        if (updateRequest == null) {
            throw new ConflictException("Request body is required");
        }

        if (updateRequest.getRequestIds() == null || updateRequest.getRequestIds().isEmpty()) {
            throw new ConflictException("Request IDs cannot be empty");
        }

        if (updateRequest.getStatus() == null) {
            throw new ConflictException("Status is required");
        }

        return eventService.changeRequestsStatus(userId, eventId, updateRequest);
    }


    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<EventShortDto> findAll(@PathVariable(name = PATH_VARIABLE_USER_ID) @Positive Integer userId,
                                             @RequestParam(name = REQUEST_PARAM_FROM, defaultValue = DEFAULT_VALUE_0) @PositiveOrZero int from,
                                             @RequestParam(name = REQUEST_PARAM_SIZE, defaultValue = DEFAULT_VALUE_REQUEST_PARAM_SIZE) @Positive int size) {
        log.info("Получен запрос: Получить список событий пользователя c id = {} в количестве size = {} с отступом from = {}", userId, size, from);

        return eventService.findAllByUser(userId, from, size);
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    EventFullDto findById(@PathVariable(name = PATH_VARIABLE_USER_ID) @Positive Integer userId,
                          @PathVariable(name = PATH_VARIABLE_EVENT_ID) @Positive Integer eventId) {
        log.info("Получен запрос: Получить данные по событию c id = {} у пользователя с id = {}.", eventId, userId);

        return eventService.findByUserAndEvent(userId, eventId);
    }

    // GET /users/{userId}/events/{eventId}/requests
    @GetMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getEventRequests(
            @PathVariable @Positive Integer userId,
            @PathVariable @Positive Integer eventId) {

        return eventService.getRequestsByEvent(userId, eventId);
    }
}
