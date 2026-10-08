package com.smarthospital.repository;

import com.smarthospital.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);
    List<Doctor> findByActiveTrue();
    long countByActiveTrue();
    long countByDepartmentIdAndActiveTrue(Long departmentId);
}
