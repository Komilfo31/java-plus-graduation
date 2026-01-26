package ru.practicum.request;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
@Validated
@Slf4j
public class RequestController {
    private final RequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable @Positive Integer userId,
                                                 @RequestParam @Positive Integer eventId) {
        return requestService.createRequest(userId, eventId);
    }

    // GET /users/{userId}/requests
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getUserRequests(@PathVariable @Positive Integer userId) {
        return requestService.getUserRequests(userId);
    }

    // PATCH /users/{userId}/requests/{requestId}/cancel
    @PatchMapping("/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public ParticipationRequestDto cancelRequest(@PathVariable @Positive Integer userId,
                                                 @PathVariable @Positive Integer requestId) {
        return requestService.cancelRequest(userId, requestId);
    }
}
