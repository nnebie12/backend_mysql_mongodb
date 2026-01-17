package com.example.demo.web.controllersMongoDB;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTO.RecommandationIACreationDTO;
import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.entiesMongodb.enums.Saison;
import com.example.demo.servicesMongoDB.RecommandationIAService;

@RestController
@RequestMapping("/api/v1/recommandations") 
public class RecommandationIAController {

    private final RecommandationIAService recommandationService;

    public RecommandationIAController(RecommandationIAService recommandationService) {
        this.recommandationService = recommandationService;
    }

    @PostMapping
    public ResponseEntity<RecommandationIA> creerRecommandation(
            @RequestParam Long userId,
            @RequestParam String type,
            @RequestBody RecommandationIACreationDTO request,
            @RequestParam(required = false, defaultValue = "0.0") Double score) {
        
        RecommandationIA nouvelleRecommandation = recommandationService.addRecommandation(
                userId, type, request.getRecommandations(), score);
        
        return new ResponseEntity<>(nouvelleRecommandation, HttpStatus.CREATED);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecommandationIA>> getRecommandationsByUserId(@PathVariable Long userId) {
        List<RecommandationIA> recommandations = recommandationService.getRecommandationsByUserId(userId);
        return new ResponseEntity<>(recommandations, HttpStatus.OK);
    }
    
    @PostMapping("/user/{userId}/generer-personnalisee")
    public ResponseEntity<RecommandationIA> genererRecommandationPersonnalisee(@PathVariable Long userId) {
        try {
            RecommandationIA recommandation = recommandationService.genererRecommandationPersonnalisee(userId);
            return new ResponseEntity<>(recommandation, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/user/{userId}/generer-saisonniere")
    public ResponseEntity<RecommandationIA> genererRecommandationSaisonniere(@PathVariable Long userId) {
        try {
            RecommandationIA recommandation = recommandationService.genererRecommandationSaisonniere(userId);
            return new ResponseEntity<>(recommandation, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/user/{userId}/generer-habitudes")
    public ResponseEntity<RecommandationIA> genererRecommandationHabitudes(@PathVariable Long userId) {
        try {
            RecommandationIA recommandation = recommandationService.genererRecommandationHabitudes(userId);
            return new ResponseEntity<>(recommandation, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/user/{userId}/generer-creneau")
    public ResponseEntity<RecommandationIA> genererRecommandationCreneauActuel(@PathVariable Long userId) {
        try {
            RecommandationIA recommandation = recommandationService.genererRecommandationCreneauActuel(userId);
            return new ResponseEntity<>(recommandation, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/user/{userId}/generer-engagement")
    public ResponseEntity<RecommandationIA> genererRecommandationEngagement(@PathVariable Long userId) {
        try {
            RecommandationIA recommandation = recommandationService.genererRecommandationEngagement(userId);
            return new ResponseEntity<>(recommandation, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/user/{userId}/generer-toutes")
    public ResponseEntity<List<RecommandationIA>> genererToutesRecommandations(@PathVariable Long userId) {
        try {
            List<RecommandationIA> recommandations = List.of(
                recommandationService.genererRecommandationPersonnalisee(userId),
                recommandationService.genererRecommandationSaisonniere(userId),
                recommandationService.genererRecommandationHabitudes(userId),
                recommandationService.genererRecommandationCreneauActuel(userId),
                recommandationService.genererRecommandationEngagement(userId)
            );
            return new ResponseEntity<>(recommandations, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<RecommandationIA>> getRecommandationsByUserIdAndType(
            @PathVariable Long userId,
            @PathVariable String type) {
        
        List<RecommandationIA> recommandations = recommandationService.getRecommandationsByUserIdAndType(userId, type);
        return new ResponseEntity<>(recommandations, HttpStatus.OK);
    }

    @PutMapping("/{recommandationId}/utilise")
    public ResponseEntity<RecommandationIA> marquerCommeUtilise(@PathVariable String recommandationId) {
        RecommandationIA recommandation = recommandationService.markAsUsed(recommandationId);
        return new ResponseEntity<>(recommandation, HttpStatus.OK);
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> supprimerRecommandationsUtilisateur(@PathVariable Long userId) {
        recommandationService.deleteRecommandationsUser(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return new ResponseEntity<>("Contrôleur RecommandationIA fonctionne correctement !", HttpStatus.OK);
    }

    @PostMapping("/user/{userId}/generer-hybride")
    public ResponseEntity<RecommandationIA> genererHybride(@PathVariable Long userId) {
        // Cette méthode doit être ajoutée à l'interface RecommandationIAService 
        // pour appeler enhancedRecommendationService.genererRecommandationHybride
        RecommandationIA recommandation = recommandationService.genererRecommandationHybride(userId);
        return new ResponseEntity<>(recommandation, HttpStatus.CREATED);
    }

    @PostMapping("/user/{userId}/generer-saison")
    public ResponseEntity<RecommandationIA> generateSeasonal(
            @PathVariable Long userId) {
        RecommandationIA recommandation = recommandationService.genererRecommandationSaisonniere(userId);
        return new ResponseEntity<>(recommandation, HttpStatus.CREATED);
    }
}