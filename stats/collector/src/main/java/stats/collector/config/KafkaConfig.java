package stats.collector.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "collector.kafka")
public class KafkaConfig {

    private ProducerConfig producer;

    @Getter
    @Setter
    @ToString
    public static class ProducerConfig {
        private Properties properties = new Properties();
        private Map<String, String> topics = new HashMap<>();

        public ProducerConfig() {
        }

        public String getTopic(String key) {
            return topics.get(key);
        }

        public boolean hasTopic(String key) {
            return topics.containsKey(key);
        }

        public int getTopicCount() {
            return topics.size();
        }
    }

    public Properties getProducerProperties() {
        return producer != null ? producer.getProperties() : new Properties();
    }

    public Map<String, String> getProducerTopics() {
        return producer != null ? producer.getTopics() : Map.of();
    }

    public void validate() {
        if (producer == null) {
            throw new IllegalStateException("Producer configuration is required");
        }
        if (producer.getProperties() == null) {
            throw new IllegalStateException("Kafka properties are required");
        }
        if (producer.getTopics() == null || producer.getTopics().isEmpty()) {
            throw new IllegalStateException("At least one Kafka topic must be configured");
        }
    }
}
