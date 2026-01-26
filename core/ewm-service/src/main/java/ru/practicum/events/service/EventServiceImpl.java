package ru.practicum.events.service;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
import ru.practicum.handling.exception.ConflictException;
import ru.practicum.events.dto.*;
import ru.practicum.events.enums.EventState;
import ru.practicum.events.enums.EventStateAction;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.params.AdminEventParams;
import ru.practicum.events.params.PublicEventParams;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.user.UserRepository;
import ru.practicum.user.model.User;
import ru.practicum.handling.exception.NotFoundException;
import ru.practicum.handling.exception.ValidationException;
import ru.practicum.request.*;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.dto.RequestStatusUpdateRequest;
import ru.practicum.statistics.dto.EndpointHitDto;
import ru.practicum.statistics.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final UserRepository userRepository;
    private final StatClient statClient;
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);


    @Override
    public List<EventFullDto> search(AdminEventParams params) {
        validatePaginationParams(params);
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());

        Specification<Event> spec = buildAdminSpecification(params);
        Page<Event> events = eventRepository.findAll(spec, pageable);

        // Получаем ID всех найденных событий
        List<Integer> eventIds = events.getContent().stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        // Получаем подтвержденные запросы для всех событий
        Map<Integer, Integer> confirmedRequestsMap = getConfirmedRequestsForEvents(eventIds);

        // Получаем просмотры для всех событий (с обработкой ошибок)
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
                Integer confirmed = requestRepository.countConfirmedByEventId(eventId);
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

        // Если сервис статистики недоступен, возвращаем 0 для всех событий
        for (Integer eventId : eventIds) {
            viewsMap.put(eventId, 0);
        }

        // Попытка получить статистику, если сервис доступен
        try {
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            LocalDateTime start = EPOCH;
            LocalDateTime end = LocalDateTime.now();

            List<ViewStatsDto> stats = statClient.getStats(start, end, uris, true);

            // Обновляем карту просмотров
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
            // Оставляем значения по умолчанию (0)
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
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));

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
        // получить просмотры ДО записи хита
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
        // записываем хит ПОСЛЕ подсчёта
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

        Integer confirmed = requestRepository.countConfirmedByEventId(eventId);
        confirmed = confirmed == null ? 0 : confirmed;

        EventFullDto dto = eventMapper.toEventFullDto(event);

        dto.setConfirmedRequests(confirmed);
        // views + 1, потому что только что добавили хит
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
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Request> rq = sub.from(Request.class);
                sub.select(cb.count(rq));
                sub.where(cb.equal(rq.get("event").get("id"), root.get("id")),
                        cb.equal(rq.get("status"), "CONFIRMED"));
                predicates.add(cb.or(
                        cb.equal(root.get("participantLimit"), 0),
                        cb.greaterThan(root.get("participantLimit"), sub)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int from = params.getFrom() == null ? 0 : params.getFrom();
        int size = params.getSize() == null ? 10 : params.getSize();
        Pageable pageable;
        if ("VIEWS".equalsIgnoreCase(params.getSort())) {
            pageable = PageRequest.of(from / size, size); // сортировать по views в памяти ниже
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
            List<ConfirmedCount> counts = requestRepository.countConfirmedForEventIds(ids);
            if (counts != null) {
                for (ConfirmedCount c : counts) {
                    confirmedMap.put(c.getEventId(), c.getCnt());
                }
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
        // 1. Проверяем существование события и принадлежность его пользователю
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found."));

        // Проверка, является ли пользователь инициатором события
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("User with id=" + userId +
                    " is not the initiator of event with id=" + eventId);
        }

        // 2. Получаем все запросы для этого события
        List<Request> requests = requestRepository.findByEventId(eventId);

        // 3. Маппинг и возврат DTO
        return requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }


    private Specification<Event> buildAdminSpecification(AdminEventParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Фильтр по пользователям
            if (params.getUsers() != null && !params.getUsers().isEmpty()) {
                predicates.add(root.get("initiator").get("id").in(params.getUsers()));
            }

            // Фильтр по состояниям
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

            // Фильтр по категориям
            if (params.getCategories() != null && !params.getCategories().isEmpty()) {
                predicates.add(root.get("category").get("id").in(params.getCategories()));
            }

            // Фильтр по дате начала
            if (params.getRangeStart() != null) {
                LocalDateTime start = parseDateTime(params.getRangeStart());
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), start));
            }

            // Фильтр по дате окончания
            if (params.getRangeEnd() != null) {
                LocalDateTime end = parseDateTime(params.getRangeEnd());
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), end));
            }

            // Валидация диапазона дат
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
        // 1. Проверки сущностей
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("User with id=" + userId + " is not initiator of event with id=" + eventId);
        }

        // 2. Если заявок нет в запросе — bad request
        if (updateRequest.getRequestIds() == null || updateRequest.getRequestIds().isEmpty()) {
            throw new ValidationException("requestIds must be not empty");
        }

        RequestStatus targetStatus = updateRequest.getStatus();
        if (targetStatus == null || targetStatus == RequestStatus.PENDING) {
            throw new ValidationException("Invalid target status");
        }

        // 3. Получаем все указанные запросы и проверяем, что они принадлежат событию
        List<Request> requests = requestRepository.findAllByIdIn(updateRequest.getRequestIds());
        if (requests.size() != updateRequest.getRequestIds().size()) {
            throw new NotFoundException("One or more requests not found");
        }

        for (Request r : requests) {
            if (!r.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Request id=" + r.getId() + " does not belong to event id=" + eventId);
            }
        }

        // 4. Проверяем бизнес-условия: изменять можно только PENDING-заявки
        List<Request> nonPending = requests.stream()
                .filter(r -> r.getStatus() != RequestStatus.PENDING)
                .collect(Collectors.toList());
        if (!nonPending.isEmpty()) {
            throw new ConflictException("Only requests with status PENDING can be changed");
        }

        List<ParticipationRequestDto> confirmedDtos = new ArrayList<>();
        List<ParticipationRequestDto> rejectedDtos = new ArrayList<>();

        // 5. Если хотим подтвердить заявки
        if (targetStatus == RequestStatus.CONFIRMED) {
            // если лимит = 0 или нет премодерации => подтверждение не требуется
            if (event.getParticipantLimit() == 0 || Boolean.FALSE.equals(event.getRequestModeration())) {
                return EventRequestStatusUpdateResult.builder()
                        .confirmedRequests(Collections.emptyList())
                        .rejectedRequests(Collections.emptyList())
                        .build();
            }

            long confirmedCountNow = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            int limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();

            // --- Проверка: если лимит уже достигнут, кидаем 409 ---
            if (limit > 0 && confirmedCountNow >= limit) {
                throw new ConflictException("The participant limit has been reached");
            }

            // сортируем заявки по created ASC
            List<Request> sorted = requests.stream()
                    .sorted(Comparator.comparing(Request::getCreated))
                    .collect(Collectors.toList());

            for (Request req : sorted) {
                if (confirmedCountNow < limit) {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmedCountNow++;
                    requestRepository.save(req);
                    confirmedDtos.add(requestMapper.toParticipationRequestDto(req));
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                    requestRepository.save(req);
                    rejectedDtos.add(requestMapper.toParticipationRequestDto(req));
                }
            }
        } else if (targetStatus == RequestStatus.REJECTED) {
            // отклоняем все указанные заявки
            for (Request req : requests) {
                req.setStatus(RequestStatus.REJECTED);
                requestRepository.save(req);
                rejectedDtos.add(requestMapper.toParticipationRequestDto(req));
            }
        }

        // Гарантируем транзакционность — всё сохранено
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedDtos)
                .rejectedRequests(rejectedDtos)
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
            throw new ConflictException("Cannot publish event that is not in PENDING state");
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден.", log));

        Event event = eventMapper.toEvent(user, newEventDto, category);

        event = eventRepository.save(event);

        log.info("Добавлено новое событие {}.", event);

        return eventMapper.toEventFullDto(event);
    }

    @Override
    public EventFullDto update(Integer userId, Integer eventId, UpdateEventUserRequest updateEventUserRequest) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден.", log));

        Event oldEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено.", log));

        if (oldEvent.getState() == EventState.PUBLISHED)
            throw new ConflictException("Only pending or canceled events can be changed.");

        // Вместо создания нового частичного Event, обновляем старый Event напрямую из DTO.
        // Это устраняет риски BeanUtils.copyProperties и Reflection.getIgnoreProperties.

        // 1. Обновление категории (Ваша логика)
        if (updateEventUserRequest.getCategoryId() != null) {
            Category category = categoryRepository.findById(updateEventUserRequest.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Категория с id = " + updateEventUserRequest.getCategoryId() + " не найдена.", log));
            oldEvent.setCategory(category);
        }

        // 2. Обновление полей, только если они не null в DTO
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

        // **КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ** - Защита participantLimit
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
            // Логика обработки CANCELED или SEND_TO_REVIEW
            oldEvent.setState(updateEventUserRequest.getStateAction() == EventState.CANCELED ? EventState.CANCELED : EventState.PENDING);
        }


        eventRepository.save(oldEvent);

        log.info("Пользователем обновлены данные события {}.", oldEvent);

        return eventMapper.toEventFullDto(oldEvent);
    }

    @Override
    public List<EventShortDto> findAllByUser(Integer userId, int from, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден.", log));

        List<Event> eventListAll = eventRepository.findByInitiatorOrderByIdAsc(user);

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден.", log));

        Event event = eventRepository.findByInitiatorAndId(user, eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found", log));

        log.info("Получены данные по событию c id = {} у пользователя с id = {}.", eventId, userId);

        return eventMapper.toEventFullDto(event);
    }
}
