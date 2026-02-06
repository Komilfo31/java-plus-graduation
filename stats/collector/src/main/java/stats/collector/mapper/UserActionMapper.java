package stats.collector.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Component
public class UserActionMapper {
    public UserActionAvro toAvro(UserActionProto proto) {
        Instant instant = Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        );
        String protoActionTypeName = proto.getActionType().name();
        String avroActionTypeName = protoActionTypeName.replace("ACTION_", "");
        ActionTypeAvro actionTypeAvro = ActionTypeAvro.valueOf(avroActionTypeName);

        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setTimestamp(instant)
                .setActionType(actionTypeAvro)
                .build();
    }
}
