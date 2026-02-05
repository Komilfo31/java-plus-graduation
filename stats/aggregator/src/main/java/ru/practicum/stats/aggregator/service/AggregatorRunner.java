package ru.practicum.stats.aggregator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AggregatorRunner implements CommandLineRunner {
    final AggregationStarter starter;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Aggregator is running...");
        starter.start();
    }
}
