package stats.collector.service;

import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionProto;
import stats.collector.mapper.UserActionMapper;

import java.time.Instant;

@Service
public class UserActionService {
    private final UserActionMapper mapper;
    private final KafkaUserActionProducer kafkaProducer;

    public UserActionService(UserActionMapper mapper, KafkaUserActionProducer kafkaProducer) {
        this.mapper = mapper;
        this.kafkaProducer = kafkaProducer;
    }

    public void processUserAction(UserActionProto userActionProto) {
        UserActionAvro userActionAvro = mapper.toAvro(userActionProto);
        Instant instant = Instant.ofEpochSecond(
                userActionProto.getTimestamp().getSeconds(),
                userActionProto.getTimestamp().getNanos()
        );
        kafkaProducer.send(userActionAvro, instant);
    }
}
