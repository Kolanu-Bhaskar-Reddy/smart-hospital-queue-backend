package com.smarthospital.repository;

import com.smarthospital.entity.Appointment;
import com.smarthospital.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdAndAppointmentDateOrderBySlotTimeAsc(Long doctorId, LocalDate date);

    List<Appointment> findByPatientIdOrderByAppointmentDateDescSlotTimeDesc(Long patientId);

    List<Appointment> findByDoctorIdAndAppointmentDateGreaterThanEqualAndStatus(
            Long doctorId, LocalDate from, AppointmentStatus status);

    boolean existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusIn(
            Long patientId, Long doctorId, LocalDate date, Collection<AppointmentStatus> statuses);

    long countByAppointmentDate(LocalDate date);

    long countByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status);

    @Query("""
           select a from Appointment a
           where (:date is null or a.appointmentDate = :date)
             and (:status is null or a.status = :status)
             and (:doctorId is null or a.doctor.id = :doctorId)
           order by a.appointmentDate desc, a.slotTime desc
           """)
    List<Appointment> search(@Param("date") LocalDate date,
                             @Param("status") AppointmentStatus status,
                             @Param("doctorId") Long doctorId);

    @Query("select a.doctor.department.name, count(a) from Appointment a group by a.doctor.department.name")
    List<Object[]> countByDepartment();

    @Query("select a.status, count(a) from Appointment a group by a.status")
    List<Object[]> countByStatus();
}
