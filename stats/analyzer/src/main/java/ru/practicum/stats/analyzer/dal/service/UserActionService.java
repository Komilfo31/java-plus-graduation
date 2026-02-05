package ru.practicum.stats.analyzer.dal.service;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface UserActionService {
    void saveUserAction(UserActionAvro userActionAvro);
}
