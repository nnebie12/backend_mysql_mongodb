package com.example.demo.web.controllersMysql;

import com.example.demo.DTO.AuthRequest;
import com.example.demo.DTO.AuthResponse;
import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.security.CustomUserDetails; 
import com.example.demo.servicesMysql.AuthService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody AuthRequest authRequest) {
        try {
            AuthResponse authResponse = authService.authenticate(authRequest);
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Email ou mot de passe incorrect");
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<UserEntity> register(@RequestBody @Valid UserEntity user) {
        try {
            UserEntity savedUser = authService.registerUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (DataIntegrityViolationException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null); 
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); 
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserEntity> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); 
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        UserEntity user = new UserEntity();
        user.setId(userDetails.getId());
        user.setEmail(userDetails.getUsername());
        user.setRole(userDetails.getRole());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> validateToken() {
        return ResponseEntity.ok("Token valide");
    }
}