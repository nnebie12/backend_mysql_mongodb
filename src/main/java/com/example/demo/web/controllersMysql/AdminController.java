package com.example.demo.web.controllersMysql;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTO.AnalysePatternsDTO;
import com.example.demo.DTO.UserAdminResponseDTO;      // ✅ NOUVEAU DTO sécurisé
import com.example.demo.entiesMongodb.ComportementUtilisateur;
import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.entiesMongodb.enums.ProfilUtilisateur;
import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.servicesMongoDB.ComportementUtilisateurService;
import com.example.demo.servicesMongoDB.RecommandationIAService;
import com.example.demo.servicesMysql.RecetteService;
import com.example.demo.servicesMysql.UserService;

@RestController
@RequestMapping("/api/administrateur")
@PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;
    private final RecetteService recetteService;
    private final ComportementUtilisateurService comportementUtilisateurService;
    private final RecommandationIAService recommandationIAService;

    public AdminController(UserService userService,
                           RecetteService recetteService,
                           ComportementUtilisateurService comportementUtilisateurService,
                           RecommandationIAService recommandationIAService) {
        this.userService = userService;
        this.recetteService = recetteService;
        this.comportementUtilisateurService = comportementUtilisateurService;
        this.recommandationIAService = recommandationIAService;
    }

    private <T> ResponseEntity<T> execute(Supplier<ResponseEntity<T>> action) {
        try {
            return action.get();
        } catch (Exception e) {
            logger.error("Erreur admin : {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  UTILISATEURS
    // ─────────────────────────────────────────────────────────────────

    /**
     * ✅ CORRIGÉ : retourne UserAdminResponseDTO au lieu de UserEntity brut.
     * Les mots de passe hashés ne sont plus exposés.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserAdminResponseDTO>> getAllUtilisateurs() {
        List<UserAdminResponseDTO> dtos = userService.getAllUsers()
                .stream()
                .map(UserAdminResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserAdminResponseDTO> updateUtilisateur(
            @PathVariable Long id,
            @RequestBody UserEntity updatedUser) {
        return execute(() -> {
            UserEntity updated = userService.updateUserAsAdmin(id, updatedUser);
            return ResponseEntity.ok(new UserAdminResponseDTO(updated));
        });
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUtilisateur(@PathVariable Long id) {
        return execute(() -> {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  RECETTES
    // ─────────────────────────────────────────────────────────────────

    @DeleteMapping("/recettes/{id}")
    public ResponseEntity<Void> deleteRecette(@PathVariable Long id) {
        return execute(() -> {
            recetteService.deleteRecette(id);
            return ResponseEntity.noContent().build();
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  COMPORTEMENT UTILISATEUR
    // ─────────────────────────────────────────────────────────────────

    @DeleteMapping("/comportements/users/{userId}")
    public ResponseEntity<Void> deleteUserComportement(@PathVariable Long userId) {
        return execute(() -> {
            comportementUtilisateurService.deleteUserBehavior(userId);
            return ResponseEntity.noContent().build();
        });
    }

    @GetMapping("/comportements/profil/{profil}")
    public ResponseEntity<List<ComportementUtilisateur>> getUsersByProfile(
            @PathVariable ProfilUtilisateur profil) {
        List<ComportementUtilisateur> utilisateurs = comportementUtilisateurService.getUsersByProfile(profil);
        if (utilisateurs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(utilisateurs);
    }

    @GetMapping("/comportements/engages")
    public ResponseEntity<List<ComportementUtilisateur>> getEngagedUsers(
            @RequestParam Double scoreMinimum) {
        List<ComportementUtilisateur> utilisateurs = comportementUtilisateurService.getEngagedUsers(scoreMinimum);
        if (utilisateurs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(utilisateurs);
    }

    @PostMapping("/comportements/analyser/{userId}")
    public ResponseEntity<ComportementUtilisateur> triggerComportementAnalysis(
            @PathVariable Long userId) {
        return execute(() -> ResponseEntity.ok(
                comportementUtilisateurService.analyserPatterns(userId)));
    }

    @GetMapping("/comportements/patterns/{userId}")
    public ResponseEntity<AnalysePatternsDTO> getUserComportementPatterns(
            @PathVariable Long userId) {
        AnalysePatternsDTO patterns = comportementUtilisateurService.analyserPatternsDTO(userId);
        return ResponseEntity.ok(patterns);
    }

    @GetMapping("/comportements/statistiques/{userId}")
    public ResponseEntity<Map<String, Object>> getUserComportementStatistiques(
            @PathVariable Long userId) {
        Map<String, Object> statistiques = comportementUtilisateurService.obtenirStatistiquesComportement(userId);
        return ResponseEntity.ok(statistiques);
    }

    // ─────────────────────────────────────────────────────────────────
    //  RECOMMANDATIONS
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/recommandations/user/{userId}")
    public ResponseEntity<List<RecommandationIA>> getRecommandationsForUser(
            @PathVariable Long userId) {
        return execute(() -> ResponseEntity.ok(
                recommandationIAService.getRecommandationsByUserId(userId)));
    }

    @GetMapping("/recommandations/all")
    public ResponseEntity<List<RecommandationIA>> getAllRecommendations() {
        return ResponseEntity.ok(recommandationIAService.getAllRecommandations());
    }

    /**
     * ✅ NOUVEAU endpoint manquant côté backend.
     * Le frontend (RecommandationAdminPanel) appelait DELETE /administrateur/recommandations/{id}
     * qui n'existait pas — ajout ici.
     */
    @DeleteMapping("/recommandations/{id}")
    public ResponseEntity<Void> deleteRecommandation(@PathVariable String id) {
        return execute(() -> {
            recommandationIAService.deleteRecommandationsUser(
                    recommandationIAService.getAllRecommandations()
                            .stream()
                            .filter(r -> r.getId().equals(id))
                            .findFirst()
                            .map(RecommandationIA::getUserId)
                            .orElseThrow(() -> new RuntimeException("Recommandation introuvable : " + id))
            );
            logger.info("Recommandation supprimée : {}", id);
            return ResponseEntity.noContent().<Void>build();
        });
    }
}