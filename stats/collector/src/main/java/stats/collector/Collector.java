package stats.collector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import stats.collector.config.KafkaConfig;

@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan
@EnableConfigurationProperties(KafkaConfig.class)
public class Collector {

    public static void main(String[] args) {
        SpringApplication.run(Collector.class, args);
    }
}
