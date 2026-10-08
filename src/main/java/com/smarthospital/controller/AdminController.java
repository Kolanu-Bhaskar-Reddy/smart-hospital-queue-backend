package com.smarthospital.controller;

import com.smarthospital.dto.Dtos.*;
import com.smarthospital.entity.AppointmentStatus;
import com.smarthospital.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final DepartmentService departments;
    private final DoctorService doctors;
    private final AppointmentService appointments;
    private final QueueService queues;
    private final AdminService admin;

    // departments
    @GetMapping("/departments")
    public List<DepartmentDto> listDepartments() { return departments.list(); }

    @PostMapping("/departments")
    public DepartmentDto createDepartment(@Valid @RequestBody DepartmentRequest r) { return departments.create(r); }

    @PutMapping("/departments/{id}")
    public DepartmentDto updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentRequest r) {
        return departments.update(id, r);
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departments.delete(id);
        return ResponseEntity.noContent().build();
    }

    // doctors
    @GetMapping("/doctors")
    public List<DoctorDto> listDoctors() { return doctors.search(null, null); }

    @PostMapping("/doctors")
    public DoctorDto createDoctor(@Valid @RequestBody DoctorRequest r) { return doctors.create(r); }

    @PutMapping("/doctors/{id}")
    public DoctorDto updateDoctor(@PathVariable Long id, @Valid @RequestBody DoctorRequest r) {
        return doctors.update(id, r);
    }

    @DeleteMapping("/doctors/{id}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
        doctors.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // patients
    @GetMapping("/patients")
    public List<UserInfo> patients() { return admin.patients(); }

    @PutMapping("/patients/{id}/active")
    public UserInfo setActive(@PathVariable Long id, @RequestParam boolean value) {
        return admin.setPatientActive(id, value);
    }

    // appointments, queues, reports
    @GetMapping("/appointments")
    public List<AppointmentDto> appointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) Long doctorId) {
        return appointments.search(date, status, doctorId);
    }

    @PutMapping("/appointments/{id}/cancel")
    public AppointmentDto cancel(@PathVariable Long id) { return appointments.adminCancel(id); }

    @GetMapping("/queues")
    public List<DoctorQueueDto> liveQueues() { return queues.allLiveQueues(); }

    @GetMapping("/reports/summary")
    public ReportSummary summary() { return admin.summary(); }
}
