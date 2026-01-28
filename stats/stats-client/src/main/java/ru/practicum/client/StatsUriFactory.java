package ru.practicum.client;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class StatsUriFactory {

    private final RetryTemplate retryTemplate;
    private final StatsDiscoveryService discoveryService;

    public URI buildUri(String path) {
        ServiceInstance instance = retryTemplate.execute(ctx -> discoveryService.getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }
}