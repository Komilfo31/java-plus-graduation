package ru.practicum.mapper;

import ru.practicum.model.ViewStats;
import ru.practicum.statistics.dto.ViewStatsDto;

public class StatisticsMapper {
    public static ViewStatsDto toViewStatsDto(ViewStats viewStats) {
        return new ViewStatsDto(viewStats.getApp(), viewStats.getUri(), viewStats.getHits());
    }
}
