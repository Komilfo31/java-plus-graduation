package ru.practicum.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.exception.StatsClientException;
import ru.practicum.statistics.dto.EndpointHitDto;
import ru.practicum.statistics.dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class StatClient {

    private final StatsUriFactory uriFactory;

    private static final DateTimeFormatter API_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient = RestClient.builder()
            .defaultStatusHandler(
                    HttpStatusCode::isError,
                    (request, response) -> {
                        String errorMessage = String.format(
                                "Ошибка сервиса статистики [%s %s]: %d %s",
                                request.getMethod(),
                                request.getURI(),
                                response.getStatusCode().value(),
                                response.getStatusText()
                        );
                        log.error(errorMessage);
                        throw new StatsClientException(errorMessage);
                    })
            .build();


    public void hit(EndpointHitDto endpointHitDto) {
        URI uri = uriFactory.buildUri("/hit");

        restClient.post()
                .uri(uri)
                .body(endpointHitDto)
                .retrieve()
                .toBodilessEntity();
    }

    public List<ViewStatsDto> getStats(
            LocalDateTime start,
            LocalDateTime end,
            List<String> uris,
            boolean unique
    ) {
        validateGetStatsParam(start, end);

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        URI uri = UriComponentsBuilder
                .fromUri(uriFactory.buildUri("/stats"))
                .queryParam("start", start.format(formatter))
                .queryParam("end", end.format(formatter))
                .queryParam("unique", unique)
                .queryParamIfPresent(
                        "uris",
                        uris == null || uris.isEmpty()
                                ? Optional.empty()
                                : Optional.of(uris)
                )
                .build(false)
                .toUri();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private void validateGetStatsParam(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            throw new IllegalArgumentException("Дата начала не может быть нулевой");
        }
        if (end == null) {
            throw new IllegalArgumentException("Дата окончания не может быть нулевой");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Дата окончания не может быть раньше даты начала");
        }
    }
}