package com.smarthospital.service;

import com.smarthospital.dto.Dtos.*;
import com.smarthospital.dto.Mapper;
import com.smarthospital.entity.*;
import com.smarthospital.exception.ApiException;
import com.smarthospital.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.smarthospital.entity.AppointmentStatus.*;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private static final Set<AppointmentStatus> ACTIVE = EnumSet.of(WAITING, IN_PROGRESS);

    private final AppointmentRepository appointments;
    private final DoctorService doctorService;

    @Transactional
    public AppointmentDto book(User patient, BookRequest r) {
        Doctor d = doctorService.find(r.doctorId());
        LocalDate today = LocalDate.now();
        if (r.date().isBefore(today)) throw ApiException.badRequest("You can't book a date in the past");
        if (r.date().isAfter(today.plusDays(30)))
            throw ApiException.badRequest("Appointments can be booked up to 30 days ahead");

        int index = DoctorService.allSlots(d).indexOf(r.slotTime());
        if (index < 0) throw ApiException.badRequest("That time is not one of this doctor's slots");
        if (r.date().equals(today) && !r.slotTime().isAfter(LocalTime.now()))
            throw ApiException.badRequest("That slot has already passed. Pick a later one.");
        if (appointments.existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusIn(
                patient.getId(), d.getId(), r.date(), ACTIVE))
            throw ApiException.conflict("You already have an appointment with this doctor on that day");

        Appointment a = Appointment.builder()
                .patient(patient).doctor(d).appointmentDate(r.date()).slotTime(r.slotTime())
                .tokenNumber(index + 1).status(WAITING).reason(r.reason())
                .slotKey(d.getId() + "|" + r.date() + "|" + r.slotTime())
                .build();
        try {
            return Mapper.appointment(appointments.saveAndFlush(a));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("Someone just booked that slot. Please choose another.");
        }
    }

    public List<AppointmentDto> mine(User patient) {
        return appointments.findByPatientIdOrderByAppointmentDateDescSlotTimeDesc(patient.getId())
                .stream().map(Mapper::appointment).toList();
    }

    @Transactional
    public AppointmentDto cancel(User patient, Long id) {
        Appointment a = own(patient, id);
        if (a.getStatus() != WAITING)
            throw ApiException.conflict("Only appointments that are still waiting can be cancelled");
        a.setStatus(CANCELLED);
        a.setSlotKey(null); // frees the slot for someone else
        return Mapper.appointment(a);
    }

    /** Live position in the queue: who is being served, how many are ahead, estimated wait. */
    public QueueStatusDto queueStatus(User patient, Long id) {
        Appointment a = own(patient, id);
        Doctor d = a.getDoctor();
        boolean today = a.getAppointmentDate().equals(LocalDate.now());
        Integer nowServing = null;
        int ahead = 0;
        if (today) {
            List<Appointment> day = appointments
                    .findByDoctorIdAndAppointmentDateOrderBySlotTimeAsc(d.getId(), a.getAppointmentDate());
            nowServing = day.stream().filter(x -> x.getStatus() == IN_PROGRESS)
                    .map(Appointment::getTokenNumber).findFirst().orElse(null);
            if (a.getStatus() == WAITING) {
                ahead = (int) day.stream().filter(x ->
                        x.getStatus() == IN_PROGRESS
                        || (x.getStatus() == WAITING && x.getTokenNumber() < a.getTokenNumber())).count();
            }
        }
        boolean live = today && a.getStatus() == WAITING || today && a.getStatus() == IN_PROGRESS;
        return new QueueStatusDto(a.getId(), a.getTokenNumber(), a.getStatus(), live, nowServing, ahead,
                ahead * d.getConsultationMinutes(), d.getUser().getName(), d.getDepartment().getName(),
                a.getAppointmentDate(), a.getSlotTime());
    }

    // ---- admin ----
    public List<AppointmentDto> search(LocalDate date, AppointmentStatus status, Long doctorId) {
        return appointments.search(date, status, doctorId).stream().map(Mapper::appointment).toList();
    }

    @Transactional
    public AppointmentDto adminCancel(Long id) {
        Appointment a = appointments.findById(id).orElseThrow(() -> ApiException.notFound("Appointment not found"));
        if (a.getStatus() != WAITING)
            throw ApiException.conflict("Only appointments that are still waiting can be cancelled");
        a.setStatus(CANCELLED);
        a.setSlotKey(null);
        return Mapper.appointment(a);
    }

    private Appointment own(User patient, Long id) {
        Appointment a = appointments.findById(id).orElseThrow(() -> ApiException.notFound("Appointment not found"));
        if (!a.getPatient().getId().equals(patient.getId())) throw ApiException.notFound("Appointment not found");
        return a;
    }
}
