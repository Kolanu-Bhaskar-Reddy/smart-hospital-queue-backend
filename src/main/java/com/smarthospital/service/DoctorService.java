package com.smarthospital.service;

import com.smarthospital.dto.Dtos.*;
import com.smarthospital.dto.Mapper;
import com.smarthospital.entity.*;
import com.smarthospital.exception.ApiException;
import com.smarthospital.repository.AppointmentRepository;
import com.smarthospital.repository.DoctorRepository;
import com.smarthospital.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorService {
    private final DoctorRepository doctors;
    private final UserRepository users;
    private final AppointmentRepository appointments;
    private final DepartmentService departmentService;
    private final PasswordEncoder encoder;

    // ---------- reads ----------
    public List<DoctorDto> search(Long departmentId, String q) {
        String term = q == null ? "" : q.trim().toLowerCase();
        return doctors.findByActiveTrue().stream()
                .filter(d -> departmentId == null || d.getDepartment().getId().equals(departmentId))
                .filter(d -> term.isEmpty()
                        || d.getUser().getName().toLowerCase().contains(term)
                        || d.getDepartment().getName().toLowerCase().contains(term)
                        || (d.getSpecialization() != null && d.getSpecialization().toLowerCase().contains(term)))
                .sorted(Comparator.comparing(d -> d.getUser().getName()))
                .map(Mapper::doctor).toList();
    }

    public Doctor find(Long id) {
        return doctors.findById(id).filter(Doctor::isActive)
                .orElseThrow(() -> ApiException.notFound("Doctor not found"));
    }

    public DoctorDto get(Long id) { return Mapper.doctor(find(id)); }

    /** Every slot of the doctor's day with availability; token number = slot position + 1. */
    public List<SlotDto> slots(Long doctorId, LocalDate date) {
        Doctor d = find(doctorId);
        Set<LocalTime> taken = appointments.findByDoctorIdAndAppointmentDateOrderBySlotTimeAsc(doctorId, date).stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .map(Appointment::getSlotTime).collect(Collectors.toSet());
        List<LocalTime> all = allSlots(d);
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<SlotDto> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            LocalTime t = all.get(i);
            boolean past = date.isBefore(today) || (date.equals(today) && !t.isAfter(now));
            out.add(new SlotDto(t, i + 1, !past && !taken.contains(t)));
        }
        return out;
    }

    public static List<LocalTime> allSlots(Doctor d) {
        List<LocalTime> r = new ArrayList<>();
        int step = d.getConsultationMinutes();
        LocalTime t = d.getStartTime();
        while (true) {
            LocalTime next = t.plusMinutes(step);
            if (!next.isAfter(t) || next.isAfter(d.getEndTime())) break; // stop at end of day / midnight wrap
            r.add(t);
            t = next;
        }
        return r;
    }

    // ---------- admin writes ----------
    @Transactional
    public DoctorDto create(DoctorRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw ApiException.conflict("An account with this email already exists");
        if (r.password() == null || r.password().length() < 6)
            throw ApiException.badRequest("Password must be at least 6 characters");
        checkHours(r);
        User u = users.save(User.builder().name(r.name().trim()).email(email)
                .password(encoder.encode(r.password())).phone(r.phone()).role(Role.DOCTOR).build());
        Doctor d = doctors.save(Doctor.builder().user(u).department(departmentService.find(r.departmentId()))
                .specialization(r.specialization()).consultationMinutes(r.consultationMinutes())
                .startTime(r.startTime()).endTime(r.endTime()).build());
        return Mapper.doctor(d);
    }

    @Transactional
    public DoctorDto update(Long id, DoctorRequest r) {
        Doctor d = find(id);
        checkHours(r);
        User u = d.getUser();
        String email = r.email().trim().toLowerCase();
        if (!u.getEmail().equals(email) && users.existsByEmail(email))
            throw ApiException.conflict("An account with this email already exists");
        u.setName(r.name().trim());
        u.setEmail(email);
        u.setPhone(r.phone());
        if (r.password() != null && !r.password().isBlank()) {
            if (r.password().length() < 6) throw ApiException.badRequest("Password must be at least 6 characters");
            u.setPassword(encoder.encode(r.password()));
        }
        d.setDepartment(departmentService.find(r.departmentId()));
        d.setSpecialization(r.specialization());
        d.setConsultationMinutes(r.consultationMinutes());
        d.setStartTime(r.startTime());
        d.setEndTime(r.endTime());
        users.save(u);
        return Mapper.doctor(doctors.save(d));
    }

    /** Soft delete: keeps appointment history, frees today's and future slots. */
    @Transactional
    public void deactivate(Long id) {
        Doctor d = find(id);
        appointments.findByDoctorIdAndAppointmentDateGreaterThanEqualAndStatus(
                d.getId(), LocalDate.now(), AppointmentStatus.WAITING).forEach(a -> {
            a.setStatus(AppointmentStatus.CANCELLED);
            a.setSlotKey(null);
        });
        d.setActive(false);
        d.getUser().setActive(false);
    }

    private void checkHours(DoctorRequest r) {
        if (!r.endTime().isAfter(r.startTime())) throw ApiException.badRequest("End time must be after start time");
        if (r.startTime().plusMinutes(r.consultationMinutes()).isAfter(r.endTime()))
            throw ApiException.badRequest("Working hours are shorter than one consultation");
    }
}
