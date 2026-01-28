package ru.practicum.compilations.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PublicCompilationsParams {
    private Boolean pinned;
    private Integer from = 0;
    private Integer size = 10;
}