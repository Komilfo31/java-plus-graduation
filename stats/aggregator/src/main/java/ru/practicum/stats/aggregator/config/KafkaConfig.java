package ru.practicum.stats.aggregator.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "aggregator.kafka")
public class KafkaConfig {

    private Producer producer;
    ;

    private Consumer consumer;
    ;

    @NotEmpty(message = "Kafka topics must not be empty")
    private Map<String, String> topics = Collections.emptyMap();

    @Positive(message = "Consume attempt timeout must be positive")
    private long consumeAttemptTimeoutMillis = 5000L;

    @Getter
    @Setter
    public static class Consumer {
        @NotNull(message = "Consumer properties must not be null")
        private Properties properties = new Properties();
    }

    @Getter
    @Setter
    public static class Producer {
        @NotNull(message = "Producer properties must not be null")
        private Properties properties = new Properties();
    }

    public enum TopicType {
        USER_ACTIONS("user-actions"),
        EVENTS_SIMILARITY("events-similarity");

        private final String topicName;

        TopicType(String topicName) {
            this.topicName = topicName;
        }

        public String getTopicName() {
            return topicName;
        }
    }

    public String getTopic(TopicType type) {
        String topic = topics.get(type.getTopicName());
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Unknown topic type: " + type +
                    ". Available topics: " + topics.keySet());
        }
        return topic;
    }
}
