package com.smarthospital.controller;

import com.smarthospital.dto.Dtos.*;
import com.smarthospital.entity.User;
import com.smarthospital.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class PatientAppointmentController {
    private final AppointmentService service;

    @PostMapping
    public AppointmentDto book(@AuthenticationPrincipal User me, @Valid @RequestBody BookRequest r) {
        return service.book(me, r);
    }

    @GetMapping("/my")
    public List<AppointmentDto> mine(@AuthenticationPrincipal User me) { return service.mine(me); }

    @PutMapping("/{id}/cancel")
    public AppointmentDto cancel(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return service.cancel(me, id);
    }

    @GetMapping("/{id}/queue")
    public QueueStatusDto queue(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return service.queueStatus(me, id);
    }
}
