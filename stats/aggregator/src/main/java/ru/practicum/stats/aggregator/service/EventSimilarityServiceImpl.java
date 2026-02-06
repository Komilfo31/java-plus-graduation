package ru.practicum.stats.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityServiceImpl implements EventSimilarityService {
    private final Map<Long, Map<Long, Double>> userWeights = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventWeightSums = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSum = new ConcurrentHashMap<>();

    private static final Map<ActionTypeAvro, Double> ACTION_WEIGHTS = Map.of(
            ActionTypeAvro.VIEW, 0.4,
            ActionTypeAvro.REGISTER, 0.8,
            ActionTypeAvro.LIKE, 1.0
    );

    @Override
    public Optional<List<EventSimilarityAvro>> updateState(UserActionAvro userAction) {
        Objects.requireNonNull(userAction, "UserAction must not be null");

        log.debug("Получили userAction: userId={}, eventId={}, action={}",
                userAction.getUserId(), userAction.getEventId(), userAction.getActionType());

        Long userId = userAction.getUserId();
        Long eventId = userAction.getEventId();
        Double newWeight = convertTypeActionToWeight(userAction.getActionType());
        Instant timestamp = userAction.getTimestamp();

        validateIds(userId, eventId);

        Map<Long, Double> userWeight = userWeights.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        Double currentWeight = userWeight.get(userId);

        if (currentWeight != null && currentWeight >= newWeight) {
            log.debug("Вес не увеличился, пропускаем пересчет. Текущий: {}, Новый: {}", currentWeight, newWeight);
            return Optional.empty();
        }

        userWeight.put(userId, newWeight);
        updateEventWeightSum(eventId);
        List<EventSimilarityAvro> similarities = updateMinWeightsAndCalculateSimilarities(
                eventId, userId, newWeight, currentWeight, timestamp);

        return Optional.of(similarities);
    }

    private List<EventSimilarityAvro> updateMinWeightsAndCalculateSimilarities(
            Long eventA,
            Long userId,
            Double newWeightForEventA,
            Double oldWeightForEventA,
            Instant timestamp) {
        List<EventSimilarityAvro> eventSimilarities = new ArrayList<>();

        for (Long eventB : userWeights.keySet()) {
            if (eventB.equals(eventA)) {
                continue;
            }

            Long minId = Math.min(eventB, eventA);
            Long maxId = Math.max(eventB, eventA);
            Map<Long, Double> usersWeightForEventB = userWeights.get(eventB);

            if (usersWeightForEventB == null || !usersWeightForEventB.containsKey(userId)) {
                continue;
            }

            Double userWeightForEventB = usersWeightForEventB.get(userId);
            Double deltaMin = calculateDelta(userWeightForEventB, oldWeightForEventA, newWeightForEventA);

            if (deltaMin != 0) {
                minWeightsSum.computeIfAbsent(minId, k -> new ConcurrentHashMap<>()).merge(maxId, deltaMin, Double::sum);
            }

            Double score = calculateSimilarity(minId, maxId);

            if (score == null) {
                continue;
            }

            EventSimilarityAvro eventsSimilarityAvro = createEventSimilarity(minId, maxId, score, timestamp);
            eventSimilarities.add(eventsSimilarityAvro);
            log.trace("Avro-сообщение: {}", eventsSimilarityAvro);
        }

        return eventSimilarities;
    }

    private EventSimilarityAvro createEventSimilarity(Long minId, Long maxId, Double score, Instant timestamp) {
        return EventSimilarityAvro.newBuilder()
                .setEventA(minId)
                .setEventB(maxId)
                .setScore(score)
                .setTimestamp(timestamp)
                .build();
    }

    private Double convertTypeActionToWeight(ActionTypeAvro actionType) {
        Double weight = ACTION_WEIGHTS.get(actionType);
        if (weight == null) {
            log.warn("Unknown action type: {}, using default weight 0.0", actionType);
            return 0.0;
        }
        return weight;
    }

    private Double calculateSimilarity(Long minId, Long maxId) {
        Map<Long, Double> innerMap = minWeightsSum.get(minId);
        if (innerMap == null) {
            log.debug("Неполные данные для расчета сходства пары ({}, {})", minId, maxId);
            return null;
        }

        Double sumMin = innerMap.get(maxId);
        Double sumA = eventWeightSums.get(minId);
        Double sumB = eventWeightSums.get(maxId);

        if (sumMin == null || sumA == null || sumB == null) {
            log.debug("Неполные данные для расчета сходства пары ({}, {})", minId, maxId);
            return null;
        }

        if (sumA == 0 || sumB == 0) {
            return 0.0;
        }

        Double similarity = sumMin / Math.sqrt(sumA * sumB);
        log.debug("Рассчитано сходство для пары ({}, {}): sumMin={}, sumA={}, sumB={}, similarity={}",
                minId, maxId, sumMin, sumA, sumB, similarity);
        return similarity;
    }

    private Double calculateDelta(Double userWeightForEventB, Double oldWeightForEventA, Double newWeightForEventA) {
        double oldWEA = (oldWeightForEventA != null) ? oldWeightForEventA : 0.0;
        Double oldMin = Math.min(oldWEA, userWeightForEventB);
        Double newMin = Math.min(newWeightForEventA, userWeightForEventB);
        return newMin - oldMin;
    }

    private void updateEventWeightSum(Long eventId) {
        Map<Long, Double> users = userWeights.get(eventId);
        if (users == null || users.isEmpty()) {
            log.warn("No users found for event {}", eventId);
            return;
        }

        Double sumWeight = users.values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        Double sumWeightOld = eventWeightSums.put(eventId, sumWeight);

        if (sumWeightOld == null) {
            log.debug("Добавлена сумма весов для мероприятия {}: {}", eventId, sumWeight);
        } else {
            log.debug("Обновлена сумма весов для мероприятия {}: с {} на {}", eventId, sumWeightOld, sumWeight);
        }
    }

    private void validateIds(Long userId, Long eventId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user id: " + userId);
        }
        if (eventId == null || eventId <= 0) {
            throw new IllegalArgumentException("Invalid event id: " + eventId);
        }
    }
}
