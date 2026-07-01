// ═══════════════════════════════════════════════════════════════════════════
//  PATCH #7 — UserServiceImpl.java
//  Ajout : soft delete (RGPD Art. 17) au lieu de suppression définitive
// ═══════════════════════════════════════════════════════════════════════════

package com.example.demo.servicesImplMysql;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.exception.UserNotFoundException;
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
    
    /**
     * ✅ PATCH : soft delete (RGPD Art. 17)
     * Au lieu de supprimer définitivement, marquer l'utilisateur comme inactif
     * et anonymiser ses données personnelles.
     * 
     * Les données peuvent être supprimées définitivement après 30 jours
     * pour respecter les délais de rétention légaux.
     */
    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User with ID " + id + " not found for deletion.");
        }
        
        Optional<UserEntity> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            
            // ✅ Soft delete : marquer inactif au lieu de supprimer
            user.setActif(false);
            user.setDeletedAt(LocalDateTime.now());
            
            // ✅ Anonymiser les données personnelles (RGPD Art. 17)
            user.anonymize();
            
            userRepository.save(user);
        }
    }

    /**
     * ✅ NOUVEAU — Soft delete un utilisateur (RGPD)
     * Variante explicite de deleteUser() pour une meilleure clarté
     * 
     * Marque l'utilisateur comme inactif et anonymise ses données.
     * Les données restent en base pour l'audit trail jusqu'à la suppression définitive.
     */
    public void softDeleteUser(Long id) {
        Optional<UserEntity> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("User with ID " + id + " not found.");
        }
        
        UserEntity user = userOpt.get();
        
        // Vérifier que l'utilisateur n'est pas déjà supprimé
        if (user.isDeleted()) {
            throw new IllegalStateException("User already soft-deleted on " + user.getDeletedAt());
        }
        
        // Marquer comme inactif et anonymiser
        user.setActif(false);
        user.setDeletedAt(LocalDateTime.now());
        user.anonymize();
        
        userRepository.save(user);
    }

    /**
     * ✅ NOUVEAU — Suppression définitive après délai de rétention (RGPD)
     * 
     * Après 30 jours de soft delete, l'utilisateur peut demander la suppression
     * définitive. Cette méthode supprime complètement l'enregistrement de la base.
     */
    public void hardDeleteUser(Long id) {
        Optional<UserEntity> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("User with ID " + id + " not found.");
        }
        
        UserEntity user = userOpt.get();
        
        // Vérifier que l'utilisateur est en soft delete
        if (!user.isDeleted()) {
            throw new IllegalStateException("User not in soft-delete state. Must soft-delete first.");
        }
        
        // Vérifier la période de rétention (30 jours)
        LocalDateTime now = LocalDateTime.now();
        long daysSinceDeletion = ChronoUnit.DAYS.between(user.getDeletedAt(), now);
        
        if (daysSinceDeletion < 30) {
            throw new IllegalStateException(
                String.format(
                    "User soft-deleted %d days ago. Permanent deletion allowed after 30 days. " +
                    "Please wait %d more days (GDPR Art. 17).",
                    daysSinceDeletion, 
                    30 - daysSinceDeletion
                )
            );
        }
        
        // Suppression définitive
        userRepository.deleteById(id);
    }

    /**
     * ✅ NOUVEAU — Vérifier si l'utilisateur est actif
     * Utilisé pour les requêtes : filtrer les utilisateurs supprimés
     */
    public List<UserEntity> getActiveUsers() {
        // À implémenter si UserRepository a une méthode findByActifTrue()
        // return userRepository.findByActifTrue();
        return userRepository.findAll().stream()
            .filter(UserEntity::isActive)
            .toList();
    }
    
    @Override
    public UserEntity updateUserAsAdmin(Long id, UserEntity userDetails) {
        Optional<UserEntity> existingUserOpt = userRepository.findById(id);
        if (existingUserOpt.isEmpty()) {
            throw new UserNotFoundException("User with ID " + id + " not found.");
        }
        UserEntity existingUser = existingUserOpt.get();

        // ✅ Vérifier que l'utilisateur n'est pas supprimé (soft delete)
        if (existingUser.isDeleted()) {
            throw new IllegalStateException(
                "Cannot update deleted user (soft-deleted on " + existingUser.getDeletedAt() + ")"
            );
        }

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
            String encoded = passwordEncoder.encode(userDetails.getMotDePasse());
            existingUser.setMotDePasse(encoded);
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