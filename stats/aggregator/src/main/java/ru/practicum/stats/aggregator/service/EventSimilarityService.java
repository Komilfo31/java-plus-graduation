package ru.practicum.stats.aggregator.service;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.List;
import java.util.Optional;

public interface EventSimilarityService {

    Optional<List<EventSimilarityAvro>> updateState(UserActionAvro userAction);

}
