package ru.practicum.request.service;

import interaction.client.EventFeignClient;
import interaction.client.UserFeignClient;
import interaction.exceptions.exception.ConflictException;
import interaction.exceptions.exception.NotFoundException;
import interaction.model.event.dto.EventFullDto;
import interaction.model.request.ParticipationRequestDto;
import interaction.model.request.RequestStatus;
import interaction.model.request.RequestStatusUpdateRequest;
import interaction.model.user.UserShortDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final UserFeignClient userFeignClient;
    private final EventFeignClient eventFeignClient;
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Integer userId, Integer eventId) {
        checkUserExists(userId);

       EventFullDto event = getEventById(eventId);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Request already exists");
        }

        if (event.getInitiator() != null && event.getInitiator().getId() != null) {
            Integer initiatorId = event.getInitiator().getId();
            if (initiatorId.equals(userId)) {
                throw new ConflictException("Event initiator cannot request participation");
            }
        }

        if (!"PUBLISHED".equalsIgnoreCase(event.getState())) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        Integer participantLimit = event.getParticipantLimit();
        if (participantLimit != null && participantLimit != 0) {
            long confirmedRequestsCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedRequestsCount >= participantLimit) {
                throw new ConflictException("Participant limit reached");
            }
        }

        Request request = new Request();
        request.setCreated(LocalDateTime.now());
        request.setEventId(eventId);
        request.setRequesterId(userId);

        Boolean requestModeration = event.getRequestModeration();
        if (Boolean.FALSE.equals(requestModeration) || (participantLimit != null && participantLimit == 0)) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        Request savedRequest = requestRepository.save(request);
        return requestMapper.toParticipationRequestDto(savedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Integer userId) {
        checkUserExists(userId);

        List<Request> requests = requestRepository.findByRequesterId(userId);

        return requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Integer userId, Integer requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("Cannot cancel request that belongs to another user");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ConflictException("Only pending requests can be canceled");
        }



        request.setStatus(RequestStatus.CANCELED);

        Request savedRequest = requestRepository.save(request);

        return requestMapper.toParticipationRequestDto(savedRequest);
    }

    private void checkUserExists(Integer userId) {
        try {
            UserShortDto userDto = userFeignClient.getById(userId);
            if (userDto == null) {
                throw new NotFoundException("User with id=" + userId + " was not found");
            }
        } catch (Exception e) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private EventFullDto getEventById(Integer eventId) {
        try {
            EventFullDto event = eventFeignClient.getEventFullDtoById(eventId);
            if (event == null) {
                throw new ConflictException("Event with id=" + eventId + " was not found");
            }
            return event;
        } catch (Exception e) {
            throw new ConflictException("Event with id=" + eventId + " was not found");
        }
    }

    @Transactional
    public List<ParticipationRequestDto> updateRequestsStatusBatch(
            RequestStatusUpdateRequest batchRequest) {

        List<Integer> requestIds = batchRequest.getRequestIds();
        RequestStatus newStatus = batchRequest.getStatus();

        // Получаем все запросы
        List<Request> requests = requestRepository.findAllById(requestIds);

        if (requests.size() != requestIds.size()) {
            throw new NotFoundException("Some requests not found");
        }

        // Обновляем статус
        for (Request request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException(
                        "Request id=" + request.getId() + " is not in PENDING status"
                );
            }
            request.setStatus(newStatus);
        }

        // Сохраняем
        List<Request> savedRequests = requestRepository.saveAll(requests);

        // Возвращаем DTO
        return savedRequests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }
}
