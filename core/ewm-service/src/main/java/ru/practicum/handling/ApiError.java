package ru.practicum.handling;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


@AllArgsConstructor
@Getter
public class ApiError {

    private List<String> errors;
    private String message;
    private String reason;
    private String status;
    private String timestamp;

}
