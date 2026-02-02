package ru.practicum.events.service;

import interaction.client.RequestFeignClient;
import interaction.client.UserFeignClient;
import interaction.exceptions.exception.ConflictException;
import interaction.exceptions.exception.DuplicatedDataException;
import interaction.exceptions.exception.NotFoundException;
import interaction.exceptions.exception.ValidationException;
import interaction.model.event.dto.EventFullDto;
import interaction.model.event.dto.EventShortDto;
import interaction.model.event.dto.NewEventDto;
import interaction.model.event.dto.UpdateEventAdminRequest;
import interaction.model.event.dto.UpdateEventUserRequest;
import interaction.model.event.enums.EventState;
import interaction.model.event.enums.EventStateAction;
import interaction.model.request.EventRequestStatusUpdateResult;
import interaction.model.request.ParticipationRequestDto;
import interaction.model.request.RequestStatus;
import interaction.model.request.RequestStatusUpdateRequest;
import interaction.model.user.UserShortDto;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.storage.CategoryRepository;
import ru.practicum.client.StatClient;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.params.AdminEventParams;
import ru.practicum.events.params.PublicEventParams;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.statistics.dto.EndpointHitDto;
import ru.practicum.statistics.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final StatClient statClient;
    private final RequestFeignClient requestClient;
    private final UserFeignClient userFeignClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    @Override
    public List<EventFullDto> search(AdminEventParams params) {
        validatePaginationParams(params);
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());

        Specification<Event> spec = buildAdminSpecification(params);
        Page<Event> events = eventRepository.findAll(spec, pageable);

        List<Integer> eventIds = events.getContent().stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Integer, Integer> confirmedRequestsMap = getConfirmedRequestsForEvents(eventIds);
        Map<Integer, Integer> viewsMap = getViewsForEvents(eventIds);

        return events.getContent().stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toEventFullDto(event);
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0));
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private Map<Integer, Integer> getConfirmedRequestsForEvents(List<Integer> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Integer> confirmedMap = new HashMap<>();

        for (Integer eventId : eventIds) {
            try {
                Integer confirmed = requestClient.getConfirmedRequestsCount(eventId);
                confirmedMap.put(eventId, confirmed != null ? confirmed : 0);
            } catch (Exception e) {
                log.warn("Failed to get confirmed requests for event {}: {}", eventId, e.getMessage());
                confirmedMap.put(eventId, 0);
            }
        }

        return confirmedMap;
    }

    private Map<Integer, Integer> getViewsForEvents(List<Integer> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Integer> viewsMap = new HashMap<>();

        for (Integer eventId : eventIds) {
            viewsMap.put(eventId, 0);
        }

        try {
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            LocalDateTime start = EPOCH;
            LocalDateTime end = LocalDateTime.now();

            List<ViewStatsDto> stats = statClient.getStats(start, end, uris, true);

            for (ViewStatsDto stat : stats) {
                try {
                    Integer eventId = extractEventIdFromUri(stat.getUri());
                    if (eventId != null) {
                        viewsMap.put(eventId, stat.getHits());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse event ID from URI: {}", stat.getUri());
                }
            }
        } catch (Exception ex) {
            log.warn("Stat service unavailable, using default views (0)");
        }

        return viewsMap;
    }

    private Integer extractEventIdFromUri(String uri) {
        if (uri == null || !uri.startsWith("/events/")) {
            return null;
        }
        try {
            return Integer.parseInt(uri.substring("/events/".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int getEventViews(Integer eventId) {
        try {
            LocalDateTime start = EPOCH;
            LocalDateTime end = LocalDateTime.now();
            List<String> uris = List.of("/events/" + eventId);

            List<ViewStatsDto> stats = statClient.getStats(start, end, uris, false);
            return stats.stream().mapToInt(ViewStatsDto::getHits).sum();
        } catch (Exception ex) {
            log.warn("Failed to fetch views for event {}: {}", eventId, ex.getMessage());
            return 0;
        }
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Integer eventId, UpdateEventAdminRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ConflictException("Event with id=" + eventId + " not found"));

        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }

        updateEventFields(event, dto);
        handleStateAction(event, dto.getStateAction());

        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    @Transactional
    public EventFullDto getPublicEventById(Integer eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));

        if (!"PUBLISHED".equalsIgnoreCase(event.getState().name())) {
            throw new NotFoundException("Event with id=" + eventId + " is not published");
        }
        LocalDateTime start = event.getPublishedOn() != null ? event.getPublishedOn()
                : (event.getCreatedOn() != null ? event.getCreatedOn() : EPOCH);
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = List.of("/events/" + eventId);
        int views = 0;
        try {
            List<ViewStatsDto> stats = statClient.getStats(start, end, uris, true);
            views = stats.stream().mapToInt(ViewStatsDto::getHits).sum();
        } catch (Exception ex) {
            log.warn("Failed to fetch views for event {}: {}", eventId, ex.getMessage());
        }
        try {
            EndpointHitDto hit = EndpointHitDto.builder()
                    .app("ewm-main-service")
                    .uri("/events/" + eventId)
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();
            statClient.hit(hit);
        } catch (Exception ex) {
            log.warn("Stat hit failed for event {}: {}", eventId, ex.getMessage());
        }

        Integer confirmed = 0;
        try {
            confirmed = requestClient.getConfirmedRequestsCount(eventId);
        } catch (Exception e) {
            log.warn("Failed to get confirmed requests for event {}: {}", eventId, e.getMessage());
        }

        EventFullDto dto = eventMapper.toEventFullDto(event);
        dto.setConfirmedRequests(confirmed);
        dto.setViews(views + 1);

        return dto;
    }

    @Override
    public List<EventShortDto> searchPublicEvents(PublicEventParams params, HttpServletRequest request) {
        if (params.getFrom() != null && params.getFrom() < 0) throw new ValidationException("from must be >= 0");
        if (params.getSize() != null && (params.getSize() <= 0 || params.getSize() > 1000))
            throw new ValidationException("size must be between 1 and 1000");

        Specification<Event> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));

            if (params.getText() != null && !params.getText().isBlank()) {
                String pat = "%" + params.getText().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("annotation")), pat),
                        cb.like(cb.lower(root.get("description")), pat)
                ));
            }
            if (params.getCategories() != null && !params.getCategories().isEmpty()) {
                predicates.add(root.get("category").get("id").in(params.getCategories()));
            }
            if (params.getPaid() != null) {
                predicates.add(cb.equal(root.get("paid"), params.getPaid()));
            }
            LocalDateTime now = LocalDateTime.now();
            if ((params.getRangeStart() == null || params.getRangeStart().isBlank()) &&
                    (params.getRangeEnd() == null || params.getRangeEnd().isBlank())) {
                predicates.add(cb.greaterThan(root.get("eventDate"), now));
            } else {
                if (params.getRangeStart() != null && !params.getRangeStart().isBlank()) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"),
                            LocalDateTime.parse(params.getRangeStart(), formatter)));
                }
                if (params.getRangeEnd() != null && !params.getRangeEnd().isBlank()) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"),
                            LocalDateTime.parse(params.getRangeEnd(), formatter)));
                }
            }
            if (Boolean.TRUE.equals(params.getOnlyAvailable())) {
                predicates.add(cb.equal(root.get("participantLimit"), 0));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int from = params.getFrom() == null ? 0 : params.getFrom();
        int size = params.getSize() == null ? 10 : params.getSize();
        Pageable pageable;
        if ("VIEWS".equalsIgnoreCase(params.getSort())) {
            pageable = PageRequest.of(from / size, size);
        } else {
            pageable = PageRequest.of(from / size, size, Sort.by("eventDate").ascending());
        }

        Page<Event> page = eventRepository.findAll(spec, pageable);
        List<Event> events = page.getContent();

        try {
            EndpointHitDto hit = EndpointHitDto.builder()
                    .app("ewm-main-service")
                    .uri(request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""))
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();
            statClient.hit(hit);
        } catch (Exception ex) {
            log.warn("Stat hit failed for search: {}", ex.getMessage());
        }
        List<String> uris = events.stream().map(e -> "/events/" + e.getId()).collect(Collectors.toList());
        Map<Integer, Integer> viewsMap = new HashMap<>();
        if (!uris.isEmpty()) {
            try {
                LocalDateTime start = (params.getRangeStart() != null && !params.getRangeStart().isBlank())
                        ? LocalDateTime.parse(params.getRangeStart(), formatter) : EPOCH;
                LocalDateTime end = (params.getRangeEnd() != null && !params.getRangeEnd().isBlank())
                        ? LocalDateTime.parse(params.getRangeEnd(), formatter) : LocalDateTime.now();
                List<ViewStatsDto> stats = statClient.getStats(start, end, uris, false);
                for (ViewStatsDto s : stats) {
                    String uri = s.getUri();
                    if (uri == null) continue;
                    Integer id = Integer.valueOf(uri.substring(uri.lastIndexOf('/') + 1));
                    viewsMap.put(id, s.getHits());
                }
            } catch (Exception ex) {
                log.warn("Failed to fetch batch views: {}", ex.getMessage());
            }
        }
        List<Integer> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Integer, Integer> confirmedMap = new HashMap<>();
        if (!ids.isEmpty()) {
            Map<Integer, Integer> batchResult = new HashMap<>();
            try {
                batchResult = requestClient.getConfirmedRequestsBatch(ids);
            } catch (Exception e) {
                log.warn("Failed to get batch confirmed requests: {}", e.getMessage());
            }

            for (Integer id : ids) {
                confirmedMap.put(id, batchResult.getOrDefault(id, 0));
            }
        }
        List<EventShortDto> dtos = events.stream().map(e -> {
            EventShortDto s = eventMapper.toEventShortDto(e);
            s.setViews(viewsMap.getOrDefault(e.getId(), 0));
            s.setConfirmedRequests(confirmedMap.getOrDefault(e.getId(), 0));
            return s;
        }).collect(Collectors.toList());

        if ("VIEWS".equalsIgnoreCase(params.getSort())) {
            dtos.sort(Comparator.comparing(EventShortDto::getViews, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getRequestsByEvent(Integer userId, Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found."));

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("User with id=" + userId +
                    " is not the initiator of event with id=" + eventId);
        }

        List<ParticipationRequestDto> requests;
        try {
            requests = requestClient.getRequestsByEventId(eventId);
        } catch (Exception e) {
            log.warn("Failed to get requests via Feign for event {}: {}", eventId, e.getMessage());
            throw new RuntimeException("Failed to fetch requests: " + e.getMessage());
        }

        return requests;
    }

    private Specification<Event> buildAdminSpecification(AdminEventParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (params.getUsers() != null && !params.getUsers().isEmpty()) {
                predicates.add(root.get("initiatorId").in(params.getUsers()));
            }

            if (params.getStates() != null && !params.getStates().isEmpty()) {
                List<EventState> eventStates = params.getStates().stream()
                        .map(state -> {
                            try {
                                return EventState.valueOf(state.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                throw new ValidationException("Invalid state: " + state);
                            }
                        })
                        .collect(Collectors.toList());
                predicates.add(root.get("state").in(eventStates));
            }

            if (params.getCategories() != null && !params.getCategories().isEmpty()) {
                predicates.add(root.get("category").get("id").in(params.getCategories()));
            }

            if (params.getRangeStart() != null) {
                LocalDateTime start = parseDateTime(params.getRangeStart());
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), start));
            }

            if (params.getRangeEnd() != null) {
                LocalDateTime end = parseDateTime(params.getRangeEnd());
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), end));
            }

            if (params.getRangeStart() != null && params.getRangeEnd() != null) {
                LocalDateTime start = parseDateTime(params.getRangeStart());
                LocalDateTime end = parseDateTime(params.getRangeEnd());
                if (end.isBefore(start)) {
                    throw new ValidationException("RangeEnd cannot be before rangeStart");
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestsStatus(Integer userId, Integer eventId, RequestStatusUpdateRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("User with id=" + userId + " is not initiator of event with id=" + eventId);
        }

        if (updateRequest.getRequestIds() == null || updateRequest.getRequestIds().isEmpty()) {
            throw new ValidationException("requestIds must be not empty");
        }

        RequestStatus targetStatus = updateRequest.getStatus();
        if (targetStatus == null || targetStatus == RequestStatus.PENDING) {
            throw new ValidationException("Invalid target status");
        }

        List<ParticipationRequestDto> requestDtos;
        try {
            requestDtos = requestClient.getRequestsByIds(updateRequest.getRequestIds());
        } catch (Exception e) {
            log.warn("Failed to get requests via Feign: {}", e.getMessage());
            throw new NotFoundException("Failed to fetch requests: " + e.getMessage());
        }

        if (requestDtos.size() != updateRequest.getRequestIds().size()) {
            throw new NotFoundException("One or more requests not found");
        }

        for (ParticipationRequestDto req : requestDtos) {
            if (!req.getEvent().equals(eventId)) {
                throw new ConflictException("Request id=" + req.getId() + " does not belong to event id=" + eventId);
            }
        }

        List<ParticipationRequestDto> nonPending = requestDtos.stream()
                .filter(r -> r.getStatus() == null || !r.getStatus().equalsIgnoreCase("PENDING"))
                .collect(Collectors.toList());
        if (!nonPending.isEmpty()) {
            throw new ConflictException("Only requests with status PENDING can be changed");
        }

        if (event.getParticipantLimit() == 0 || Boolean.FALSE.equals(event.getRequestModeration())) {
            List<ParticipationRequestDto> confirmed = new ArrayList<>();
            for (ParticipationRequestDto req : requestDtos) {
                req.setStatus(RequestStatus.CONFIRMED.name());
                confirmed.add(req);
            }

            try {
                RequestStatusUpdateRequest batchRequest = new RequestStatusUpdateRequest();
                batchRequest.setRequestIds(updateRequest.getRequestIds());
                batchRequest.setStatus(RequestStatus.CONFIRMED);
                requestClient.updateRequestsStatusBatch(batchRequest);
            } catch (Exception e) {
                log.error("Failed to update request statuses via Feign: {}", e.getMessage());
                throw new ConflictException("Failed to update request statuses");
            }

            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(confirmed)
                    .rejectedRequests(Collections.emptyList())
                    .build();
        }

        if (targetStatus == RequestStatus.CONFIRMED) {
            long confirmedCountNow = 0;
            try {
                Integer count = requestClient.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
                confirmedCountNow = count != null ? count.longValue() : 0L;
            } catch (Exception e) {
                log.warn("Failed to get confirmed count via Feign: {}", e.getMessage());
            }

            int participantLimit = event.getParticipantLimit() != null ? event.getParticipantLimit() : 0;

            int freeLimit = (int) Math.min(
                    participantLimit - confirmedCountNow,
                    updateRequest.getRequestIds().size()
            );

            if (freeLimit <= 0) {
                throw new ConflictException("The participant limit has been reached");
            }

            List<Integer> confirmedIds = updateRequest.getRequestIds().subList(0, freeLimit);
            List<Integer> rejectedIds = updateRequest.getRequestIds().subList(freeLimit, updateRequest.getRequestIds().size());

            List<ParticipationRequestDto> confirmed = new ArrayList<>();
            List<ParticipationRequestDto> rejected = new ArrayList<>();

            for (ParticipationRequestDto req : requestDtos) {
                if (confirmedIds.contains(req.getId())) {
                    req.setStatus(RequestStatus.CONFIRMED.name());
                    confirmed.add(req);
                } else {
                    req.setStatus(RequestStatus.REJECTED.name());
                    rejected.add(req);
                }
            }

            try {
                if (!confirmedIds.isEmpty()) {
                    RequestStatusUpdateRequest confirmRequest = new RequestStatusUpdateRequest();
                    confirmRequest.setRequestIds(confirmedIds);
                    confirmRequest.setStatus(RequestStatus.CONFIRMED);
                    requestClient.updateRequestsStatusBatch(confirmRequest);
                }

                if (!rejectedIds.isEmpty()) {
                    RequestStatusUpdateRequest rejectRequest = new RequestStatusUpdateRequest();
                    rejectRequest.setRequestIds(rejectedIds);
                    rejectRequest.setStatus(RequestStatus.REJECTED);
                    requestClient.updateRequestsStatusBatch(rejectRequest);
                }
            } catch (Exception e) {
                log.error("Failed to update request statuses via Feign: {}", e.getMessage());
                throw new ConflictException("Failed to update request statuses");
            }

            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(confirmed)
                    .rejectedRequests(rejected)
                    .build();

        } else if (targetStatus == RequestStatus.REJECTED) {
            List<ParticipationRequestDto> rejected = new ArrayList<>();

            for (ParticipationRequestDto req : requestDtos) {
                req.setStatus(RequestStatus.REJECTED.name());
                rejected.add(req);
            }

            try {
                RequestStatusUpdateRequest batchRequest = new RequestStatusUpdateRequest();
                batchRequest.setRequestIds(updateRequest.getRequestIds());
                batchRequest.setStatus(RequestStatus.REJECTED);
                requestClient.updateRequestsStatusBatch(batchRequest);
            } catch (Exception e) {
                log.error("Failed to update request statuses via Feign: {}", e.getMessage());
                throw new ConflictException("Failed to update request statuses");
            }

            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(Collections.emptyList())
                    .rejectedRequests(rejected)
                    .build();
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(Collections.emptyList())
                .rejectedRequests(Collections.emptyList())
                .build();
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest dto) {
        if (dto.getAnnotation() != null) {
            validateAnnotation(dto.getAnnotation());
            event.setAnnotation(dto.getAnnotation());
        }

        if (dto.getDescription() != null) {
            validateDescription(dto.getDescription());
            event.setDescription(dto.getDescription());
        }

        if (dto.getTitle() != null) {
            validateTitle(dto.getTitle());
            event.setTitle(dto.getTitle());
        }

        if (dto.getEventDate() != null) {
            validateEventDate(dto.getEventDate());
            event.setEventDate(dto.getEventDate());
        }

        if (dto.getLocation() != null) {
            event.setLocationLat(dto.getLocation().getLat());
            event.setLocationLon(dto.getLocation().getLon());
        }

        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }

        if (dto.getParticipantLimit() != null) {
            validateParticipantLimit(dto.getParticipantLimit());
            event.setParticipantLimit(dto.getParticipantLimit());
        }

        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
    }

    private void handleStateAction(Event event, EventStateAction stateAction) {
        if (stateAction == null) return;

        switch (stateAction) {
            case PUBLISH_EVENT:
                validatePublishEvent(event);
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                break;

            case REJECT_EVENT:
                validateRejectEvent(event);
                event.setState(EventState.CANCELED);
                break;
        }
    }

    private void validatePublishEvent(Event event) {
        if (event.getState() != EventState.PENDING) {
            throw new DuplicatedDataException("Cannot publish event that is not in PENDING state");
        }

        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Cannot publish event less than 1 hour before event date");
        }
    }

    private void validateRejectEvent(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Cannot reject already published event");
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format. Expected: yyyy-MM-dd HH:mm:ss");
        }
    }

    private void validateEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Event date cannot be in the past");
        }
    }

    private void validatePaginationParams(AdminEventParams params) {
        if (params.getFrom() < 0) {
            throw new ValidationException("From must be >= 0");
        }
        if (params.getSize() <= 0) {
            throw new ValidationException("Size must be > 0");
        }
        if (params.getSize() > 1000) {
            throw new ValidationException("Size cannot exceed 1000");
        }
    }

    private void validateAnnotation(String annotation) {
        if (annotation.length() < 20 || annotation.length() > 2000) {
            throw new ValidationException("Annotation must be between 20 and 2000 characters");
        }
        if (annotation.trim().isEmpty()) {
            throw new ValidationException("Annotation cannot be empty or contain only spaces");
        }
    }

    private void validateDescription(String description) {
        if (description == null) {
            throw new ValidationException("Description cannot be null");
        }

        if (description.length() < 20 || description.length() > 7000) {
            throw new ValidationException("Description must be between 20 and 7000 characters");
        }
        if (description.trim().isEmpty()) {
            throw new ValidationException("Description cannot be empty or contain only spaces");
        }
    }

    private void validateTitle(String title) {
        if (title.length() < 3 || title.length() > 120) {
            throw new ValidationException("Title must be between 3 and 120 characters");
        }
        if (title.trim().isEmpty()) {
            throw new ValidationException("Title cannot be empty or contain only spaces");
        }
    }

    private void validateParticipantLimit(Integer participantLimit) {
        if (participantLimit < 0) {
            throw new ValidationException("Participant limit cannot be negative");
        }
    }

    @Override
    public EventFullDto add(Integer userId, NewEventDto newEventDto) {
        Category category = categoryRepository.findById(newEventDto.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Категория с id = " + newEventDto.getCategoryId() + " не найдена.", log));

        try {
            UserShortDto userDto = userFeignClient.getById(userId);
            if (userDto == null) {
                throw new NotFoundException("Пользователь с id = " + userId + " не найден.", log);
            }
        } catch (Exception e) {
            log.warn("Failed to get user via Feign: {}", e.getMessage());
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.", log);
        }

        if (newEventDto.getDescription() == null || newEventDto.getDescription().trim().isEmpty()) {
            throw new ValidationException("Description cannot be null or empty");
        }

        if (newEventDto.getAnnotation() == null || newEventDto.getAnnotation().trim().isEmpty()) {
            throw new ValidationException("Annotation cannot be null or empty");
        }

        if (newEventDto.getParticipantLimit() != null) {
            if (newEventDto.getParticipantLimit() < 0) {
                throw new ValidationException("Participant limit cannot be negative. Provided: " +
                        newEventDto.getParticipantLimit());
            }

            if (newEventDto.getParticipantLimit() > 100000) {
                throw new ValidationException("Participant limit is too high. Maximum allowed: 100000");
            }
        }

        if (newEventDto.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (newEventDto.getEventDate().isBefore(now)) {
                throw new ValidationException("Event date cannot be in the past. Provided: " +
                        newEventDto.getEventDate() + ", Current: " + now);
            }

            LocalDateTime minAllowedDate = now.plusHours(2);
            if (newEventDto.getEventDate().isBefore(minAllowedDate)) {
                throw new ValidationException("Event date must be at least 2 hours from now. " +
                        "Provided: " + newEventDto.getEventDate() + ", Minimum allowed: " + minAllowedDate);
            }
        }

        Event event = eventMapper.toEvent(userId, newEventDto, category);
        event = eventRepository.save(event);

        log.info("Добавлено новое событие {}.", event);

        return eventMapper.toEventFullDto(event);
    }

    @Override
    public EventFullDto update(Integer userId, Integer eventId, UpdateEventUserRequest updateEventUserRequest) {
        try {
            UserShortDto userDto = userFeignClient.getById(userId);
            if (userDto == null) {
                throw new NotFoundException("Пользователь с id = " + userId + " не найден.", log);
            }
        } catch (Exception e) {
            log.warn("Failed to get user via Feign: {}", e.getMessage());
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.", log);
        }

        Event oldEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено.", log));

        if (oldEvent.getState() == EventState.PUBLISHED)
            throw new ConflictException("Only pending or canceled events can be changed.");

        if (LocalDateTime.now().isAfter(oldEvent.getEventDate().minusHours(1).minusSeconds(5))) {
            throw new ConflictException("Cannot change event less than 1 hour before it starts");
        }

        if (updateEventUserRequest.getEventDate() != null) {
            LocalDateTime newEventDate = updateEventUserRequest.getEventDate();

            LocalDateTime minAllowedDate = LocalDateTime.now().plusHours(2);
            if (newEventDate.isBefore(minAllowedDate)) {
                throw new ValidationException("Даты события не может быть раньше, чем через два часа от текущего момента");
            }

            if (LocalDateTime.now().isAfter(newEventDate.minusHours(1).minusSeconds(5))) {
                throw new ConflictException("New event date must be at least 1 hour from now");
            }
        }

        if (updateEventUserRequest.getCategoryId() != null) {
            Category category = categoryRepository.findById(updateEventUserRequest.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Категория с id = " + updateEventUserRequest.getCategoryId() + " не найдена.", log));
            oldEvent.setCategory(category);
        }

        if (updateEventUserRequest.getTitle() != null) {
            oldEvent.setTitle(updateEventUserRequest.getTitle());
        }
        if (updateEventUserRequest.getAnnotation() != null) {
            oldEvent.setAnnotation(updateEventUserRequest.getAnnotation());
        }
        if (updateEventUserRequest.getDescription() != null) {
            oldEvent.setDescription(updateEventUserRequest.getDescription());
        }
        if (updateEventUserRequest.getEventDate() != null) {
            oldEvent.setEventDate(updateEventUserRequest.getEventDate());
        }
        if (updateEventUserRequest.getPaid() != null) {
            oldEvent.setPaid(updateEventUserRequest.getPaid());
        }

        if (updateEventUserRequest.getParticipantLimit() != null) {
            oldEvent.setParticipantLimit(updateEventUserRequest.getParticipantLimit());
        }

        if (updateEventUserRequest.getRequestModeration() != null) {
            oldEvent.setRequestModeration(updateEventUserRequest.getRequestModeration());
        }
        if (updateEventUserRequest.getLocation() != null) {
            oldEvent.setLocationLat(updateEventUserRequest.getLocation().getLat());
            oldEvent.setLocationLon(updateEventUserRequest.getLocation().getLon());
        }
        if (updateEventUserRequest.getStateAction() != null) {
            oldEvent.setState(updateEventUserRequest.getStateAction() == EventState.CANCELED ? EventState.CANCELED : EventState.PENDING);
        }

        eventRepository.save(oldEvent);

        log.info("Пользователем обновлены данные события {}.", oldEvent);

        return eventMapper.toEventFullDto(oldEvent);
    }

    @Override
    public List<EventShortDto> findAllByUser(Integer userId, int from, int size) {
        try {
            UserShortDto userDto = userFeignClient.getById(userId);
            if (userDto == null) {
                throw new NotFoundException("Пользователь с id = " + userId + " не найден.", log);
            }
        } catch (Exception e) {
            log.warn("Failed to get user via Feign: {}", e.getMessage());
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.", log);
        }

        List<Event> eventListAll = eventRepository.findByInitiatorOrderByIdAsc(userId);

        int toIndex = from + size;
        if (toIndex > eventListAll.size() - 1) toIndex = eventListAll.size();

        if (from > toIndex) {
            from = 0;
            toIndex = 0;
        }

        List<Event> eventList = new ArrayList<Event>(eventListAll.subList(from, toIndex));

        log.info("Получен список событий пользователя с id {} и параметрами: from = {}, size = {}.", userId, from, size);

        return eventList.stream()
                .map(eventMapper::toEventShortDto)
                .toList();
    }

    @Override
    public EventFullDto findByUserAndEvent(Integer userId, Integer eventId) {
        try {
            UserShortDto userDto = userFeignClient.getById(userId);
            if (userDto == null) {
                throw new NotFoundException("Пользователь с id = " + userId + " не найден.", log);
            }
        } catch (Exception e) {
            log.warn("Failed to get user via Feign: {}", e.getMessage());
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.", log);
        }

        Event event = eventRepository.findByInitiatorAndId(userId, eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found", log));

        log.info("Получены данные по событию c id = {} у пользователя с id = {}.", eventId, userId);

        return eventMapper.toEventFullDto(event);
    }

    @Override
    public EventFullDto getEventFullDtoById(Integer eventId) {
        log.info("Получение полной информации о событии с ID = {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));

        updateEventViews(event);

        Integer confirmedRequests = 0;
        try {
            confirmedRequests = requestClient.getConfirmedRequestsCount(eventId);
        } catch (Exception e) {
            log.warn("Failed to get confirmed requests for event {}: {}", eventId, e.getMessage());
        }

        EventFullDto dto = eventMapper.toEventFullDto(event);
        dto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);

        return dto;
    }

    @Override
    public EventShortDto getEventShortDtoById(Integer eventId) {
        log.info("Получение краткой информации о событии с ID = {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));

        updateEventViews(event);

        Integer confirmedRequests = 0;
        try {
            confirmedRequests = requestClient.getConfirmedRequestsCount(eventId);
        } catch (Exception e) {
            log.warn("Failed to get confirmed requests for event {}: {}", eventId, e.getMessage());
        }

        EventShortDto dto = eventMapper.toEventShortDto(event);
        dto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);

        return dto;
    }

    @Override
    public List<EventShortDto> getByIds(List<Integer> eventIds) {
        log.info("Получение списка событий по IDs: {}", eventIds);

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Event> events = eventRepository.findAllById(eventIds);

        updateEventsViews((List<Event>) events);

        Map<Integer, Integer> confirmedMap = getConfirmedRequestsForEvents(eventIds);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setConfirmedRequests(confirmedMap.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Boolean existsById(Integer eventId) {
        return eventRepository.existsById(eventId);
    }

    @Override
    public String getEventState(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));
        return event.getState().name();
    }

    private void updateEventViews(Event event) {
        try {
            Integer currentViews = getEventViewsFromStats(event.getId());
            if (currentViews > event.getViews()) {
                event.setViews(currentViews);
                eventRepository.save(event);
            }
        } catch (Exception ex) {
            log.warn("Failed to update views for event {}: {}", event.getId(), ex.getMessage());
        }
    }

    private void updateEventsViews(List<Event> events) {
        if (events.isEmpty()) {
            return;
        }

        List<Integer> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Integer, Integer> viewsMap = getViewsForEventsFromStats(eventIds);

        boolean needsSave = false;
        for (Event event : events) {
            Integer currentViews = viewsMap.getOrDefault(event.getId(), 0);
            if (currentViews > event.getViews()) {
                event.setViews(currentViews);
                needsSave = true;
            }
        }

        if (needsSave) {
            eventRepository.saveAll(events);
        }
    }

    private Integer getEventViewsFromStats(Integer eventId) {
        try {
            LocalDateTime start = EPOCH;
            LocalDateTime end = LocalDateTime.now();
            List<String> uris = List.of("/events/" + eventId);

            List<ViewStatsDto> stats = statClient.getStats(start, end, uris, false);
            return stats.stream().mapToInt(ViewStatsDto::getHits).sum();
        } catch (Exception ex) {
            log.warn("Failed to fetch views for event {}: {}", eventId, ex.getMessage());
            return 0;
        }
    }

    private Map<Integer, Integer> getViewsForEventsFromStats(List<Integer> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Integer> viewsMap = new HashMap<>();

        try {
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            LocalDateTime start = EPOCH;
            LocalDateTime end = LocalDateTime.now();

            List<ViewStatsDto> stats = statClient.getStats(start, end, uris, true);

            for (ViewStatsDto stat : stats) {
                try {
                    Integer eventId = extractEventIdFromUri(stat.getUri());
                    if (eventId != null) {
                        viewsMap.put(eventId, stat.getHits());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse event ID from URI: {}", stat.getUri());
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch batch views: {}", ex.getMessage());
        }

        for (Integer eventId : eventIds) {
            viewsMap.putIfAbsent(eventId, 0);
        }

        return viewsMap;
    }
}
