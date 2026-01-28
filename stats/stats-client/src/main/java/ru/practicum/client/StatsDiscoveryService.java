package ru.practicum.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import ru.practicum.exception.StatsClientException;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatsDiscoveryService {

    private final DiscoveryClient discoveryClient;
    private static final String STATS_SERVER_ID = "stats-server";

    public ServiceInstance getInstance() {
        try {
            List<ServiceInstance> instances = discoveryClient
                    .getInstances(STATS_SERVER_ID);

            if (instances == null || instances.isEmpty()) {
                throw new StatsClientException(
                        "Сервис статистики не найден в Service Discovery");
            }

            ServiceInstance instance = instances.getFirst();
            log.debug("Найден сервис статистики: {}", instance.getUri());
            return instance;

        } catch (StatsClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка Service Discovery для {}", STATS_SERVER_ID, e);
            throw new StatsClientException(
                    "Ошибка обнаружения stats-server через Service Discovery", e);
        }
    }
}
