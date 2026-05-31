package com.example.demo.web.controllersMongoDB;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.DTO.AnalysePatternsDTO;
import com.example.demo.DTO.ComportementUtilisateurRequestDTO;
import com.example.demo.DTO.ComportementUtilisateurResponseDTO;
import com.example.demo.entiesMongodb.ComportementUtilisateur;
import com.example.demo.entiesMongodb.HistoriqueRecherche;
import com.example.demo.entiesMongodb.enums.ProfilUtilisateur;
import com.example.demo.servicesMongoDB.ComportementUtilisateurService;
import com.example.demo.web.mapper.ComportementUtilisateurMapper;

@RestController
@RequestMapping("/api/v1/comportement-utilisateur")
public class ComportementUtilisateurController {

    private static final Logger logger = LoggerFactory.getLogger(ComportementUtilisateurController.class);
    
    private final ComportementUtilisateurService comportementService;
    private final ComportementUtilisateurMapper mapper;

    public ComportementUtilisateurController(
            ComportementUtilisateurService comportementService,
            ComportementUtilisateurMapper mapper) {
        this.comportementService = comportementService;
        this.mapper = mapper;
    }
    
    // ==================== CRUD BASIQUE ====================
    
    @PostMapping
    public ResponseEntity<ComportementUtilisateurResponseDTO> createBehavior(@RequestParam Long userId) {
        ComportementUtilisateur comportement = comportementService.createBehavior(userId);
        return new ResponseEntity<>(mapper.toResponseDto(comportement), HttpStatus.CREATED);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ComportementUtilisateurResponseDTO> getBehaviorByUserId(@PathVariable Long userId) {
        Optional<ComportementUtilisateur> comportement = comportementService.getBehaviorByUserId(userId);
        return comportement.map(c -> new ResponseEntity<>(mapper.toResponseDto(c), HttpStatus.OK))
                           .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/user/{userId}/or-create")
    public ResponseEntity<ComportementUtilisateurResponseDTO> getOrCreateBehavior(@PathVariable Long userId) {
        ComportementUtilisateur comportement = comportementService.getOrCreateBehavior(userId);
        return new ResponseEntity<>(mapper.toResponseDto(comportement), HttpStatus.OK);
    }
    
    @PutMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR')")
    public ResponseEntity<ComportementUtilisateurResponseDTO> updateBehaviorPartial(
            @PathVariable Long userId,
            @RequestBody ComportementUtilisateurRequestDTO comportementDTO) {
        Optional<ComportementUtilisateur> existingComportementOpt = comportementService.getBehaviorByUserId(userId);
        if (existingComportementOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        ComportementUtilisateur existingComportement = existingComportementOpt.get();
        mapper.updateEntityFromRequestDto(comportementDTO, existingComportement);
        ComportementUtilisateur comportementMisAJour = comportementService.updateBehavior(existingComportement);
        
        return new ResponseEntity<>(mapper.toResponseDto(comportementMisAJour), HttpStatus.OK);
    }
    
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteUserBehavior(@PathVariable Long userId) {
        comportementService.deleteUserBehavior(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    // ==================== MÉTRIQUES & ANALYSE ====================
    
    /**
     * Déclenche l'analyse comportementale complète pour un utilisateur
     */
    @PostMapping("/user/{userId}/analyser")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR')")
    public ResponseEntity<ComportementUtilisateurResponseDTO> analyserComportement(@PathVariable Long userId) {
        ComportementUtilisateur comportement = comportementService.analyserPatterns(userId);
        return new ResponseEntity<>(mapper.toResponseDto(comportement), HttpStatus.OK);
    }
    
    /**
     * Récupère l'analyse détaillée des patterns
     */
    @GetMapping("/user/{userId}/patterns")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR') or (#userId == authentication.principal.id)")
    public ResponseEntity<AnalysePatternsDTO> getPatterns(@PathVariable Long userId) {
        AnalysePatternsDTO patterns = comportementService.analyserPatternsDTO(userId);
        return new ResponseEntity<>(patterns, HttpStatus.OK);
    }
    
    /**
     * Récupère les statistiques comportementales détaillées
     */
    @GetMapping("/user/{userId}/statistiques")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR') or (#userId == authentication.principal.id)")
    public ResponseEntity<Map<String, Object>> getStatistiques(@PathVariable Long userId) {
        Map<String, Object> stats = comportementService.obtenirStatistiquesComportement(userId);
        return new ResponseEntity<>(stats, HttpStatus.OK);
    }
    
    /**
     * Rafraîchit les métriques depuis les interactions et recherches
     */
    @PostMapping("/user/{userId}/refresh-metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR')")
    public ResponseEntity<Void> updateMetrics(@PathVariable Long userId) {
        comportementService.updateMetrics(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    /**
     * Récupère l'analyse avancée (RFM, Churn, Patterns)
     */
    @GetMapping("/user/{userId}/analyse-avancee")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR') or (#userId == authentication.principal.id)")
    public ResponseEntity<Map<String, Object>> getAnalyseAvancee(@PathVariable Long userId) {
        return new ResponseEntity<>(comportementService.analyserComportementAvance(userId), HttpStatus.OK);
    }
    
    /**
     * Calcule le risque de churn d'un utilisateur
     */
    @GetMapping("/user/{userId}/risque-churn")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR') or (#userId == authentication.principal.id)")
    public ResponseEntity<Map<String, Object>> getRisqueChurn(@PathVariable Long userId) {
        return new ResponseEntity<>(comportementService.obtenirRisqueChurn(userId), HttpStatus.OK);
    }
    
    /**
     * Récupère la segmentation RFM d'un utilisateur
     */
    @GetMapping("/user/{userId}/segmentation-rfm")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR') or (#userId == authentication.principal.id)")
    public ResponseEntity<Map<String, Object>> getSegmentationRFM(@PathVariable Long userId) {
        return new ResponseEntity<>(comportementService.obtenirSegmentRFM(userId), HttpStatus.OK);
    }
    
    /**
     * Récupère les actions d'engagement recommandées
     */
    @GetMapping("/user/{userId}/actions-engagement")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR') or (#userId == authentication.principal.id)")
    public ResponseEntity<List<Map<String, String>>> getActionsEngagement(@PathVariable Long userId) {
        return new ResponseEntity<>(comportementService.obtenirActionsEngagement(userId), HttpStatus.OK);
    }
    
    // ==================== STATISTIQUES GLOBALES ====================

    /**
     * Endpoint alias pour le frontend userBehaviorService.js
     * qui appelle GET /api/v1/comportement-utilisateur/stats-globales.
     * Délègue au service d'analyse avancée.
     */
    @GetMapping("/stats-globales")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR')")
    public ResponseEntity<Map<String, Object>> getStatsGlobales() {
        List<ComportementUtilisateur> tous = comportementService.getEngagedUsers(0.0);
        long actifs  = tous.stream().filter(c -> c.getMetriques() != null
            && c.getMetriques().getScoreEngagement() != null
            && c.getMetriques().getScoreEngagement() >= 50).count();
        long inactifs = tous.size() - actifs;
        return ResponseEntity.ok(Map.of(
            "totalUtilisateurs", tous.size(),
            "actifs", actifs,
            "inactifs", inactifs
        ));
    }

    @GetMapping("/stats/rfm")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR')")
    public ResponseEntity<Map<String, Integer>> getStatsRFM() {
        try {
            logger.info("Calcul des stats RFM");
            
            List<ComportementUtilisateur> tousUtilisateurs = comportementService.getEngagedUsers(0.0);
            
            logger.info("Nombre total d'utilisateurs: {}", tousUtilisateurs.size());
            
            int champions = 0, fideles = 0, risque = 0, nouveaux = 0;
            
            for (ComportementUtilisateur comp : tousUtilisateurs) {
                if (comp.getMetriques() == null || comp.getMetriques().getProfilUtilisateur() == null) {
                    nouveaux++;
                    continue;
                }
                
                switch (comp.getMetriques().getProfilUtilisateur()) {
                    case FIDELE:
                        Double scoreEngagement = comp.getMetriques().getScoreEngagement();
                        if (scoreEngagement != null && scoreEngagement > 80) {
                            champions++;
                        } else {
                            fideles++;
                        }
                        break;
                    case ACTIF:
                        fideles++;
                        break;
                    case OCCASIONNEL:
                    case DEBUTANT:
                        risque++;
                        break;
                    case NOUVEAU:
                    default:
                        nouveaux++;
                }
            }
            
            int total = tousUtilisateurs.size();
            if (total == 0) {
                // ✅ Si aucun utilisateur, retourner des valeurs par défaut
                Map<String, Integer> statsVides = Map.of(
                    "champions", 0,
                    "fidele", 0,
                    "risque", 0,
                    "nouveau", 0
                );
                logger.warn("Aucun utilisateur trouvé, retour de stats vides");
                return ResponseEntity.ok(statsVides);
            }
            
            Map<String, Integer> stats = Map.of(
                "champions", (champions * 100) / total,
                "fidele", (fideles * 100) / total,
                "risque", (risque * 100) / total,
                "nouveau", (nouveaux * 100) / total
            );
            
            logger.info("Stats RFM calculées: {}", stats);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Erreur dans getStatsRFM: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "champions", 0,
                "fidele", 0,
                "risque", 0,
                "nouveau", 0
            ));
        }
    }
    
    // ==================== RECHERCHES & INTERACTIONS ====================
    
    @PostMapping("/user/{userId}/record-search")
    public ResponseEntity<ComportementUtilisateurResponseDTO> recordSearch(
            @PathVariable Long userId,
            @RequestParam String terme,
            @RequestParam(required = false) Integer nombreResultats,
            @RequestParam(required = false) Boolean rechercheFructueuse,
            @RequestBody(required = false) List<HistoriqueRecherche.Filtre> filtres) {
        
        ComportementUtilisateur comportement = comportementService.recordSearch(
            userId, terme, filtres, nombreResultats, rechercheFructueuse);
        return new ResponseEntity<>(mapper.toResponseDto(comportement), HttpStatus.OK);
    }
    
    @GetMapping("/user/{userId}/frequent-terms")
    public ResponseEntity<List<String>> getFrequentSearchTerms(@PathVariable Long userId) {
        List<String> termes = comportementService.getFrequentSearchTerms(userId);
        return new ResponseEntity<>(termes, HttpStatus.OK);
    }
    
    // ==================== FILTRES PAR PROFIL ====================
    
    @GetMapping("/profil/{profil}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComportementUtilisateurResponseDTO>> getUsersByProfile(
            @PathVariable ProfilUtilisateur profil) {
        List<ComportementUtilisateur> utilisateurs = comportementService.getUsersByProfile(profil);
        return new ResponseEntity<>(mapper.toResponseDtoList(utilisateurs), HttpStatus.OK);
    }
    
    @GetMapping("/engaged")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR')")
    public ResponseEntity<List<ComportementUtilisateurResponseDTO>> getEngagedUsers(
        @RequestParam(defaultValue = "50.0") Double scoreMinimum
    ) {
        try {
            logger.info("Recherche utilisateurs engagés avec score > {}", scoreMinimum);
            
            List<ComportementUtilisateur> utilisateurs = comportementService.getEngagedUsers(scoreMinimum);
            
            logger.info("Trouvé {} utilisateurs engagés", utilisateurs.size());
            
            return ResponseEntity.ok(mapper.toResponseDtoList(utilisateurs));
            
        } catch (Exception e) {
            logger.error("Erreur dans getEngagedUsers: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(List.of());
        }
    }
    
}
