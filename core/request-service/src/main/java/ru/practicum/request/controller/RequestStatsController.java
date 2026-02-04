package ru.practicum.request.controller;

import interaction.exceptions.exception.ConflictException;
import interaction.exceptions.exception.NotFoundException;
import interaction.model.request.ParticipationRequestDto;
import interaction.model.request.RequestStatus;
import interaction.model.request.RequestStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.request.mapper.ConfirmedCount;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.repository.RequestRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestStatsController {

    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;

    @PatchMapping("/batch/status")
    public List<ParticipationRequestDto> updateRequestsStatusBatch(
            @RequestBody RequestStatusUpdateRequest batchRequest) {

        List<Request> requests = requestRepository.findAllByIdIn(batchRequest.getRequestIds());

        if (requests.size() != batchRequest.getRequestIds().size()) {
            throw new NotFoundException("One or more requests not found");
        }

        for (Request request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Only requests with status PENDING can be changed");
            }

            if (batchRequest.getRequestIds() != null &&
                    batchRequest.getRequestIds().contains(request.getId())) {
                request.setStatus(RequestStatus.CONFIRMED);
            }  else if (batchRequest.getStatus() != null) {
                request.setStatus(batchRequest.getStatus());
            }
        }

        List<Request> savedRequests = requestRepository.saveAll(requests);
        return savedRequests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/events/{eventId}/confirmed/count")
    public Integer getConfirmedRequestsCount(@PathVariable Integer eventId) {
        Integer count = requestRepository.countConfirmedByEventId(eventId);
        return count != null ? count : 0;
    }

    @GetMapping("/events/confirmed/batch")
    public Map<Integer, Integer> getConfirmedRequestsBatch(@RequestParam List<Integer> eventIds) {
        Map<Integer, Integer> result = new HashMap<>();

        List<ConfirmedCount> counts = requestRepository.countConfirmedForEventIds(eventIds);
        for (ConfirmedCount count : counts) {
            result.put(count.getEventId(), count.getCnt());
        }

        for (Integer eventId : eventIds) {
            result.putIfAbsent(eventId, 0);
        }

        return result;
    }

    @GetMapping("/events/{eventId}")
    public List<ParticipationRequestDto> getRequestsByEventId(@PathVariable Integer eventId) {
        List<Request> requests = requestRepository.findByEventId(eventId);
        return requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/batch")
    public List<ParticipationRequestDto> getRequestsByIds(@RequestBody List<Integer> requestIds) {
        List<Request> requests = requestRepository.findAllByIdIn(requestIds);
        return requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/events/{eventId}/count-by-status")
    public Integer countByEventIdAndStatus(@PathVariable Integer eventId,
                                           @RequestParam RequestStatus status) {
        Integer count = requestRepository.countByEventIdAndStatus(eventId, status);
        return count != null ? count : 0;
    }
}
