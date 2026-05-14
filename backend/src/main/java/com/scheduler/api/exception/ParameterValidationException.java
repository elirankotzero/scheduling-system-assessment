package com.scheduler.api.exception;

import java.util.List;

public class ParameterValidationException extends RuntimeException {
    private final List<String> missingParameters;

    public ParameterValidationException(List<String> missingParameters) {
        super("Missing required parameters: " + String.join(", ", missingParameters));
        this.missingParameters = missingParameters;
    }

    public List<String> getMissingParameters() {
        return missingParameters;
    }
}
