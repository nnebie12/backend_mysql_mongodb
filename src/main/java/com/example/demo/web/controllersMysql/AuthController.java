package com.example.demo.web.controllersMysql;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTO.AuthRequest;
import com.example.demo.DTO.AuthResponse;
import com.example.demo.DTO.LoginRequest;
import com.example.demo.DTO.UpdateProfileRequest;
import com.example.demo.DTO.UserResponse;
import com.example.demo.servicesMysql.AuthService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    

    public AuthController(AuthService authService) {
        this.authService = authService;
       
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody @Valid LoginRequest authRequest) {
        try {
            AuthResponse authResponse = authService.authenticate(authRequest);
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body("Email ou mot de passe incorrect");
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid AuthRequest request) { 
        try {
            UserResponse savedUser = authService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser() { 
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        try {
            UserResponse user = authService.getCurrentUser(email);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<String> validateToken() {
        return ResponseEntity.ok("Token valide");
    }
    
    @PutMapping("/update")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {
        try {
            String email = authentication.getName();
            UserResponse user = authService.updateProfile(email, request);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
