package ru.practicum.request.controller;

import interaction.model.request.ParticipationRequestDto;
import interaction.model.request.RequestStatusUpdateRequest;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.request.service.RequestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j
public class RequestController {
    private final RequestService requestService;

    @PostMapping("/users/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable @Positive Integer userId,
                                                 @RequestParam @Positive Integer eventId) {
        return requestService.createRequest(userId, eventId);
    }

    @GetMapping("/users/{userId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getUserRequests(@PathVariable @Positive Integer userId) {
        return requestService.getUserRequests(userId);
    }

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public ParticipationRequestDto cancelRequest(@PathVariable @Positive Integer userId,
                                                 @PathVariable @Positive Integer requestId) {
        return requestService.cancelRequest(userId, requestId);
    }

    @PostMapping("/requests/batch/status")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> updateRequestsStatusBatch(
            @RequestBody RequestStatusUpdateRequest batchRequest) {

        return requestService.updateRequestsStatusBatch(batchRequest);
    }
}
