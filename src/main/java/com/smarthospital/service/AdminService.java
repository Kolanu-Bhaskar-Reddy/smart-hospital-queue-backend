package com.smarthospital.service;

import com.smarthospital.dto.Dtos.*;
import com.smarthospital.dto.Mapper;
import com.smarthospital.entity.*;
import com.smarthospital.exception.ApiException;
import com.smarthospital.repository.AppointmentRepository;
import com.smarthospital.repository.DoctorRepository;
import com.smarthospital.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository users;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;

    public List<UserInfo> patients() {
        return users.findByRoleOrderByNameAsc(Role.PATIENT).stream().map(Mapper::user).toList();
    }

    public UserInfo setPatientActive(Long id, boolean active) {
        User u = users.findById(id).filter(x -> x.getRole() == Role.PATIENT)
                .orElseThrow(() -> ApiException.notFound("Patient not found"));
        u.setActive(active);
        return Mapper.user(users.save(u));
    }

    public ReportSummary summary() {
        LocalDate today = LocalDate.now();
        Map<String, Long> byDept = new LinkedHashMap<>();
        appointments.countByDepartment().forEach(r -> byDept.put((String) r[0], (Long) r[1]));
        Map<String, Long> byStatus = new LinkedHashMap<>();
        appointments.countByStatus().forEach(r -> byStatus.put(((AppointmentStatus) r[0]).name(), (Long) r[1]));
        return new ReportSummary(appointments.count(), appointments.countByAppointmentDate(today),
                appointments.countByAppointmentDateAndStatus(today, AppointmentStatus.WAITING),
                appointments.countByAppointmentDateAndStatus(today, AppointmentStatus.COMPLETED),
                appointments.countByAppointmentDateAndStatus(today, AppointmentStatus.CANCELLED),
                doctors.countByActiveTrue(), users.countByRole(Role.PATIENT), byDept, byStatus);
    }
}
