package com.example.demo.web.controllersMongoDB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        // Remplacer getBehaviorByUserId par getOrCreateBehavior pour éviter le 404
        ComportementUtilisateur comportement = comportementService.getOrCreateBehavior(userId);
        
        Map<String, Object> analyse = new HashMap<>();
        
        // Initialisation par défaut pour éviter les Maps vides
        analyse.put("userId", userId);
        
        if (comportement.getMetriques() != null) {
            analyse.put("metriques", Map.of(
                "scoreEngagement", Optional.ofNullable(comportement.getMetriques().getScoreEngagement()).orElse(0.0),
                "profilUtilisateur", Optional.ofNullable(comportement.getMetriques().getProfilUtilisateur()).map(Enum::name).orElse("NOUVEAU"),
                "risqueChurn", calculerRisqueChurn(comportement),
                "scoreRecommandation", Optional.ofNullable(comportement.getMetriques().getScoreRecommandation()).orElse(0.0)
            ));
        } else {
            // Fallback si métriques nulles
            analyse.put("metriques", Map.of("scoreEngagement", 0.0, "profilUtilisateur", "NOUVEAU", "risqueChurn", 50.0));
        }

        analyse.put("segmentRFM", determinerSegmentRFM(comportement));
        
        return new ResponseEntity<>(analyse, HttpStatus.OK);
    }
    
    /**
     * Calcule le risque de churn d'un utilisateur
     */
    @GetMapping("/user/{userId}/risque-churn")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR') or (#userId == authentication.principal.id)")
    public ResponseEntity<Map<String, Object>> getRisqueChurn(@PathVariable Long userId) {
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);
        
        if (comportementOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        double risqueChurn = calculerRisqueChurn(comportementOpt.get());
        String niveau = risqueChurn > 70 ? "CRITIQUE" : risqueChurn > 40 ? "MOYEN" : "FAIBLE";
        
        Map<String, Object> result = Map.of(
            "risqueChurn", risqueChurn,
            "niveau", niveau,
            "recommandations", genererRecommandationsChurn(risqueChurn)
        );
        
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
    
    /**
     * Récupère la segmentation RFM d'un utilisateur
     */
    @GetMapping("/user/{userId}/segmentation-rfm")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR') or (#userId == authentication.principal.id)")
    public ResponseEntity<Map<String, Object>> getSegmentationRFM(@PathVariable Long userId) {
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);
        
        if (comportementOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        Map<String, Object> segmentation = determinerSegmentRFM(comportementOpt.get());
        return new ResponseEntity<>(segmentation, HttpStatus.OK);
    }
    
    /**
     * Récupère les actions d'engagement recommandées
     */
    @GetMapping("/user/{userId}/actions-engagement")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR') or (#userId == authentication.principal.id)")
    public ResponseEntity<List<Map<String, String>>> getActionsEngagement(@PathVariable Long userId) {
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);
        
        if (comportementOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        ComportementUtilisateur comportement = comportementOpt.get();
        double scoreEngagement = comportement.getMetriques() != null && comportement.getMetriques().getScoreEngagement() != null
            ? comportement.getMetriques().getScoreEngagement() : 0.0;
        
        List<Map<String, String>> actions = genererActionsEngagement(scoreEngagement, comportement);
        return new ResponseEntity<>(actions, HttpStatus.OK);
    }
    
    // ==================== STATISTIQUES GLOBALES ====================
    
    /**
     * Récupère les statistiques RFM globales pour le dashboard
     */
    @GetMapping("/stats/rfm")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR')")
    public ResponseEntity<Map<String, Integer>> getStatsRFM() {
        try {
            // 🔍 DEBUG
            System.out.println("📊 Calcul des stats RFM...");
            
            List<ComportementUtilisateur> tousUtilisateurs = comportementService.getEngagedUsers(0.0);
            
            System.out.println("👥 Nombre total d'utilisateurs: " + tousUtilisateurs.size());
            
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
                System.out.println("⚠️ Aucun utilisateur trouvé, retour de stats vides");
                return ResponseEntity.ok(statsVides);
            }
            
            Map<String, Integer> stats = Map.of(
                "champions", (champions * 100) / total,
                "fidele", (fideles * 100) / total,
                "risque", (risque * 100) / total,
                "nouveau", (nouveaux * 100) / total
            );
            
            System.out.println("✅ Stats RFM calculées: " + stats);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            System.err.println("❌ Erreur dans getStatsRFM: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("🔍 Recherche utilisateurs engagés avec score > " + scoreMinimum);
            
            List<ComportementUtilisateur> utilisateurs = comportementService.getEngagedUsers(scoreMinimum);
            
            System.out.println("✅ Trouvé " + utilisateurs.size() + " utilisateurs engagés");
            
            return ResponseEntity.ok(mapper.toResponseDtoList(utilisateurs));
            
        } catch (Exception e) {
            System.err.println("❌ Erreur dans getEngagedUsers: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(List.of());
        }
    }
    
    // ==================== MÉTHODES UTILITAIRES PRIVÉES ====================
    
    private double calculerRisqueChurn(ComportementUtilisateur comportement) {
        double risque = 0.0;
        
        if (comportement.getMetriques() == null) return 80.0;
        
        Double scoreEngagement = comportement.getMetriques().getScoreEngagement();
        if (scoreEngagement == null || scoreEngagement < 20) {
            risque = 80.0;
        } else if (scoreEngagement < 40) {
            risque = 60.0;
        } else if (scoreEngagement < 60) {
            risque = 30.0;
        } else {
            risque = 10.0;
        }
        
        // Ajuster selon la dernière activité
        if (comportement.getMetriques().getDerniereActivite() != null) {
            long joursInactivite = java.time.Duration.between(
                comportement.getMetriques().getDerniereActivite(),
                java.time.LocalDateTime.now()
            ).toDays();
            
            if (joursInactivite > 30) risque += 20;
            else if (joursInactivite > 14) risque += 10;
        }
        
        return Math.min(100.0, risque);
    }
    
    private Map<String, Object> determinerSegmentRFM(ComportementUtilisateur comportement) {
        String segment;
        String description;
        
        if (comportement.getMetriques() == null) {
            segment = "NOUVEAU";
            description = "Utilisateur récent sans historique suffisant";
        } else {
            Double score = comportement.getMetriques().getScoreEngagement();
            ProfilUtilisateur profil = comportement.getMetriques().getProfilUtilisateur();
            
            if (score != null && score > 80 && profil == ProfilUtilisateur.FIDELE) {
                segment = "CHAMPION";
                description = "Utilisateur très engagé et fidèle";
            } else if (score != null && score > 60) {
                segment = "FIDELE";
                description = "Utilisateur régulier et engagé";
            } else if (score != null && score > 30) {
                segment = "ACTIF";
                description = "Utilisateur moyennement actif";
            } else {
                segment = "A_RISQUE";
                description = "Utilisateur à réengager";
            }
        }
        
        return Map.of(
            "segment", segment,
            "description", description,
            "scoreEngagement", comportement.getMetriques() != null && comportement.getMetriques().getScoreEngagement() != null
                ? comportement.getMetriques().getScoreEngagement() : 0.0
        );
    }
    
    private List<String> genererRecommandationsChurn(double risqueChurn) {
        if (risqueChurn > 70) {
            return List.of(
                "Envoyer une notification personnalisée immédiate",
                "Proposer du contenu exclusif ou une offre spéciale",
                "Organiser un contact direct (email, SMS)"
            );
        } else if (risqueChurn > 40) {
            return List.of(
                "Relancer avec des recommandations personnalisées",
                "Envoyer un digest hebdomadaire des nouveautés"
            );
        } else {
            return List.of(
                "Continuer l'engagement régulier",
                "Proposer du contenu premium"
            );
        }
    }
    
    private List<Map<String, String>> genererActionsEngagement(double scoreEngagement, ComportementUtilisateur comportement) {
        List<Map<String, String>> actions = new java.util.ArrayList<>();
        
        if (scoreEngagement < 30) {
            actions.add(Map.of(
                "typeRecommandation", "Notification Push",
                "description", "Envoyer une recette personnalisée basée sur ses préférences",
                "priorite", "HAUTE"
            ));
            actions.add(Map.of(
                "typeRecommandation", "Email de Réengagement",
                "description", "Proposer une découverte guidée de nouvelles fonctionnalités",
                "priorite", "HAUTE"
            ));
        } else if (scoreEngagement < 60) {
            actions.add(Map.of(
                "typeRecommandation", "Suggestion Proactive",
                "description", "Recommander des recettes tendance adaptées à ses habitudes",
                "priorite", "MOYENNE"
            ));
        } else {
            actions.add(Map.of(
                "typeRecommandation", "Contenu Premium",
                "description", "Proposer du contenu exclusif pour utilisateurs fidèles",
                "priorite", "BASSE"
            ));
        }
        
        return actions;
    }
}