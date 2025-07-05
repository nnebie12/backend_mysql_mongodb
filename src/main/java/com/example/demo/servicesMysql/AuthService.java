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

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public AuthResponse authenticate(AuthRequest authRequest) {
        Optional<UserEntity> userOpt = userService.getUserByEmail(authRequest.getEmail());

        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            
            String plainPassword = authRequest.getMotDePasse();
            String storedPasswordHash = user.getMotDePasse();

            boolean passwordMatches = passwordEncoder.matches(plainPassword, storedPasswordHash);

            if (passwordMatches) {
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

    public Long extractUserId(String token) {
        return jwtUtil.extractUserId(token);
    }

    public String extractRole(String token) {
        return jwtUtil.extractRole(token);
    }

    public UserEntity registerUser(UserEntity user) {
        if (userService.getUserByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }
        String plainPassword = user.getMotDePasse();
        user.setMotDePasse(passwordEncoder.encode(plainPassword));

        return userService.saveUser(user);
    }

    public String getEmailFromToken(String token) {
        return jwtUtil.extractEmail(token);
    }
}
