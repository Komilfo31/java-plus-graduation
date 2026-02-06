package interaction.client;


import interaction.model.request.ParticipationRequestDto;
import interaction.model.request.RequestStatus;
import interaction.model.request.RequestStatusUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", fallbackFactory = RequestClientFallbackFactory.class)
public interface RequestFeignClient {

    @GetMapping("/requests/events/{eventId}/confirmed/count")
    Integer getConfirmedRequestsCount(@PathVariable("eventId") Integer eventId);

    @GetMapping("/requests/events/confirmed/batch")
    Map<Integer, Integer> getConfirmedRequestsBatch(@RequestParam("eventIds") List<Integer> eventIds);

    @GetMapping("/requests/events/{eventId}")
    List<ParticipationRequestDto> getRequestsByEventId(@PathVariable("eventId") Integer eventId);

    @PostMapping("/requests/batch")
    List<ParticipationRequestDto> getRequestsByIds(@RequestBody List<Integer> requestIds);

    @GetMapping("/requests/events/{eventId}/count-by-status")
    Integer countByEventIdAndStatus(@PathVariable("eventId") Integer eventId,
                                    @RequestParam("status") RequestStatus status);

    @PostMapping("/requests/batch/status")
    List<ParticipationRequestDto> updateRequestsStatusBatch(
            @RequestBody RequestStatusUpdateRequest batchRequest);

    @GetMapping("/users/{userId}")
    List<ParticipationRequestDto> getRequestsByUserId(@PathVariable Integer userId);
}
