package ru.practicum.stats.analyzer.dal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;
import ru.practicum.stats.analyzer.dal.model.ActionType;
import ru.practicum.stats.analyzer.dal.model.EventSimilarity;
import ru.practicum.stats.analyzer.dal.model.UserAction;
import ru.practicum.stats.analyzer.dal.repository.EventSimilarityRepository;
import ru.practicum.stats.analyzer.dal.repository.UserActionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {
    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    private final EventSimilarityRepository eventSimilarityRepository;
    private final UserActionRepository userActionRepository;

    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        Objects.requireNonNull(request, "Request cannot be null");
        log.debug("Рекомендации для userId={}", request.getUserId());

        Long userId = request.getUserId();
        int maxResults = Math.max(1, request.getMaxResults());

        PageRequest pageRequest = PageRequest.of(0, maxResults, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<UserAction> interactions = userActionRepository.findAllByUserId(userId, pageRequest);

        if (interactions.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> recentEvents = interactions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> allUserEvents = interactions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        List<EventSimilarity> similarities = new ArrayList<>();
        for (Long eventId : recentEvents) {
            similarities.addAll(eventSimilarityRepository.findAllByEventAOrEventB(eventId, eventId));
        }

        return similarities.stream()
                .filter(sim -> !(allUserEvents.contains(sim.getEventA()) && allUserEvents.contains(sim.getEventB())))
                .sorted(Comparator.comparing(EventSimilarity::getScore).reversed())
                .limit(maxResults)
                .map(sim -> {
                    Long recommendedEventId = allUserEvents.contains(sim.getEventA()) ? sim.getEventB() : sim.getEventA();
                    return createRecommendedEvent(recommendedEventId, sim.getScore());
                })
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        Objects.requireNonNull(request, "Request cannot be null");
        log.debug("Похожие события для eventId={}", request.getEventId());

        Long userId = request.getUserId();
        Long eventId = request.getEventId();
        int limit = Math.max(1, request.getMaxResults());

        Set<Long> interacted = userActionRepository.findAllByUserId(userId).stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        List<EventSimilarity> similar = eventSimilarityRepository.findAllByEventAOrEventB(eventId, eventId);

        return similar.stream()
                .filter(sim -> !interacted.contains(sim.getEventA()) || !interacted.contains(sim.getEventB()))
                .sorted(Comparator.comparing(EventSimilarity::getScore).reversed())
                .limit(limit)
                .map(sim -> {
                    Long recommendedEventId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                    return createRecommendedEvent(recommendedEventId, sim.getScore());
                })
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        Objects.requireNonNull(request, "Request cannot be null");

        Set<Long> eventIds = new HashSet<>(request.getEventIdList());

        if (eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Double> eventScore = userActionRepository.findAllByEventIdIn(eventIds).stream()
                .collect(Collectors.groupingBy(UserAction::getEventId,
                        Collectors.summingDouble(action -> convertTypeActionToWeight(action.getActionType()))));

        return eventScore.entrySet().stream()
                .map(el -> createRecommendedEvent(el.getKey(), el.getValue()))
                .toList();
    }

    private RecommendedEventProto createRecommendedEvent(Long eventId, Double score) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(score)
                .build();
    }


    private Double convertTypeActionToWeight(ActionType actionType) {
        return switch (actionType) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }
}