package ru.practicum.events.mapper;

import interaction.model.event.dto.EventFullDto;
import interaction.model.event.dto.EventShortDto;
import interaction.model.event.dto.Location;
import interaction.model.event.dto.NewEventDto;
import interaction.model.event.dto.UpdateEventUserRequest;
import interaction.model.event.enums.EventState;
import interaction.model.user.UserShortDto;
import org.springframework.stereotype.Component;
import interaction.model.category.dto.CategoryDto;
import ru.practicum.category.model.Category;
import ru.practicum.events.model.Event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class EventMapper {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EventFullDto toEventFullDto(Event event) {
        if (event == null) {
            return null;
        }

        EventFullDto dto = new EventFullDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(event.getCategory().getId());
        categoryDto.setName(event.getCategory().getName());
        dto.setCategory(categoryDto);

        dto.setConfirmedRequests(event.getConfirmedRequests() != null ? event.getConfirmedRequests() : 0);
        dto.setCreatedOn(event.getCreatedOn() != null ?
                event.getCreatedOn().format(formatter) : null);
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate().format(formatter));

        UserShortDto userShortDto = new UserShortDto();
        userShortDto.setId(event.getInitiatorId());
        userShortDto.setName(String.valueOf(event.getInitiatorId()));
        dto.setInitiator(userShortDto);

        Location location = new Location();
        location.setLat(event.getLocationLat());
        location.setLon(event.getLocationLon());
        dto.setLocation(location);

        dto.setPaid(event.getPaid());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setPublishedOn(event.getPublishedOn() != null ?
                event.getPublishedOn().format(formatter) : null);
        dto.setRequestModeration(event.getRequestModeration());
        dto.setState(event.getState().name());
        dto.setTitle(event.getTitle());
        dto.setViews(event.getViews() != null ? event.getViews() : 0);

        return dto;
    }

    public EventShortDto toEventShortDto(Event event) {
        if (event == null) {
            return null;
        }

        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(event.getCategory().getId());
        categoryDto.setName(event.getCategory().getName());
        dto.setCategory(categoryDto);

        dto.setConfirmedRequests(event.getConfirmedRequests() != null ? event.getConfirmedRequests() : 0);
        dto.setEventDate(event.getEventDate().format(formatter));
        UserShortDto userShortDto = new UserShortDto();
        userShortDto.setId(event.getInitiatorId());
        userShortDto.setName(String.valueOf(event.getInitiatorId()));
        dto.setInitiator(userShortDto);

        dto.setPaid(event.getPaid());
        dto.setTitle(event.getTitle());
        dto.setViews(event.getViews() != null ? event.getViews() : 0);

        return dto;
    }

    public Event toEventForUpdate(Integer userId, Integer eventId, UpdateEventUserRequest request) {
        if (request == null) return null;

        Event event = new Event();
        event.setId(eventId);
        event.setInitiatorId(userId);

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getLocation() != null) {
            event.setLocationLat(request.getLocation().getLat());
            event.setLocationLon(request.getLocation().getLon());
        }

        return event;
    }

    public Event toEvent(Integer userId, NewEventDto dto, Category category) {
        if (dto == null) {
            return null;
        }

        Event event = new Event();
        event.setAnnotation(dto.getAnnotation());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setPaid(dto.getPaid() != null ? dto.getPaid() : false);
        event.setParticipantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0);
        event.setRequestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true);
        event.setTitle(dto.getTitle());

        // связки
        event.setInitiatorId(userId);
        event.setCategory(category);

        // координаты
        if (dto.getLocation() != null) {
            event.setLocationLat(dto.getLocation().getLat());
            event.setLocationLon(dto.getLocation().getLon());
        }

        // технические поля
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);

        return event;
    }

}
