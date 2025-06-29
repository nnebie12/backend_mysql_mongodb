package com.example.demo.servicesImplMysql;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.repositoryMysql.UserRepository;
import com.example.demo.servicesMysql.UserService;

@Service
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public UserEntity saveUser(UserEntity userEntity) {
        // Hash du mot de passe lors de la création
        if (userEntity.getMotDePasse() != null) {
            userEntity.setMotDePasse(passwordEncoder.encode(userEntity.getMotDePasse()));
        }
        return userRepository.save(userEntity);
    }
    
    @Override
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    @Override
    public UserEntity updateUser(Long id, UserEntity userDetails) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        user.setNom(userDetails.getNom()); 
        user.setPrenom(userDetails.getPrenom());
        user.setEmail(userDetails.getEmail());
        
        if (userDetails.getMotDePasse() != null && !userDetails.getMotDePasse().isEmpty()) {
            user.setMotDePasse(passwordEncoder.encode(userDetails.getMotDePasse()));
        }
        
        user.setPreferenceAlimentaire(userDetails.getPreferenceAlimentaire());
        user.setRole(userDetails.getRole()); 
        
        return userRepository.save(user);
    }
    
    // Méthode pour admin (peut modifier le rôle)
    @PreAuthorize("hasRole('ADMIN')")
    public UserEntity updateUserAsAdmin(Long id, UserEntity userDetails) {
        return updateUser(id, userDetails);
    }
}