package ru.practicum.request.service;


import interaction.model.request.ParticipationRequestDto;
import interaction.model.request.RequestStatusUpdateRequest;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto createRequest(Integer userId, Integer eventId);

    // Метод для получения всех запросов пользователя
    List<ParticipationRequestDto> getUserRequests(Integer userId);

    // Метод для отмены запроса
    ParticipationRequestDto cancelRequest(Integer userId, Integer requestId);

    List<ParticipationRequestDto> updateRequestsStatusBatch(
            RequestStatusUpdateRequest batchRequest);
}
