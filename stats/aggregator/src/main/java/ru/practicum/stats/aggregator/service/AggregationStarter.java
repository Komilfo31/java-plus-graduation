package ru.practicum.stats.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.aggregator.config.KafkaConfig;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class AggregationStarter implements DisposableBean {

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new ConcurrentHashMap<>();
    private final EnumMap<KafkaConfig.TopicType, String> topics = new EnumMap<>(KafkaConfig.TopicType.class);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final KafkaConsumer<Long, SpecificRecordBase> consumer;
    private final KafkaProducer<String, SpecificRecordBase> producer;
    private final EventSimilarityServiceImpl eventSimilarityService;
    private final Duration consumeAttemptTimeout;



    public AggregationStarter(KafkaConfig kafkaConfig,
                              EventSimilarityServiceImpl eventSimilarityService) {
        Objects.requireNonNull(kafkaConfig, "KafkaConfig must not be null");
        Objects.requireNonNull(eventSimilarityService, "EventSimilarityService must not be null");

        this.consumer = new KafkaConsumer<>(kafkaConfig.getConsumer().getProperties());
        this.producer = new KafkaProducer<>(kafkaConfig.getProducer().getProperties());
        this.eventSimilarityService = eventSimilarityService;
        this.consumeAttemptTimeout = Duration.ofMillis(kafkaConfig.getConsumeAttemptTimeoutMillis());
        for (KafkaConfig.TopicType type : KafkaConfig.TopicType.values()) {
            topics.put(type, kafkaConfig.getTopic(type));
        }
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.warn("AggregationStarter is already running");
            return;
        }

        try {
            consumer.subscribe(List.of(topics.get(KafkaConfig.TopicType.USER_ACTIONS)));

            while (running.get()) {
                ConsumerRecords<Long, SpecificRecordBase> records = consumer.poll(consumeAttemptTimeout);

                for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                    UserActionAvro userAction = handleRecord(record);
                    Optional<List<EventSimilarityAvro>> eventSimilarities = eventSimilarityService.updateState(userAction);
                    eventSimilarities.ifPresent(this::sendEventSimilarities);

                    TopicPartition tp = new TopicPartition(record.topic(), record.partition());
                    currentOffsets.put(tp, new OffsetAndMetadata(record.offset() + 1));
                }

                if (!currentOffsets.isEmpty()) {
                    consumer.commitAsync();
                }
            }

        } catch (WakeupException ignored) {
            log.info("Consumer shutdown detected.");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            shutdown();
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Stopping aggregation process...");
            consumer.wakeup();
        }
    }

    @Override
    public void destroy() {
        stop();
    }

    private void shutdown() {
        try {
            producer.flush();
            if (!currentOffsets.isEmpty()) {
                consumer.commitSync(currentOffsets);
            }
        } finally {
            log.info("Закрываем консьюмер");
            consumer.close();
            log.info("Закрываем продюсер");
            producer.close();
        }
    }

    private UserActionAvro handleRecord(ConsumerRecord<Long, SpecificRecordBase> record) {
        log.debug("Received record: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        if (!(record.value() instanceof UserActionAvro)) {
            throw new IllegalArgumentException(
                    String.format("Unexpected record type: %s. Expected: UserActionAvro",
                            record.value().getClass().getName()));
        }
        return (UserActionAvro) record.value();
    }

    private void sendEventSimilarities(List<EventSimilarityAvro> eventSimilarities) {
        String topic = topics.get(KafkaConfig.TopicType.EVENTS_SIMILARITY);

        for (EventSimilarityAvro eventSimilarity : eventSimilarities) {
            ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(topic, eventSimilarity);
            log.debug("Отправляем record: {}", record);
            producer.send(record);
        }

        producer.flush();
        log.info("Отправили {} event similarities в топик {}", eventSimilarities.size(), topic);
    }
}
