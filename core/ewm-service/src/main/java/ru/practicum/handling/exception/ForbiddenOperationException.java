package ru.practicum.handling.exception;

import org.slf4j.Logger;

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }

    public ForbiddenOperationException(String message, Logger logger) {
        this(message);
        logger.error(message);
    }
}
