package ru.practicum.shareit.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlerTest {

    private final ErrorHandler errorHandler = new ErrorHandler();

    @Test
    void handlesNotFound() {
        assertThat(errorHandler.handleNotFound(new NotFoundException("нет такого id")).getError())
                .isEqualTo("нет такого id");
    }

    @Test
    void handlesValidation() {
        assertThat(errorHandler.handleValidation(new ValidationException("некорректные данные")).getError())
                .isEqualTo("некорректные данные");
    }

    @Test
    void handlesForbidden() {
        assertThat(errorHandler.handleForbidden(new ForbiddenException("нет доступа")).getError())
                .isEqualTo("нет доступа");
    }

    @Test
    void handlesConflict() {
        assertThat(errorHandler.handleConflict(new ConflictException("email занят")).getError())
                .isEqualTo("email занят");
    }

    @Test
    void handlesUnexpectedThrowable() {
        assertThat(errorHandler.handleThrowable(new RuntimeException("что-то сломалось")).getError())
                .isEqualTo("что-то сломалось");
    }

    @Test
    void errorResponseSupportsNoArgsConstructorAndSetter() {
        ErrorResponse response = new ErrorResponse();
        response.setError("ошибка");

        assertThat(response.getError()).isEqualTo("ошибка");
    }
}
