package ru.practicum.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@EqualsAndHashCode(of = {"app", "uri", "ip"})
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppUri {
    private String app;
    private String uri;
    private String ip;
}
