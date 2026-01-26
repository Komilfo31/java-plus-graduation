package ru.practicum.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Integer> {

    @Query("select count(r) from Request r where r.event.id = :eventId and r.status = 'CONFIRMED'")
    Integer countConfirmedByEventId(@Param("eventId") Integer eventId);

    @Query("select r.event.id as eventId, count(r) as cnt " +
            "from Request r " +
            "where r.event.id in :eventIds and r.status = 'CONFIRMED' " +
            "group by r.event.id")
    List<ConfirmedCount> countConfirmedForEventIds(@Param("eventIds") List<Integer> eventIds);

    // Находит все заявки для определенного пользователя
    List<Request> findByRequesterId(Integer requesterId);

    // Находит конкретную заявку для определенного пользователя и заявки
    Optional<Request> findByRequesterIdAndId(Integer requesterId, Integer requestId);

    // Находит все заявки на участие в событии
    List<Request> findByEventId(Integer eventId);

    // Подсчитывает количество подтвержденных заявок на участие в событии
    Integer countByEventIdAndStatus(Integer eventId, RequestStatus status);

    // Проверка существования запроса
    boolean existsByRequesterIdAndEventId(Integer requesterId, Integer eventId);

    List<Request> findAllByIdIn(List<Integer> ids);

    List<Request> findByEventIdAndStatus(Integer eventId, RequestStatus status);

}