package br.com.cloudport.servicogate.exception;

import java.time.OffsetDateTime;
import java.util.List;

public class ApiError {

    private final OffsetDateTime timestamp;
    private final int status;
    private final String message;
    private final List<String> errors;

    public ApiError(int status, String message, List<String> errors) {
        this.timestamp = OffsetDateTime.now();
        this.status = status;
        this.message = message;
        this.errors = errors;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getErrors() {
        return errors;
    }
}
