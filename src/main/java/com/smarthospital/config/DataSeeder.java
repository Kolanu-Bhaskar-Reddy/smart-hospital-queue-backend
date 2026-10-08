package com.smarthospital.config;

import com.smarthospital.entity.*;
import com.smarthospital.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/** Creates demo accounts and departments on first start (only when the database is empty). */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final DoctorRepository doctors;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;

        user("Hospital Admin", "admin@hospital.com", "Admin@123", "9000000000", Role.ADMIN);
        user("Demo Patient", "patient@hospital.com", "Patient@123", "9000000001", Role.PATIENT);

        Department gen = dept("General Medicine", "Everyday illness, fevers, check-ups");
        Department card = dept("Cardiology", "Heart and blood pressure care");
        Department peds = dept("Pediatrics", "Care for babies, children and teens");
        dept("Orthopedics", "Bones, joints and muscles");

        doctor("Dr. Asha Rao", "asha@hospital.com", gen, "Family physician", 15, 9, 13);
        doctor("Dr. Vikram Shah", "vikram@hospital.com", card, "Cardiologist", 20, 10, 16);
        doctor("Dr. Meera Nair", "meera@hospital.com", peds, "Pediatrician", 15, 9, 15);
    }

    private User user(String name, String email, String pw, String phone, Role role) {
        return users.save(User.builder().name(name).email(email).password(encoder.encode(pw))
                .phone(phone).role(role).build());
    }

    private Department dept(String name, String desc) {
        return departments.save(Department.builder().name(name).description(desc).build());
    }

    private void doctor(String name, String email, Department d, String spec, int minutes, int from, int to) {
        User u = user(name, email, "Doctor@123", "9000000002", Role.DOCTOR);
        doctors.save(Doctor.builder().user(u).department(d).specialization(spec).consultationMinutes(minutes)
                .startTime(LocalTime.of(from, 0)).endTime(LocalTime.of(to, 0)).build());
    }
}
