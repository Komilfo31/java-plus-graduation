package ru.practicum.events.service;

import interaction.model.event.dto.EventFullDto;
import interaction.model.event.dto.EventShortDto;
import interaction.model.event.dto.NewEventDto;
import interaction.model.event.dto.UpdateEventAdminRequest;
import interaction.model.event.dto.UpdateEventUserRequest;
import interaction.model.request.EventRequestStatusUpdateResult;
import interaction.model.request.ParticipationRequestDto;
import interaction.model.request.RequestStatusUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.events.params.AdminEventParams;
import ru.practicum.events.params.PublicEventParams;

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

    EventFullDto getEventFullDtoById(Integer eventId);

    EventShortDto getEventShortDtoById(Integer eventId);

    List<EventShortDto> getByIds(List<Integer> eventIds);

    Boolean existsById(Integer eventId);

    String getEventState(Integer eventId);
}
