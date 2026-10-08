package com.smarthospital.service;

import com.smarthospital.dto.Dtos.*;
import com.smarthospital.dto.Mapper;
import com.smarthospital.entity.Role;
import com.smarthospital.entity.User;
import com.smarthospital.exception.ApiException;
import com.smarthospital.repository.UserRepository;
import com.smarthospital.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    /** Public registration always creates a PATIENT; doctors are created by an admin. */
    public AuthResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw ApiException.conflict("An account with this email already exists");
        User u = users.save(User.builder().name(r.name().trim()).email(email)
                .password(encoder.encode(r.password())).phone(r.phone()).role(Role.PATIENT).build());
        return new AuthResponse(jwt.generate(u), Mapper.user(u));
    }

    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));
        if (!encoder.matches(r.password(), u.getPassword()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        if (!u.isActive()) throw ApiException.forbidden("This account has been deactivated. Contact the hospital admin.");
        return new AuthResponse(jwt.generate(u), Mapper.user(u));
    }
}
