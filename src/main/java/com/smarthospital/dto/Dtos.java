package com.smarthospital.dto;

import com.smarthospital.entity.AppointmentStatus;
import com.smarthospital.entity.Role;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** All request/response shapes of the REST API. */
public final class Dtos {
    private Dtos() {}

    // ---- auth ----
    public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email,
                                  @NotBlank @Size(min = 6, message = "must be at least 6 characters") String password,
                                  String phone) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record UserInfo(Long id, String name, String email, String phone, Role role, boolean active) {}
    public record AuthResponse(String token, UserInfo user) {}

    // ---- departments & doctors ----
    public record DepartmentRequest(@NotBlank String name, String description) {}
    public record DepartmentDto(Long id, String name, String description) {}

    public record DoctorRequest(@NotBlank String name, @NotBlank @Email String email, String password, String phone,
                                @NotNull Long departmentId, String specialization,
                                @NotNull @Min(5) @Max(120) Integer consultationMinutes,
                                @NotNull LocalTime startTime, @NotNull LocalTime endTime) {}
    public record DoctorDto(Long id, String name, String email, String phone, Long departmentId, String departmentName,
                            String specialization, int consultationMinutes, LocalTime startTime, LocalTime endTime) {}
    public record SlotDto(LocalTime time, int token, boolean available) {}

    // ---- appointments & queue ----
    public record BookRequest(@NotNull Long doctorId, @NotNull LocalDate date, @NotNull LocalTime slotTime,
                              @Size(max = 500) String reason) {}
    public record AppointmentDto(Long id, Long patientId, String patientName, String patientPhone,
                                 Long doctorId, String doctorName, String departmentName,
                                 LocalDate date, LocalTime slotTime, int token,
                                 AppointmentStatus status, String reason) {}
    /** What a patient sees while tracking their place in line. */
    public record QueueStatusDto(Long appointmentId, int token, AppointmentStatus status, boolean live,
                                 Integer nowServingToken, int patientsAhead, int estimatedWaitMinutes,
                                 String doctorName, String departmentName, LocalDate date, LocalTime slotTime) {}
    /** A doctor's queue for today. */
    public record DoctorQueueDto(Long doctorId, String doctorName, String departmentName, int consultationMinutes,
                                 AppointmentDto nowServing, List<AppointmentDto> waiting,
                                 int completedCount, int totalToday) {}

    // ---- reports ----
    public record ReportSummary(long totalAppointments, long appointmentsToday, long waitingNow,
                                long completedToday, long cancelledToday, long totalDoctors, long totalPatients,
                                Map<String, Long> byDepartment, Map<String, Long> byStatus) {}
}
