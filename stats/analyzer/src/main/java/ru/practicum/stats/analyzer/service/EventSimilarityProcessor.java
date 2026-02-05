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
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.analyzer.config.KafkaConfig;
import ru.practicum.stats.analyzer.dal.service.EventSimilarityService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class EventSimilarityProcessor implements Runnable, DisposableBean {
    private final KafkaConsumer<String, SpecificRecordBase> consumer;
    private final EventSimilarityService eventSimilarityService;
    private final String topic;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new ConcurrentHashMap<>();
    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    public EventSimilarityProcessor(KafkaConfig config, EventSimilarityService eventSimilarityService) {
        Objects.requireNonNull(config, "KafkaConfig must not be null");
        Objects.requireNonNull(eventSimilarityService, "EventSimilarityService must not be null");

        this.consumer = new KafkaConsumer<>(config.getEventSimilarityConsumer().getProperties());
        this.eventSimilarityService = eventSimilarityService;
        this.topic = config.getTopic(KafkaConfig.TopicType.EVENTS_SIMILARITY);
    }

    public void run() {
        log.info("EventSimilarityProcessor started for topic: {}", topic);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered. Waking up consumer...");
            consumer.wakeup();
        }));
        try {
            consumer.subscribe(List.of(topic));
            while (running.get()) {
                ConsumerRecords<String, SpecificRecordBase> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    try {
                        EventSimilarityAvro eventSimilarityAvro = handleRecord(record);
                        eventSimilarityService.saveEventSimilarity(eventSimilarityAvro);
                        TopicPartition tp = new TopicPartition(record.topic(), record.partition());
                        currentOffsets.put(tp, new OffsetAndMetadata(record.offset() + 1));
                    } catch (Exception e) {
                        log.error("Error processing record from topic={}, partition={}, offset={}",
                                record.topic(), record.partition(), record.offset(), e);
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
            log.error("Error in EventSimilarityProcessor", e);
            throw new RuntimeException("EventSimilarityProcessor failed", e);
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

    private EventSimilarityAvro handleRecord(ConsumerRecord<String, SpecificRecordBase> record) {
        log.debug("Received event-similarity record: topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());

        if (!(record.value() instanceof EventSimilarityAvro)) {
            throw new IllegalArgumentException(
                    String.format("Unexpected record type: %s. Expected: EventSimilarityAvro",
                            record.value().getClass().getName()));
        }
        return (EventSimilarityAvro) record.value();
    }

    @Override
    public void destroy() {
        log.info("EventSimilarityProcessor: Destroy method called. Stopping consumer...");
        running.set(false);
        consumer.wakeup();
        log.info("EventSimilarityProcessor shutdown initiated");
    }
}
