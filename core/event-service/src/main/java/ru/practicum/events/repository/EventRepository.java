package ru.practicum.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.category.model.Category;
import ru.practicum.events.model.Event;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Integer>, JpaSpecificationExecutor<Event> {

    @Query("SELECT e FROM Event e WHERE e.id IN :eventIds")
    Set<Event> findAllById(@Param("eventIds") List<Integer> eventIds);

    // Сохраняем старое имя, но используем правильное поле
    @Query("SELECT e FROM Event e WHERE e.initiatorId = ?1 ORDER BY e.id ASC")
    List<Event> findByInitiatorOrderByIdAsc(Integer userId);

    // Сохраняем старое имя
    @Query("SELECT e FROM Event e WHERE e.initiatorId = ?1 AND e.id = ?2")
    Optional<Event> findByInitiatorAndId(Integer userId, Integer eventId);

    Collection<Event> findByCategory(Category category);

    Integer countByCategoryId(Integer categoryId);

    List<Event> findAllByIdIn(List<Integer> ids);
}
