package ru.practicum.stats.analyzer.service;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AnalyzerRunner implements CommandLineRunner {
    final UserActionProcessor userActionProcessor;
    final EventSimilarityProcessor eventSimilarityProcessor;

    @Override
    public void run(String... args) throws Exception {
        Thread userActionThread = new Thread(userActionProcessor, "UserActionHandlerThread");
        Thread eventSimilarityThread = new Thread(eventSimilarityProcessor, "EventSimilarityThread");

        userActionThread.setDaemon(true);
        eventSimilarityThread.setDaemon(true);

        userActionThread.start();
        eventSimilarityThread.start();
    }
}
