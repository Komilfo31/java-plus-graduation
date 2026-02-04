package stats.collector.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import stats.collector.config.KafkaConfig;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class KafkaUserActionProducer implements AutoCloseable {

    protected final KafkaProducer<Long, SpecificRecordBase> producer;
    private final String topic;

    public KafkaUserActionProducer(KafkaConfig kafkaConfig) {
        this.topic = kafkaConfig.getProducer().getTopics().get("user-actions");
        this.producer = new KafkaProducer<>(kafkaConfig.getProducer().getProperties());
    }

    public void send(SpecificRecordBase userAction, Instant timeStamp) {

        Long key = ((UserActionAvro) userAction).getUserId();
        ProducerRecord<Long, SpecificRecordBase> record = new ProducerRecord<>(
                topic,
                null,
                timeStamp.toEpochMilli(),
                key,
                userAction
        );

        log.trace("Сохраняю действие пользователя {} в топик {}",
                key, topic);

        log.info("<== Json: {}", userAction);
        producer.send(record);
    }

    public void close() {
        producer.flush();
        producer.close(Duration.ofSeconds(10));
    }
}
