package ru.practicum.stats.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.analyzer.config.KafkaConfig;
import ru.practicum.stats.analyzer.dal.service.UserActionService;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class UserActionProcessor implements Runnable, DisposableBean {
    private final KafkaConsumer<Long, SpecificRecordBase> consumer;
    private final UserActionService userActionService;
    private final String topic;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new ConcurrentHashMap<>();
    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    public UserActionProcessor(KafkaConfig config, UserActionService userActionService) {
        Objects.requireNonNull(config, "KafkaConfig must not be null");
        Objects.requireNonNull(userActionService, "UserActionService must not be null");

        this.consumer = new KafkaConsumer<>(config.getUserActionsConsumer().getProperties());
        this.userActionService = userActionService;
        this.topic = config.getTopic(KafkaConfig.TopicType.USER_ACTIONS);
    }

    @Override
    public void run() {
        log.info("UserActionProcessor started for topic: {}", topic);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered. Waking up consumer...");
            consumer.wakeup();
        }));
        try {
            consumer.subscribe(List.of(topic));
            while (running.get()) {
                ConsumerRecords<Long, SpecificRecordBase> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                    try {
                        UserActionAvro userActionAvro = handleRecord(record);
                        userActionService.saveUserAction(userActionAvro);
                        TopicPartition tp = new TopicPartition(record.topic(), record.partition());
                        currentOffsets.put(tp, new OffsetAndMetadata(record.offset() + 1));
                    } catch (Exception e) {
                        log.error("Error processing user action from topic={}, partition={}, offset={}, key={}",
                                record.topic(), record.partition(), record.offset(), record.key(), e);
                    }
                }

                if (!currentOffsets.isEmpty()) {
                    consumer.commitAsync(new HashMap<>(currentOffsets), (offsets, exception) -> {
                        if (exception != null) {
                            log.error("Failed to commit offsets: {}", offsets, exception);
                        } else {
                            log.debug("Successfully committed offsets: {}", offsets);
                        }
                    });
                }
            }
        } catch (WakeupException e) {
            if (running.get()) {
                log.warn("Unexpected WakeupException while processor is running", e);
            } else {
                log.info("Consumer shutdown detected.");
            }
        } catch (Exception e) {
            log.error("Unexpected error in UserActionProcessor", e);
        } finally {
            try {
                if (!currentOffsets.isEmpty()) {
                    consumer.commitSync(currentOffsets);
                    log.info("Committed {} offsets during shutdown", currentOffsets.size());
                }
            } catch (Exception e) {
                log.error("Failed to commit offsets during shutdown", e);
            } finally {
                log.info("Closing consumer");
                consumer.close(Duration.ofSeconds(5));
            }
        }
    }

    private UserActionAvro handleRecord(ConsumerRecord<Long, SpecificRecordBase> record) {
        log.debug("Received user-action record: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        if (!(record.value() instanceof UserActionAvro)) {
            throw new IllegalArgumentException(
                    String.format("Unexpected record type: %s. Expected: UserActionAvro",
                            record.value().getClass().getName()));
        }
        return (UserActionAvro) record.value();
    }

    @Override
    public void destroy() {
        log.info("UserActionProcessor: Destroy method called. Stopping consumer...");
        running.set(false);
        consumer.wakeup();
        log.info("UserActionProcessor shutdown initiated");
    }
}
