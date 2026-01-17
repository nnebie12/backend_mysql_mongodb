package com.example.demo.web.controllersMongoDB;

import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.servicesMongoDB.GeminiAIService;
import com.example.demo.servicesMongoDB.RecommandationIAService;
import com.example.demo.servicesMongoDB.PropositionRecommandationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class GeminiAIController {

    private final GeminiAIService geminiAIService;
    private final RecommandationIAService recommandationIAService;
    private final PropositionRecommandationService propositionService;

    public GeminiAIController(GeminiAIService geminiAIService,
                             RecommandationIAService recommandationIAService,
                             PropositionRecommandationService propositionService) {
        this.geminiAIService = geminiAIService;
        this.recommandationIAService = recommandationIAService;
        this.propositionService = propositionService;
    }

    /**
     * Génère une recommandation personnalisée via IA
     */
    @PostMapping("/recommendations/personalized/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<RecommandationIA> generatePersonalizedRecommendation(
            @PathVariable Long userId,
            @RequestBody PersonalizedRequest request) {
        
        try {
            // Appel à Claude AI
            GeminiAIService.AIRecommendationResponse aiResponse = 
                geminiAIService.generatePersonalizedRecommendation(
                    request.getProfil(),
                    request.getScoreEngagement(),
                    request.getCategories(),
                    request.getTypes()
                );

            // Conversion des détails
            List<RecommandationIA.RecommandationDetail> details = 
                aiResponse.getRecommendations().stream()
                    .map(this::convertToRecommandationDetail)
                    .collect(Collectors.toList());

            // Sauvegarde dans MongoDB
            RecommandationIA recommendation = recommandationIAService.addRecommandation(
                userId,
                "PERSONNALISEE",
                details,
                aiResponse.getScore()
            );

            // Créer une proposition
            propositionService.createProposition(userId, recommendation.getId(), 3);

            return new ResponseEntity<>(recommendation, HttpStatus.CREATED);
            
        } catch (Exception e) {
            System.err.println("Erreur génération personnalisée: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Génère une recommandation saisonnière via IA
     */
    @PostMapping("/recommendations/seasonal/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<RecommandationIA> generateSeasonalRecommendation(
            @PathVariable Long userId,
            @RequestBody SeasonalRequest request) {
        
        try {
            GeminiAIService.AIRecommendationResponse aiResponse = 
                geminiAIService.generateSeasonalRecommendation(
                    request.getSaison(),
                    request.getIngredients()
                );

            List<RecommandationIA.RecommandationDetail> details = 
                aiResponse.getRecommendations().stream()
                    .map(this::convertToRecommandationDetail)
                    .collect(Collectors.toList());

            RecommandationIA recommendation = recommandationIAService.addRecommandation(
                userId,
                "SAISONNIERE",
                details,
                aiResponse.getScore()
            );

            propositionService.createProposition(userId, recommendation.getId(), 3);

            return new ResponseEntity<>(recommendation, HttpStatus.CREATED);
            
        } catch (Exception e) {
            System.err.println("Erreur génération saisonnière: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Génère une recommandation basée sur les habitudes via IA
     */
    @PostMapping("/recommendations/habit-based/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<RecommandationIA> generateHabitBasedRecommendation(
            @PathVariable Long userId,
            @RequestBody HabitRequest request) {
        
        try {
            GeminiAIService.AIRecommendationResponse aiResponse = 
                geminiAIService.generateHabitBasedRecommendation(
                    request.getTypeRecette(),
                    request.getTempsPreparation(),
                    request.getDifficulte(),
                    request.getCategoriesPreferees()
                );

            List<RecommandationIA.RecommandationDetail> details = 
                aiResponse.getRecommendations().stream()
                    .map(this::convertToRecommandationDetail)
                    .collect(Collectors.toList());

            RecommandationIA recommendation = recommandationIAService.addRecommandation(
                userId,
                "HABITUDES",
                details,
                aiResponse.getScore()
            );

            propositionService.createProposition(userId, recommendation.getId(), 3);

            return new ResponseEntity<>(recommendation, HttpStatus.CREATED);
            
        } catch (Exception e) {
            System.err.println("Erreur génération habitudes: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Génère une recommandation par créneau horaire via IA
     */
    @PostMapping("/recommendations/timeslot/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<RecommandationIA> generateTimeslotRecommendation(
            @PathVariable Long userId,
            @RequestBody TimeslotRequest request) {
        
        try {
            GeminiAIService.AIRecommendationResponse aiResponse = 
                geminiAIService.generateTimeslotRecommendation(
                    request.getCreneau(),
                    request.getPreferences()
                );

            List<RecommandationIA.RecommandationDetail> details = 
                aiResponse.getRecommendations().stream()
                    .map(this::convertToRecommandationDetail)
                    .collect(Collectors.toList());

            RecommandationIA recommendation = recommandationIAService.addRecommandation(
                userId,
                "CRENEAU_ACTUEL",
                details,
                aiResponse.getScore()
            );

            propositionService.createProposition(userId, recommendation.getId(), 3);

            return new ResponseEntity<>(recommendation, HttpStatus.CREATED);
            
        } catch (Exception e) {
            System.err.println("Erreur génération créneau: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Génère une recommandation d'engagement via IA
     */
    @PostMapping("/recommendations/engagement/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<RecommandationIA> generateEngagementRecommendation(
            @PathVariable Long userId,
            @RequestBody EngagementRequest request) {
        
        try {
            GeminiAIService.AIRecommendationResponse aiResponse = 
                geminiAIService.generateEngagementRecommendation(
                    request.getEngagementScore()
                );

            List<RecommandationIA.RecommandationDetail> details = 
                aiResponse.getRecommendations().stream()
                    .map(this::convertToRecommandationDetail)
                    .collect(Collectors.toList());

            RecommandationIA recommendation = recommandationIAService.addRecommandation(
                userId,
                "ENGAGEMENT",
                details,
                aiResponse.getScore()
            );

            propositionService.createProposition(userId, recommendation.getId(), 3);

            return new ResponseEntity<>(recommendation, HttpStatus.CREATED);
            
        } catch (Exception e) {
            System.err.println("Erreur génération engagement: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Convertit un détail du service IA vers le format MongoDB
     */
    private RecommandationIA.RecommandationDetail convertToRecommandationDetail(
            GeminiAIService.RecommendationDetail source) {
        
        RecommandationIA.RecommandationDetail detail = new RecommandationIA.RecommandationDetail();
        detail.setTitre(source.getTitre());
        detail.setDescription(source.getDescription());
        detail.setLien(source.getLien());
        detail.setTags(source.getTags());
        // Le champ 'raison' peut être ajouté au modèle si nécessaire
        
        return detail;
    }

    // ============= Classes de requête =============

    public static class PersonalizedRequest {
        private String profil;
        private Integer scoreEngagement;
        private List<String> categories;
        private List<String> types;

        // Getters et Setters
        public String getProfil() { return profil; }
        public void setProfil(String profil) { this.profil = profil; }
        public Integer getScoreEngagement() { return scoreEngagement; }
        public void setScoreEngagement(Integer scoreEngagement) { this.scoreEngagement = scoreEngagement; }
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
        public List<String> getTypes() { return types; }
        public void setTypes(List<String> types) { this.types = types; }
    }

    public static class SeasonalRequest {
        private String saison;
        private List<String> ingredients;

        public String getSaison() { return saison; }
        public void setSaison(String saison) { this.saison = saison; }
        public List<String> getIngredients() { return ingredients; }
        public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
    }

    public static class HabitRequest {
        private String typeRecette;
        private String tempsPreparation;
        private String difficulte;
        private List<String> categoriesPreferees;

        public String getTypeRecette() { return typeRecette; }
        public void setTypeRecette(String typeRecette) { this.typeRecette = typeRecette; }
        public String getTempsPreparation() { return tempsPreparation; }
        public void setTempsPreparation(String tempsPreparation) { this.tempsPreparation = tempsPreparation; }
        public String getDifficulte() { return difficulte; }
        public void setDifficulte(String difficulte) { this.difficulte = difficulte; }
        public List<String> getCategoriesPreferees() { return categoriesPreferees; }
        public void setCategoriesPreferees(List<String> categoriesPreferees) { this.categoriesPreferees = categoriesPreferees; }
    }

    public static class TimeslotRequest {
        private String creneau;
        private List<String> preferences;

        public String getCreneau() { return creneau; }
        public void setCreneau(String creneau) { this.creneau = creneau; }
        public List<String> getPreferences() { return preferences; }
        public void setPreferences(List<String> preferences) { this.preferences = preferences; }
    }

    public static class EngagementRequest {
        private Integer engagementScore;

        public Integer getEngagementScore() { return engagementScore; }
        public void setEngagementScore(Integer engagementScore) { this.engagementScore = engagementScore; }
    }
}