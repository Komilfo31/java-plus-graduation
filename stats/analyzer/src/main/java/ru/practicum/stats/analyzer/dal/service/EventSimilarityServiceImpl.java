package ru.practicum.stats.analyzer.dal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.analyzer.dal.model.EventSimilarity;
import ru.practicum.stats.analyzer.dal.repository.EventSimilarityRepository;
import ru.practicum.stats.analyzer.mapper.EventSimilarityMapper;

@Service
@RequiredArgsConstructor
public class EventSimilarityServiceImpl implements EventSimilarityService {
    private final EventSimilarityRepository repository;
    private final EventSimilarityMapper mapper;

    @Override
    public void saveEventSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        EventSimilarity event = mapper.toEventSimilarity(eventSimilarityAvro);
        EventSimilarity savedEvent = repository.save(event);
    }
}
