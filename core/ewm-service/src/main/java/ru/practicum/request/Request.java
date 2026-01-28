package ru.practicum.request;


import jakarta.persistence.*;
import lombok.*;
import ru.practicum.events.model.Event;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Пользователь, отправивший запрос
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    // Событие, на участие в котором сделан запрос
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // Дата и время создания запроса
    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    // Статус заявки: PENDING, CONFIRMED, REJECTED, CANCELED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;
}