package com.smarthospital.dto;

import com.smarthospital.dto.Dtos.*;
import com.smarthospital.entity.*;

public final class Mapper {
    private Mapper() {}

    public static UserInfo user(User u) {
        return new UserInfo(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(), u.isActive());
    }

    public static DepartmentDto department(Department d) {
        return new DepartmentDto(d.getId(), d.getName(), d.getDescription());
    }

    public static DoctorDto doctor(Doctor d) {
        return new DoctorDto(d.getId(), d.getUser().getName(), d.getUser().getEmail(), d.getUser().getPhone(),
                d.getDepartment().getId(), d.getDepartment().getName(), d.getSpecialization(),
                d.getConsultationMinutes(), d.getStartTime(), d.getEndTime());
    }

    public static AppointmentDto appointment(Appointment a) {
        return new AppointmentDto(a.getId(), a.getPatient().getId(), a.getPatient().getName(), a.getPatient().getPhone(),
                a.getDoctor().getId(), a.getDoctor().getUser().getName(), a.getDoctor().getDepartment().getName(),
                a.getAppointmentDate(), a.getSlotTime(), a.getTokenNumber(), a.getStatus(), a.getReason());
    }
}
