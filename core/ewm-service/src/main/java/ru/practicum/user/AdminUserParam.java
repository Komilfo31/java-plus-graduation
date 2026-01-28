package ru.practicum.user;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.List;

@Data
public class AdminUserParam {
    private List<Integer> ids;
    @PositiveOrZero
    private Integer from = 0;
    @Positive
    private Integer size = 10;
}
