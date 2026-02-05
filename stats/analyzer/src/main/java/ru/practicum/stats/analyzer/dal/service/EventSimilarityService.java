package ru.practicum.stats.analyzer.dal.service;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public interface EventSimilarityService {
    void saveEventSimilarity(EventSimilarityAvro eventSimilarityAvro);
}
