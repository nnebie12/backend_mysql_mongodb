package com.example.demo.servicesMysql;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.DTO.AuthRequest;
import com.example.demo.DTO.AuthResponse;
import com.example.demo.DTO.LoginRequest;
import com.example.demo.DTO.UpdateProfileRequest;
import com.example.demo.DTO.UserResponse;
import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.entitiesMysql.ennums.Role;
import com.example.demo.repositoryMysql.UserRepository;
import com.example.demo.security.JwtUtil;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;


    public AuthService(PasswordEncoder passwordEncoder, JwtUtil jwtUtil, UserRepository userRepository, AuthenticationManager authenticationManager ) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;

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
        String encodedPassword = passwordEncoder.encode(request.getMotDePasse());
        userEntity.setMotDePasse(encodedPassword);
        userEntity.setPassword(encodedPassword);
        userEntity.setMotDePasseLegacy(encodedPassword);
        userEntity.setPreferenceAlimentaire(request.getPreferenceAlimentaire());
        userEntity.setIngredientsApprecies(request.getIngredientsApprecies());
        userEntity.setIngredientsEvites(request.getIngredientsEvites());
        userEntity.setContraintesAlimentaires(request.getContraintesAlimentaires());
        
        userEntity.setNiveauCuisine(request.getNiveauCuisine());
        userEntity.setNewsletter(request.getNewsletter());
        userEntity.setActif(true);
        userEntity.setActive(true);
        userEntity.setRole(Role.USER);

        UserEntity savedUser = userRepository.save(userEntity);
        return new UserResponse(savedUser);
    }
    
    public String login(LoginRequest request) {
        return authenticate(request).getToken();
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
    
    public AuthResponse authenticate(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse())
        );

        UserEntity user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé après authentification"));

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
