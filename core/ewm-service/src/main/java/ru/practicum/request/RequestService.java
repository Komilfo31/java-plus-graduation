package ru.practicum.request;

import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto createRequest(Integer userId, Integer eventId);

    // Метод для получения всех запросов пользователя
    List<ParticipationRequestDto> getUserRequests(Integer userId);

    // Метод для отмены запроса
    ParticipationRequestDto cancelRequest(Integer userId, Integer requestId);
}
