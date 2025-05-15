package com.example.demo.servicesMysql;

import com.example.demo.DTO.AuthRequest;
import com.example.demo.DTO.AuthResponse;
import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthServiceTest {

    private UserService userService;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);
        authService = new AuthService(userService, passwordEncoder, jwtUtil);
    }

    @Test
    void authenticate_shouldReturnAuthResponse_whenCredentialsAreValid() {
        AuthRequest request = new AuthRequest("test@example.com", "password");

        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setMotDePasse("encodedPassword");
        user.setNom("Test");
        user.setPrenom("User");
        user.setRole("USER");

        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com", 1L, "USER")).thenReturn("mocked-jwt-token");

        AuthResponse response = authService.authenticate(request);

        assertEquals("mocked-jwt-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("USER", response.getRole());
    }
    
    @Test
    void authenticate_shouldThrowException_whenPasswordIsInvalid() {
        AuthRequest request = new AuthRequest("test@example.com", "wrongpassword");

        UserEntity user = new UserEntity();
        user.setEmail("test@example.com");
        user.setMotDePasse("encodedPassword");

        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        Exception exception = assertThrows(RuntimeException.class, () -> authService.authenticate(request));
        assertEquals("Email ou mot de passe incorrect", exception.getMessage());
    }
    
    @Test
    void authenticate_shouldThrowException_whenUserNotFound() {
        AuthRequest request = new AuthRequest("unknown@example.com", "password");

        when(userService.getUserByEmail("unknown@example.com")).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> authService.authenticate(request));
        assertEquals("Email ou mot de passe incorrect", exception.getMessage());
    }

    @Test
    void registerUser_shouldSaveUserWithEncodedPassword_andDefaultRole() {
        UserEntity user = new UserEntity();
        user.setEmail("new@example.com");
        user.setMotDePasse("plainPassword");

        when(userService.getUserByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userService.saveUser(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = authService.registerUser(user);

        assertEquals("encodedPassword", result.getMotDePasse());
        assertEquals("USER", result.getRole());
    }

    @Test
    void registerUser_shouldThrowException_whenEmailAlreadyExists() {
        UserEntity user = new UserEntity();
        user.setEmail("existing@example.com");

        when(userService.getUserByEmail("existing@example.com")).thenReturn(Optional.of(new UserEntity()));

        Exception exception = assertThrows(RuntimeException.class, () -> authService.registerUser(user));
        assertEquals("Cet email est déjà utilisé", exception.getMessage());
    }
}

