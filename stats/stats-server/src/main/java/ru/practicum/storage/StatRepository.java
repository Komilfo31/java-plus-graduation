package ru.practicum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.model.Stat;
import ru.practicum.model.ViewStats;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface StatRepository extends JpaRepository<Stat, Integer> {
    Collection<Stat> findByCreatedBetween(LocalDateTime start, LocalDateTime end);

    Collection<Stat> findByCreatedBetweenAndUriIn(LocalDateTime start, LocalDateTime end, Collection<String> uris);

    @Query("SELECT new ru.practicum.model.ViewStats(s.app, s.uri, CAST(COUNT(s) AS integer)) " +
            "FROM Stat s " +
            "WHERE s.created BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR s.uri IN :uris) " +
            "GROUP BY s.app, s.uri " +
            "ORDER BY COUNT(s) DESC")
    List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("SELECT new ru.practicum.model.ViewStats(s.app, s.uri, CAST(COUNT(DISTINCT s.ip) AS integer)) " +
            "FROM Stat s " +
            "WHERE s.created BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR s.uri IN :uris) " +
            "GROUP BY s.app, s.uri " +
            "ORDER BY COUNT(DISTINCT s.ip) DESC")
    List<ViewStats> getStatsUnique(LocalDateTime start, LocalDateTime end, List<String> uris);
}
