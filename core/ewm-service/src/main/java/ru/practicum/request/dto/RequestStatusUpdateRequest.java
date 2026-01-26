package ru.practicum.request.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.request.RequestStatus;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestStatusUpdateRequest {
    @NotEmpty
    private List<@Positive Integer> requestIds;

    @NotNull
    private RequestStatus status; // enum: CONFIRMED или REJECTED (или PENDING etc)
}