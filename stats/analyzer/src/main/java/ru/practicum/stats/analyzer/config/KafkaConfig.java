package ru.practicum.stats.analyzer.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
@ConfigurationProperties(prefix = "analyzer.kafka")
public class KafkaConfig {

    @NotNull(message = "User actions consumer properties must not be null")
    private ConsumerProperties userActionsConsumer = new ConsumerProperties();

    @NotNull(message = "Event similarity consumer properties must not be null")
    private ConsumerProperties eventSimilarityConsumer = new ConsumerProperties();

    @NotEmpty(message = "Kafka topics must not be empty")
    private Map<String, String> topics = Collections.emptyMap();

    @Getter
    @Setter
    public static class ConsumerProperties {
        @NotNull(message = "Consumer properties must not be null")
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
