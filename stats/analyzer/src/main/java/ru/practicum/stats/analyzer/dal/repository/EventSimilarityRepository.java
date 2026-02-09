package ru.practicum.stats.analyzer.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.stats.analyzer.dal.model.EventSimilarity;

import java.util.List;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {
    List<EventSimilarity> findAllByEventAOrEventB(Long eventA, Long eventB);
}
