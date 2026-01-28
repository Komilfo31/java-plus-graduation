package ru.practicum.events.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.events.dto.*;
import ru.practicum.events.params.AdminEventParams;
import ru.practicum.events.params.PublicEventParams;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.dto.RequestStatusUpdateRequest;

import java.util.List;

public interface EventService {
    List<EventFullDto> search(AdminEventParams params);

    EventFullDto updateEventByAdmin(Integer eventId, UpdateEventAdminRequest dto);

    public EventFullDto add(Integer userId, NewEventDto newEventDto);

    public EventFullDto update(Integer userId, Integer eventId, UpdateEventUserRequest updateEventUserRequest);

    EventFullDto getPublicEventById(Integer eventId, HttpServletRequest request);

    List<EventShortDto> searchPublicEvents(PublicEventParams params, HttpServletRequest request);

    EventRequestStatusUpdateResult changeRequestsStatus(Integer userId, Integer eventId, RequestStatusUpdateRequest updateRequest);

    List<EventShortDto> findAllByUser(Integer userId, int from, int size);

    EventFullDto findByUserAndEvent(Integer userId, Integer eventId);

    List<ParticipationRequestDto> getRequestsByEvent(Integer userId, Integer eventId);
}
