package ru.practicum.stats.analyzer.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.analyzer.dal.model.EventSimilarity;

@Component
public class EventSimilarityMapper {
    public EventSimilarity toEventSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        return EventSimilarity.builder()
                .score(eventSimilarityAvro.getScore())
                .eventA(eventSimilarityAvro.getEventA())
                .eventB(eventSimilarityAvro.getEventB())
                .timestamp(eventSimilarityAvro.getTimestamp())
                .build();
    }
}
