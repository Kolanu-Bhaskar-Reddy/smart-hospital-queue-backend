package com.smarthospital.controller;

import com.smarthospital.dto.Dtos.*;
import com.smarthospital.entity.User;
import com.smarthospital.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorPortalController {
    private final QueueService queue;

    @GetMapping("/appointments/today")
    public List<AppointmentDto> today(@AuthenticationPrincipal User me) { return queue.todayAppointments(me); }

    @GetMapping("/queue")
    public DoctorQueueDto queue(@AuthenticationPrincipal User me) { return queue.queue(me); }

    @PostMapping("/queue/call-next")
    public DoctorQueueDto callNext(@AuthenticationPrincipal User me) { return queue.callNext(me); }

    @PutMapping("/appointments/{id}/complete")
    public DoctorQueueDto complete(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return queue.complete(me, id);
    }

    @PutMapping("/appointments/{id}/no-show")
    public DoctorQueueDto noShow(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return queue.noShow(me, id);
    }
}
