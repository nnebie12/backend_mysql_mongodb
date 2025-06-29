package com.example.demo.servicesMysql;

import com.example.demo.DTO.AuthRequest;
import com.example.demo.DTO.AuthResponse;
import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse authenticate(AuthRequest authRequest) {
        Optional<UserEntity> userOpt = userService.getUserByEmail(authRequest.getEmail());

        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            if (passwordEncoder.matches(authRequest.getMotDePasse(), user.getMotDePasse())) {
                String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole());
                return new AuthResponse(
                        token,
                        user.getId(),
                        user.getNom(),
                        user.getPrenom(),
                        user.getEmail(),
                        user.getRole()
                );
            }
        }
        throw new RuntimeException("Email ou mot de passe incorrect");
    }

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    public Long getUserIdFromToken(String token) {
        return jwtUtil.extractUserId(token);
    }

    public String getRoleFromToken(String token) {
        return jwtUtil.extractRole(token);
    }

    public UserEntity registerUser(UserEntity user) {
        if (userService.getUserByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }

        user.setMotDePasse(passwordEncoder.encode(user.getMotDePasse()));
        return userService.saveUser(user);
    }

    public String getEmailFromToken(String token) {
        return jwtUtil.extractEmail(token);
    }
}