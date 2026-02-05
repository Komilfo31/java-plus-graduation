package ru.practicum.stats.analyzer.dal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.analyzer.dal.model.ActionType;
import ru.practicum.stats.analyzer.dal.model.UserAction;
import ru.practicum.stats.analyzer.dal.repository.UserActionRepository;
import ru.practicum.stats.analyzer.mapper.UserActionMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService{
    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    private final UserActionRepository repository;
    private final UserActionMapper mapper;

    @Override
    public void saveUserAction(UserActionAvro newUserAction) {

        log.info("Сохранение действия: userId={}, eventId={}, action={}",
                newUserAction.getUserId(),
                newUserAction.getEventId(),
                newUserAction.getActionType());

        UserAction existing = repository.findByUserIdAndEventId(newUserAction.getUserId(), newUserAction.getEventId());

        UserAction incoming = mapper.toUserAction(newUserAction);

        if (existing == null) {
            repository.save(incoming);
            return;
        }

        Double oldWeight = convertTypeActionToWeight(existing.getActionType());
        Double newWeight = convertTypeActionToWeight(incoming.getActionType());

        if (newWeight >= oldWeight) {
            existing.setActionType(incoming.getActionType());
            existing.setTimestamp(incoming.getTimestamp());
            repository.save(existing);
            log.info("Сохранили в бд действие с большим весом: {}", existing);
        }

        log.debug("Вес недостаточен для обновления: oldWeight={}, newWeight={}",
                oldWeight, newWeight);
    }

    private Double convertTypeActionToWeight(ActionType actionType) {
        return switch (actionType) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }
}
