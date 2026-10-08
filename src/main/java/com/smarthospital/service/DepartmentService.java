package com.smarthospital.service;

import com.smarthospital.dto.Dtos.*;
import com.smarthospital.dto.Mapper;
import com.smarthospital.entity.Department;
import com.smarthospital.exception.ApiException;
import com.smarthospital.repository.DepartmentRepository;
import com.smarthospital.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departments;
    private final DoctorRepository doctors;

    public List<DepartmentDto> list() {
        return departments.findAllByOrderByNameAsc().stream().map(Mapper::department).toList();
    }

    public DepartmentDto create(DepartmentRequest r) {
        if (departments.existsByNameIgnoreCase(r.name().trim()))
            throw ApiException.conflict("A department with this name already exists");
        return Mapper.department(departments.save(
                Department.builder().name(r.name().trim()).description(r.description()).build()));
    }

    public DepartmentDto update(Long id, DepartmentRequest r) {
        Department d = find(id);
        if (!d.getName().equalsIgnoreCase(r.name().trim()) && departments.existsByNameIgnoreCase(r.name().trim()))
            throw ApiException.conflict("A department with this name already exists");
        d.setName(r.name().trim());
        d.setDescription(r.description());
        return Mapper.department(departments.save(d));
    }

    public void delete(Long id) {
        Department d = find(id);
        if (doctors.countByDepartmentIdAndActiveTrue(id) > 0)
            throw ApiException.conflict("Move or remove this department's doctors first");
        departments.delete(d);
    }

    public Department find(Long id) {
        return departments.findById(id).orElseThrow(() -> ApiException.notFound("Department not found"));
    }
}
