package com.example.demo.servicesImplMysql;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.repositoryMysql.UserRepository;
import com.example.demo.servicesMysql.UserService;
import com.example.demo.exception.UserNotFoundException;
import java.nio.charset.StandardCharsets;

@Service
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    public UserEntity saveUser(UserEntity userEntity) {
        return userRepository.save(userEntity);
    }
    
    @Override
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<UserEntity> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<UserEntity> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User with ID " + id + " not found for deletion.");
        }
        userRepository.deleteById(id);
    }
    
    @Override
    public UserEntity updateUserAsAdmin(Long id, UserEntity userDetails) {
        Optional<UserEntity> existingUserOpt = userRepository.findById(id);
        if (existingUserOpt.isEmpty()) {
            throw new UserNotFoundException("User with ID " + id + " not found.");
        }
        UserEntity existingUser = existingUserOpt.get();

        if (userDetails.getNom() != null) {
            existingUser.setNom(userDetails.getNom());
        }
        if (userDetails.getPrenom() != null) {
            existingUser.setPrenom(userDetails.getPrenom());
        }
        if (userDetails.getEmail() != null) {
            existingUser.setEmail(userDetails.getEmail());
        }
        if (userDetails.getMotDePasse() != null && !userDetails.getMotDePasse().isEmpty() && !userDetails.getMotDePasse().startsWith("$2a$")) {
            existingUser.setMotDePasse(passwordEncoder.encode(userDetails.getMotDePasse()));
        }
        if (userDetails.getRole() != null) {
            existingUser.setRole(userDetails.getRole());
        }
        if (userDetails.getPreferenceAlimentaire() != null) {
            existingUser.setPreferenceAlimentaire(userDetails.getPreferenceAlimentaire());
        }

        return userRepository.save(existingUser);
    }
}
