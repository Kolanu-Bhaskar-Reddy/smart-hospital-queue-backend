package com.smarthospital.service;

import com.smarthospital.dto.Dtos.*;
import com.smarthospital.dto.Mapper;
import com.smarthospital.entity.*;
import com.smarthospital.exception.ApiException;
import com.smarthospital.repository.AppointmentRepository;
import com.smarthospital.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static com.smarthospital.entity.AppointmentStatus.*;

/** Doctor-side queue operations and the admin's live view of every queue. */
@Service
@RequiredArgsConstructor
public class QueueService {
    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;

    public List<AppointmentDto> todayAppointments(User doctorUser) {
        Doctor d = doctorOf(doctorUser);
        return appointments.findByDoctorIdAndAppointmentDateOrderBySlotTimeAsc(d.getId(), LocalDate.now())
                .stream().map(Mapper::appointment).toList();
    }

    public DoctorQueueDto queue(User doctorUser) {
        return buildQueue(doctorOf(doctorUser));
    }

    @Transactional
    public DoctorQueueDto callNext(User doctorUser) {
        Doctor d = doctorOf(doctorUser);
        List<Appointment> day = today(d);
        if (day.stream().anyMatch(a -> a.getStatus() == IN_PROGRESS))
            throw ApiException.conflict("Finish the current consultation before calling the next patient");
        Appointment next = day.stream().filter(a -> a.getStatus() == WAITING)
                .min(Comparator.comparingInt(Appointment::getTokenNumber))
                .orElseThrow(() -> ApiException.notFound("No patients are waiting"));
        next.setStatus(IN_PROGRESS);
        return buildQueue(d);
    }

    @Transactional
    public DoctorQueueDto complete(User doctorUser, Long id) {
        Doctor d = doctorOf(doctorUser);
        Appointment a = ownedBy(d, id);
        if (a.getStatus() != IN_PROGRESS) throw ApiException.conflict("This patient is not in consultation");
        a.setStatus(COMPLETED);
        return buildQueue(d);
    }

    @Transactional
    public DoctorQueueDto noShow(User doctorUser, Long id) {
        Doctor d = doctorOf(doctorUser);
        Appointment a = ownedBy(d, id);
        if (a.getStatus() != WAITING && a.getStatus() != IN_PROGRESS)
            throw ApiException.conflict("Only waiting or in-consultation patients can be marked as no-show");
        a.setStatus(NO_SHOW);
        return buildQueue(d);
    }

    public List<DoctorQueueDto> allLiveQueues() {
        return doctors.findByActiveTrue().stream()
                .sorted(Comparator.comparing(d -> d.getUser().getName()))
                .map(this::buildQueue).toList();
    }

    // ---- helpers ----
    private DoctorQueueDto buildQueue(Doctor d) {
        List<Appointment> day = today(d);
        AppointmentDto serving = day.stream().filter(a -> a.getStatus() == IN_PROGRESS)
                .findFirst().map(Mapper::appointment).orElse(null);
        List<AppointmentDto> waiting = day.stream().filter(a -> a.getStatus() == WAITING)
                .sorted(Comparator.comparingInt(Appointment::getTokenNumber)).map(Mapper::appointment).toList();
        int completed = (int) day.stream().filter(a -> a.getStatus() == COMPLETED).count();
        int total = (int) day.stream().filter(a -> a.getStatus() != CANCELLED).count();
        return new DoctorQueueDto(d.getId(), d.getUser().getName(), d.getDepartment().getName(),
                d.getConsultationMinutes(), serving, waiting, completed, total);
    }

    private List<Appointment> today(Doctor d) {
        return appointments.findByDoctorIdAndAppointmentDateOrderBySlotTimeAsc(d.getId(), LocalDate.now());
    }

    private Doctor doctorOf(User u) {
        return doctors.findByUserId(u.getId()).orElseThrow(() -> ApiException.forbidden("No doctor profile for this account"));
    }

    private Appointment ownedBy(Doctor d, Long id) {
        Appointment a = appointments.findById(id).orElseThrow(() -> ApiException.notFound("Appointment not found"));
        if (!a.getDoctor().getId().equals(d.getId())) throw ApiException.notFound("Appointment not found");
        if (!a.getAppointmentDate().equals(LocalDate.now()))
            throw ApiException.conflict("Only today's appointments can be updated");
        return a;
    }
}
