package com.scheduler.api.controller;

import com.scheduler.api.dto.SchedulingRequest;
import com.scheduler.api.dto.SchedulingResponse;
import com.scheduler.api.service.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingService schedulingService;

    @GetMapping
    public ResponseEntity<List<SchedulingResponse>> getAll() {
        return ResponseEntity.ok(schedulingService.getAllSchedules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SchedulingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(schedulingService.getScheduleById(id));
    }

    @PostMapping
    public ResponseEntity<SchedulingResponse> create(@Valid @RequestBody SchedulingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulingService.createSchedule(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SchedulingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SchedulingRequest request) {
        return ResponseEntity.ok(schedulingService.updateSchedule(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        schedulingService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<SchedulingResponse> pause(@PathVariable Long id) {
        return ResponseEntity.ok(schedulingService.pauseSchedule(id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<SchedulingResponse> resume(@PathVariable Long id) {
        return ResponseEntity.ok(schedulingService.resumeSchedule(id));
    }
}
