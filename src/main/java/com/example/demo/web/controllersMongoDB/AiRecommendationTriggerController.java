package com.example.demo.web.controllersMongoDB;

import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.servicesMongoDB.RecommandationIAService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-recommendations") 
public class AiRecommendationTriggerController {

    private final RecommandationIAService recommandationIAService;

    public AiRecommendationTriggerController(RecommandationIAService recommandationIAService) {
        this.recommandationIAService = recommandationIAService;
    }

    /**
     * Déclenche la génération d'une recommandation personnalisée pour un utilisateur.
     * Nécessite le rôle 'ADMIN'.
     * @param userId L'ID de l'utilisateur pour lequel générer la recommandation.
     * @return La recommandation IA générée.
     */
    @PostMapping("/generate/personalized/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<RecommandationIA> generatePersonalizedRecommendation(@PathVariable Long userId) {
        try {
            RecommandationIA recommendation = recommandationIAService.genererRecommandationPersonnalisee(userId);
            return new ResponseEntity<>(recommendation, HttpStatus.CREATED);
        } catch (Exception e) {
            // Dans une application réelle, vous voudriez logguer l'exception
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Déclenche la génération d'une recommandation saisonnière pour un utilisateur.
     * Nécessite le rôle 'ADMIN'.
     * @param userId L'ID de l'utilisateur pour lequel générer la recommandation.
     * @return La recommandation IA générée.
     */
    @PostMapping("/generate/seasonal/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<RecommandationIA> generateSeasonalRecommendation(@PathVariable Long userId) {
        try {
            RecommandationIA recommendation = recommandationIAService.genererRecommandationSaisonniere(userId);
            return new ResponseEntity<>(recommendation, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Déclenche la génération d'une recommandation basée sur les habitudes de navigation pour un utilisateur.
     * Nécessite le rôle 'ADMIN'.
     * @param userId L'ID de l'utilisateur pour lequel générer la recommandation.
     * @return La recommandation IA générée.
     */
    @PostMapping("/generate/habit-based/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<RecommandationIA> generateHabitBasedRecommendation(@PathVariable Long userId) {
        try {
            RecommandationIA recommendation = recommandationIAService.genererRecommandationHabitudes(userId);
            return new ResponseEntity<>(recommendation, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Déclenche la génération d'une recommandation pour le créneau horaire actuel d'un utilisateur.
     * Nécessite le rôle 'ADMIN'.
     * @param userId L'ID de l'utilisateur pour lequel générer la recommandation.
     * @return La recommandation IA générée.
     */
    @PostMapping("/generate/timeslot/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<RecommandationIA> generateTimeslotRecommendation(@PathVariable Long userId) {
        try {
            RecommandationIA recommendation = recommandationIAService.genererRecommandationCreneauActuel(userId);
            return new ResponseEntity<>(recommendation, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Déclenche la génération d'une recommandation d'amélioration de l'engagement pour un utilisateur.
     * Nécessite le rôle 'ADMIN'.
     * @param userId L'ID de l'utilisateur pour lequel générer la recommandation.
     * @return La recommandation IA générée.
     */
    @PostMapping("/generate/engagement/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<RecommandationIA> generateEngagementRecommendation(@PathVariable Long userId) {
        try {
            RecommandationIA recommendation = recommandationIAService.genererRecommandationEngagement(userId);
            return new ResponseEntity<>(recommendation, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint générique pour déclencher n'importe quel type de recommandation par l'IA.
     * Nécessite le rôle 'ADMIN'.
     * @param userId L'ID de l'utilisateur.
     * @param type Le type de recommandation à générer (PERSONNALISEE, SAISONNIERE, HABITUDES, CRENEAU_ACTUEL, ENGAGEMENT).
     * @return La recommandation IA générée.
     */
    @PostMapping("/generate/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<RecommandationIA> generateRecommendationByType(
            @PathVariable Long userId,
            @RequestParam String type) {
        try {
            RecommandationIA recommendation;
            switch (type.toUpperCase()) {
                case "PERSONNALISEE":
                    recommendation = recommandationIAService.genererRecommandationPersonnalisee(userId);
                    break;
                case "SAISONNIERE":
                    recommendation = recommandationIAService.genererRecommandationSaisonniere(userId);
                    break;
                case "HABITUDES":
                    recommendation = recommandationIAService.genererRecommandationHabitudes(userId);
                    break;
                case "CRENEAU_ACTUEL":
                    recommendation = recommandationIAService.genererRecommandationCreneauActuel(userId);
                    break;
                case "ENGAGEMENT":
                    recommendation = recommandationIAService.genererRecommandationEngagement(userId);
                    break;
                default:
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(recommendation, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
