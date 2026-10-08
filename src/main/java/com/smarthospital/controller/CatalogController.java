package com.smarthospital.controller;

import com.smarthospital.dto.Dtos.*;
import com.smarthospital.service.DepartmentService;
import com.smarthospital.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Read-only directory of departments and doctors (public). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {
    private final DepartmentService departments;
    private final DoctorService doctors;

    @GetMapping("/departments")
    public List<DepartmentDto> departments() { return departments.list(); }

    @GetMapping("/doctors")
    public List<DoctorDto> doctors(@RequestParam(required = false) Long departmentId,
                                   @RequestParam(required = false) String q) {
        return doctors.search(departmentId, q);
    }

    @GetMapping("/doctors/{id}")
    public DoctorDto doctor(@PathVariable Long id) { return doctors.get(id); }

    @GetMapping("/doctors/{id}/slots")
    public List<SlotDto> slots(@PathVariable Long id,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return doctors.slots(id, date);
    }
}
