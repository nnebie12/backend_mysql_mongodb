package com.example.demo.servicesMysql;

import com.example.demo.DTO.AuthRequest;
import com.example.demo.DTO.AuthResponse;
import com.example.demo.DTO.LoginRequest;
import com.example.demo.DTO.UpdateProfileRequest;
import com.example.demo.DTO.UserResponse;
import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.entitiesMysql.ennums.Role;
import com.example.demo.repositoryMysql.UserRepository;
import com.example.demo.security.JwtUtil;
import org.springframework.security.core.Authentication;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;


    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, UserRepository userRepository, AuthenticationManager authenticationManager ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;

    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Transactional
    public UserResponse registerUser(AuthRequest request) {
    	if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setNom(request.getNom());
        userEntity.setPrenom(request.getPrenom());
        userEntity.setEmail(request.getEmail());
        userEntity.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        userEntity.setPreferenceAlimentaire(request.getPreferenceAlimentaire());
        userEntity.setIngredientsApprecies(request.getIngredientsApprecies());
        userEntity.setIngredientsEvites(request.getIngredientsEvites());
        userEntity.setContraintesAlimentaires(request.getContraintesAlimentaires());
        
        userEntity.setNiveauCuisine(request.getNiveauCuisine());
        userEntity.setNewsletter(request.getNewsletter());
        userEntity.setRole(Role.USER);

        UserEntity savedUser = userRepository.save(userEntity);
        return new UserResponse(savedUser);
    }
    
    public String login(LoginRequest request) {
        // 1. Authentifier l'utilisateur
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse())
        );

        // 2. Récupérer l'utilisateur complet depuis la DB pour avoir l'ID et le rôle
        UserEntity user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé après authentification"));

        // 3. Appeler generateToken avec les 3 arguments attendus
        // Argument 1 : Email (String)
        // Argument 2 : Id (Long)
        // Argument 3 : Role (String, converti depuis l'Enum)
        return jwtUtil.generateToken(
            user.getEmail(), 
            user.getId(), 
            user.getRole().name()
        );
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


    public String getEmailFromToken(String token) {
        return jwtUtil.extractEmail(token);
    }
    
    public UserResponse getCurrentUser(String email) {
        UserEntity user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return new UserResponse(user);
    }
    
    public AuthResponse authenticate(AuthRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        
        UserResponse ui = new UserResponse(user);

        return new AuthResponse(
            token, 
            ui.getId(), 
            ui.getNom(), 
            ui.getPrenom(), 
            ui.getEmail(), 
            ui.getRole(), 
            ui.getPreferenceAlimentaire(),
            ui.getContraintesAlimentaires(),
            ui.getNiveauCuisine()
        );
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        UserEntity user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (request.getNom() != null) {
            user.setNom(request.getNom());
        }
        if (request.getPrenom() != null) {
            user.setPrenom(request.getPrenom());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPreferenceAlimentaire() != null) {
            user.setPreferenceAlimentaire(request.getPreferenceAlimentaire());
        }
        if (request.getIngredientsApprecies() != null) {
            user.setIngredientsApprecies(request.getIngredientsApprecies());
        }
        if (request.getIngredientsEvites() != null) {
            user.setIngredientsEvites(request.getIngredientsEvites());
        }
        if (request.getContraintesAlimentaires() != null) {
            user.setContraintesAlimentaires(request.getContraintesAlimentaires());
        }

        UserEntity updatedUser = userRepository.save(user);
        return new UserResponse(updatedUser);
    }
}
