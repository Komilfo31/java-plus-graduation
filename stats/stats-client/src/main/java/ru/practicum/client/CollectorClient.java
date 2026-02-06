package ru.practicum.client;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Component
public class CollectorClient {
    @GrpcClient("collector")
    private static UserActionControllerGrpc.UserActionControllerBlockingStub actionClient;

    public void saveView(long userId, long eventId) {
        saveUserInteraction(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    public void saveRegister(long userId, long eventId) {
        saveUserInteraction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    public void saveLike(long userId, long eventId) {
        saveUserInteraction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    private void saveUserInteraction(long userId, long eventId, ActionTypeProto actionType) {
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();

        UserActionProto userActionProto = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(timestamp)
                .build();

        actionClient.collectUserAction(userActionProto);
    }
}
