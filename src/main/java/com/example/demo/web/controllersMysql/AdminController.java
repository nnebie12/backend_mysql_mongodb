package com.example.demo.web.controllersMysql; 

import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.servicesMysql.RecetteService;
import com.example.demo.servicesMysql.UserService;

import com.example.demo.entiesMongodb.ComportementUtilisateur;
import com.example.demo.servicesMongoDB.ComportementUtilisateurService;
import com.example.demo.DTO.AnalysePatternsDTO; 

import java.util.List;
import java.util.Map; 
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/administrateur")
public class AdminController {
    
    private final UserService userService;
    private final RecetteService recetteService;
    private final ComportementUtilisateurService comportementUtilisateurService; 
    
    public AdminController(UserService userService, 
                           RecetteService recetteService,
                           ComportementUtilisateurService comportementUtilisateurService) {
        this.userService = userService;
        this.recetteService = recetteService;
        this.comportementUtilisateurService = comportementUtilisateurService; 
    }
    
    // --- Gestion des Utilisateurs (MySQL) ---

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<List<UserEntity>> getAllUtilisateurs() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserEntity> updateUtilisateur(@PathVariable Long id, @RequestBody UserEntity updatedUser) {
        try {
            UserEntity user = userService.updateUserAsAdmin(id, updatedUser);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> deleteUtilisateur(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // --- Gestion des Recettes (MySQL) ---

    
    @PostMapping("/recettes")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<RecetteEntity> createRecette(@RequestBody RecetteEntity recetteEntity, @RequestParam Long userId) {
        try {
            RecetteEntity savedRecette = recetteService.saveRecette(recetteEntity, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedRecette);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/recettes/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<RecetteEntity> updateRecette(@PathVariable Long id, @RequestBody RecetteEntity recette) {
        try {
            RecetteEntity updatedRecette = recetteService.updateRecette(id, recette);
            return ResponseEntity.ok(updatedRecette);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/recettes/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> deleteRecette(@PathVariable Long id) {
        try {
            recetteService.deleteRecette(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Gestion du Comportement Utilisateur (MongoDB) - Fonctions Admin ---

    /**
     * Supprime le comportement utilisateur complet d'un utilisateur donné.
     */
    @DeleteMapping("/comportements/users/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> deleteUserComportement(@PathVariable Long userId) {
        try {
            comportementUtilisateurService.deleteUserBehavior(userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); 
        }
    }

    /**
     * Récupère la liste des utilisateurs par profil de comportement (ex: "fidèle", "actif").
     */
    @GetMapping("/comportements/profil/{profil}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<List<ComportementUtilisateur>> getUsersByProfile(@PathVariable String profil) {
        List<ComportementUtilisateur> utilisateurs = comportementUtilisateurService.getUsersByProfile(profil);
        if (utilisateurs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(utilisateurs);
    }

    /**
     * Récupère la liste des utilisateurs ayant un score d'engagement minimum.
     */
    @GetMapping("/comportements/engages")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<List<ComportementUtilisateur>> getEngagedUsers(@RequestParam Double scoreMinimum) {
        List<ComportementUtilisateur> utilisateurs = comportementUtilisateurService.getEngagedUsers(scoreMinimum);
        if (utilisateurs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(utilisateurs);
    }

    /**
     * Déclenche une analyse complète des patterns de comportement pour un utilisateur.
     */
    @PostMapping("/comportements/analyser/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ComportementUtilisateur> triggerComportementAnalysis(@PathVariable Long userId) {
        try {
            ComportementUtilisateur updatedComportement = comportementUtilisateurService.analyserPatterns(userId);
            return ResponseEntity.ok(updatedComportement);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les patterns d'analyse détaillés pour un utilisateur donné.
     */
    @GetMapping("/comportements/patterns/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<AnalysePatternsDTO> getUserComportementPatterns(@PathVariable Long userId) {
        AnalysePatternsDTO patterns = comportementUtilisateurService.analyserPatternsDTO(userId);
        if (patterns.getPatternsNavigation() == null || patterns.getPatternsNavigation().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(patterns);
    }

    /**
     * Récupère les statistiques détaillées du comportement pour un utilisateur donné.
     */
    @GetMapping("/comportements/statistiques/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Map<String, Object>> getUserComportementStatistiques(@PathVariable Long userId) {
        Map<String, Object> statistiques = comportementUtilisateurService.obtenirStatistiquesComportement(userId);
        if (statistiques.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(statistiques);
    }
}