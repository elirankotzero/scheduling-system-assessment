package com.scheduler.api.exception;

public class SchedulingNotFoundException extends RuntimeException {
    public SchedulingNotFoundException(Long id) {
        super("Scheduling not found with id: " + id);
    }
}
