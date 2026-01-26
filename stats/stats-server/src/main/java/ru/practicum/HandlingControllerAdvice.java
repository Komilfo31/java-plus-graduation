package ru.practicum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;


@RestControllerAdvice
public class HandlingControllerAdvice {
    private static final Logger log = LoggerFactory.getLogger(HandlingControllerAdvice.class);

    /**
     * Обрабатывает исключения, связанные с неверными аргументами, например,
     * когда дата начала позже даты окончания в запросе статистики.
     */
    @ExceptionHandler(value = {IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgumentException(final IllegalArgumentException e) {
        log.error("400 BAD REQUEST: Неверный аргумент в запросе: {}", e.getMessage());
        return Map.of("error", "BAD_REQUEST", "message", e.getMessage());
    }

    @ExceptionHandler(value = {MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMissingParamsException(final MissingServletRequestParameterException e) {
        log.error("400 BAD REQUEST: Отсутствует обязательный параметр: {}", e.getMessage());
        return Map.of("error", "BAD_REQUEST", "message", e.getMessage());
    }

    /**
     * Обрабатывает все прочие необработанные исключения.
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleThrowable(final Throwable e) {
        log.error("500 INTERNAL SERVER ERROR: Необработанное исключение: {}", e.getMessage(), e);
        return Map.of("error", "INTERNAL_SERVER_ERROR", "message", "Произошла непредвиденная ошибка на сервере.");
    }
}
