package interaction.client;

import interaction.model.request.ParticipationRequestDto;
import interaction.model.request.RequestStatus;
import interaction.model.request.RequestStatusUpdateRequest;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RequestClientFallbackFactory implements FallbackFactory<RequestFeignClient> {

    @Override
    public RequestFeignClient create(Throwable cause) {
        return new RequestFeignClient() {
            @Override
            public Integer getConfirmedRequestsCount(Integer eventId) {
                return 0;
            }

            @Override
            public Map<Integer, Integer> getConfirmedRequestsBatch(List<Integer> eventIds) {
                return eventIds.stream()
                        .collect(Collectors.toMap(id -> id, id -> 0));
            }

            @Override
            public List<ParticipationRequestDto> getRequestsByEventId(Integer eventId) {
                return Collections.emptyList();
            }

            @Override
            public List<ParticipationRequestDto> getRequestsByIds(List<Integer> requestIds) {
                return Collections.emptyList();
            }

            @Override
            public Integer countByEventIdAndStatus(Integer eventId, RequestStatus status) {
                return 0;
            }

            @Override
            public List<ParticipationRequestDto> updateRequestsStatusBatch(RequestStatusUpdateRequest batchRequest) {
                return Collections.emptyList();
            }

            @Override
            public List<ParticipationRequestDto> getRequestsByUserId(Integer userId) {
                return List.of();
            }

        };
    }
}
