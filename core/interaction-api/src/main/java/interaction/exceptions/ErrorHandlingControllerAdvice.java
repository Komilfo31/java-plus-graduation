package interaction.exceptions;

import interaction.exceptions.exception.ConflictException;
import interaction.exceptions.exception.DuplicatedDataException;
import interaction.exceptions.exception.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static interaction.constants.Constants.PATTERN_FORMATE_DATE;

@RestControllerAdvice
public class ErrorHandlingControllerAdvice {
    private static final Logger log = LoggerFactory.getLogger(ErrorHandlingControllerAdvice.class);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern(PATTERN_FORMATE_DATE);

    private ApiError api(HttpStatus status, String reason, String message, List<String> errors) {
        return new ApiError(
                errors == null ? List.of() : errors,
                message,
                reason,
                status.name(),
                LocalDateTime.now().format(FMT)
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onConstraintValidationException(ConstraintViolationException e) {
        log.warn("400 {}", e.getMessage());
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(fe -> "Field: %s. Error: %s. Value: %s"
                        .formatted(fe.getPropertyPath().toString(), fe.getMessage(), fe.getInvalidValue()))
                .orElse("Validation failed");
        return api(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                message,
                null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // проверяем, наличие ошибок при валидации значений в полях объекта
        log.warn("400 {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> "Field: %s. Error: %s. Value: %s"
                        .formatted(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                .orElse("Validation failed");
        return api(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                message,
                null);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError onNotFoundException(NotFoundException e) {
        log.warn("404 {}", e.getMessage());

        return api(HttpStatus.NOT_FOUND,
                "The required object was not found.",
                e.getMessage(),
                null);
    }

    @ExceptionHandler(DuplicatedDataException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError onDuplicatedDataException(DuplicatedDataException e) {
        log.warn("409 {}", e.getMessage());

        return api(HttpStatus.CONFLICT,
                "Duplication of an object.",
                e.getMessage(),
                null);
    }


    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onValidationException(ValidationException e) {
        log.warn("400 {}", e.getMessage());

        return api(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                e.getMessage(),
                null);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError onConflictException(ConflictException e) {
        log.warn("409 {}", e.getMessage());

        return api(HttpStatus.CONFLICT,
                "For the requested operation the conditions are not met.",
                e.getMessage(),
                null);
    }
}
