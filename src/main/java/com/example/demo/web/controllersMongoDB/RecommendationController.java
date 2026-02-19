package com.example.demo.web.controllersMongoDB;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.DTO.RecetteResponseDTO;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entiesMongodb.RecetteInteraction;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.repositoryMongoDB.NoteMongoRepository;
import com.example.demo.repositoryMongoDB.RecetteInteractionRepository;
import com.example.demo.repositoryMysql.RecetteRepository;
import com.example.demo.servicesMongoDB.GeminiRecommendationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Controller REST pour les recommandations IA
 * Expose les endpoints pour :
 * - Recommandations personnalisées
 * - Recettes similaires
 * - Détection de tendances
 * - Segmentation utilisateurs
 */
@RestController
@RequestMapping("/api/v1/recommendations")
// ⭐ CORRECTION : Suppression de @CrossOrigin car géré globalement dans SecurityConfig
public class RecommendationController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);
    
    @Autowired
    private GeminiRecommendationService aiService;
    
    @Autowired
    private RecetteInteractionRepository interactionRepo;
    
    @Autowired
    private RecetteRepository recetteRepo;
    
    @Autowired
    private NoteMongoRepository noteRepo;
    
    /**
     * GET /api/v1/recommendations/personalized/{userId}
     * Recommandations personnalisées basées sur l'historique de l'utilisateur
     * 
     * @param userId ID de l'utilisateur
     * @param limit Nombre maximum de recommandations (défaut: 10)
     * @return Liste des recettes recommandées
     */
    @GetMapping("/personalized/{userId}")
    public ResponseEntity<?> getPersonalizedRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("Requête de recommandations personnalisées pour l'utilisateur {}", userId);
        
        try {
            // Récupérer l'historique utilisateur
            List<RecetteInteraction> history = 
                interactionRepo.findByIdUser(userId);
            
            if (history.isEmpty()) {
                logger.info("Pas d'historique pour l'utilisateur {}, recommandations génériques", userId);
                return ResponseEntity.ok(Map.of(
                    "message", "Aucun historique trouvé, voici des recommandations populaires",
                    "recommendations", getPopularRecipes(limit)
                ));
            }
            
            // Récupérer les notes de l'utilisateur
            List<NoteDocument> userRatings = noteRepo.findByUserId(userId);
            
            // Toutes les recettes disponibles
            List<RecetteEntity> allRecipes = recetteRepo.findAll();
            
            // Générer les recommandations via IA
            List<RecetteResponseDTO> recommendations = 
                aiService.getPersonalizedRecommendations(userId, history, allRecipes, userRatings);
            
            // Limiter selon le paramètre
            recommendations = recommendations.stream()
                .limit(limit)
                .collect(Collectors.toList());
            
            logger.info("Retour de {} recommandations pour l'utilisateur {}", 
                recommendations.size(), userId);
            
            return ResponseEntity.ok(Map.of(
                "user_id", userId,
                "total_recommendations", recommendations.size(),
                "based_on_interactions", history.size(),
                "recommendations", recommendations
            ));
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération des recommandations pour l'utilisateur {}", userId, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors de la génération des recommandations",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * GET /api/v1/recommendations/similar/{recipeId}
     * Recettes similaires à une recette donnée
     * 
     * @param recipeId ID de la recette de référence
     * @param limit Nombre de recommandations
     * @return Liste des recettes similaires
     */
    @GetMapping("/similar/{recipeId}")
    public ResponseEntity<?> getSimilarRecipes(
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("Recherche de recettes similaires à la recette {}", recipeId);
        
        try {
            RecetteEntity recipe = recetteRepo.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recette non trouvée"));
            
            List<RecetteEntity> allRecipes = recetteRepo.findAll();
            
            // Trouver des recettes similaires via IA
            List<RecetteResponseDTO> similar = aiService.findSimilarRecipes(recipe, allRecipes);
            
            similar = similar.stream()
                .limit(limit)
                .collect(Collectors.toList());
            
            logger.info("Trouvé {} recettes similaires à '{}'", similar.size(), recipe.getTitre());
            
            return ResponseEntity.ok(Map.of(
                "reference_recipe", Map.of(
                    "id", recipe.getId(),
                    "titre", recipe.getTitre(),
                    "type", recipe.getTypeRecette()
                ),
                "total_similar", similar.size(),
                "similar_recipes", similar
            ));
            
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche de recettes similaires", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors de la recherche de recettes similaires",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * GET /api/v1/recommendations/trends
     * Détection des tendances culinaires basée sur les interactions récentes
     * 
     * @return Analyse des tendances avec insights IA
     */
    @GetMapping("/trends")
    public ResponseEntity<?> getTrends() {
        
        logger.info("Requête d'analyse des tendances");
        
        try {
            List<RecetteInteraction> allInteractions = interactionRepo.findAll();
            // Simuler des tendances basiques si le service IA échoue
            Map<String, Object> trends = aiService.detectTrends(allInteractions);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            logger.warn("Gemini Error, switching to basic trends: {}", e.getMessage());
            // Fallback : Tendances basiques sans IA
            return ResponseEntity.ok(Map.of(
                "trending_categories", List.of("Général"),
                "message", "Tendances basiques (IA indisponible)"
            ));
        }
    }
    
    /**
     * GET /api/v1/recommendations/for-you/{userId}
     * "Pour vous" - Mix de recommandations (personnalisées + tendances)
     * 
     * @param userId ID de l'utilisateur
     * @return Mix de recommandations
     */
    @GetMapping("/for-you/{userId}")
    public ResponseEntity<?> getForYouRecommendations(@PathVariable Long userId) {
        
        logger.info("Génération de recommandations 'Pour vous' pour l'utilisateur {}", userId);
        
        try {
            // Récupérer l'historique
            List<RecetteInteraction> history = 
                interactionRepo.findByIdUser(userId);
            
            List<RecetteEntity> allRecipes = recetteRepo.findAll();
            List<NoteDocument> userRatings = noteRepo.findByUserId(userId);
            
            Map<String, Object> forYou = new HashMap<>();
            
            // 1. Recommandations personnalisées (si historique existe)
            if (!history.isEmpty()) {
                List<RecetteResponseDTO> personalized = 
                    aiService.getPersonalizedRecommendations(userId, history, allRecipes, userRatings);
                
                forYou.put("personalized", personalized.stream().limit(5).collect(Collectors.toList()));
            }
            
            // 2. Recettes populaires récentes
            List<RecetteResponseDTO> popular = getPopularRecipes(5);
            forYou.put("popular", popular);
            
            // 3. Nouvelles recettes
            List<RecetteResponseDTO> newest = allRecipes.stream()
                .sorted((r1, r2) -> r2.getId().compareTo(r1.getId())) // Tri par ID décroissant (plus récent)
                .limit(5)
                .map(this::toDTO)
                .collect(Collectors.toList());
            forYou.put("newest", newest);
            
            forYou.put("user_id", userId);
            forYou.put("generated_at", java.time.LocalDateTime.now());
            
            logger.info("Recommandations 'Pour vous' générées pour l'utilisateur {}", userId);
            
            return ResponseEntity.ok(forYou);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération 'Pour vous'", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors de la génération des recommandations",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * GET /api/v1/recommendations/explore/{userId}
     * Recommandations "Découverte" - Recettes que l'utilisateur n'a jamais vues
     * 
     * @param userId ID de l'utilisateur
     * @param limit Nombre de recommandations
     * @return Recettes à découvrir
     */
    @GetMapping("/explore/{userId}")
    public ResponseEntity<?> getExploreRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        
        logger.info("Génération de recommandations 'Découverte' pour l'utilisateur {}", userId);
        
        try {
            // Récupérer les recettes déjà vues
            List<RecetteInteraction> history = 
                interactionRepo.findByIdUser(userId);
            
            java.util.Set<Long> viewedIds = history.stream()
                .map(i -> i.getRecetteEntity().getId())
                .collect(Collectors.toSet());
            
            // Récupérer les recettes non vues
            List<RecetteEntity> unseenRecipes = recetteRepo.findAll().stream()
                .filter(r -> !viewedIds.contains(r.getId()))
                .collect(Collectors.toList());
            
            // Mélanger aléatoirement pour la découverte
            java.util.Collections.shuffle(unseenRecipes);
            
            List<RecetteResponseDTO> explore = unseenRecipes.stream()
                .limit(limit)
                .map(this::toDTO)
                .collect(Collectors.toList());
            
            logger.info("Trouvé {} recettes à découvrir pour l'utilisateur {}", 
                explore.size(), userId);
            
            return ResponseEntity.ok(Map.of(
                "user_id", userId,
                "total_unseen", unseenRecipes.size(),
                "showing", explore.size(),
                "recipes", explore
            ));
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération 'Découverte'", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors de la génération des recommandations",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * POST /api/v1/recommendations/feedback
     * Enregistre le feedback sur une recommandation
     * 
     * @param feedbackData Données du feedback (userId, recipeId, action)
     * @return Confirmation
     */
    @PostMapping("/feedback")
    public ResponseEntity<?> recordRecommendationFeedback(
            @RequestBody Map<String, Object> feedbackData) {
        
        try {
            Long userId = Long.parseLong(feedbackData.get("userId").toString());
            Long recipeId = Long.parseLong(feedbackData.get("recipeId").toString());
            String action = feedbackData.get("action").toString(); // clicked, dismissed, saved
            
            logger.info("Feedback reçu: User {} - Recipe {} - Action {}", 
                userId, recipeId, action);
            
            // Enregistrer l'interaction
            // (Votre logique existante d'enregistrement d'interaction)
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Feedback enregistré"
            ));
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'enregistrement du feedback", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors de l'enregistrement du feedback"
            ));
        }
    }
    
    /**
     * GET /api/v1/recommendations/stats/{userId}
     * Statistiques sur les recommandations pour un utilisateur
     * 
     * @param userId ID de l'utilisateur
     * @return Statistiques
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<?> getRecommendationStats(@PathVariable Long userId) {
        
        try {
            List<RecetteInteraction> history = 
                interactionRepo.findByIdUser(userId);
            
            // Calculer les statistiques
            Map<String, Long> typeCount = history.stream()
                .collect(Collectors.groupingBy(
                    i -> i.getRecetteEntity().getTypeRecette().toString(),
                    Collectors.counting()
                ));
            
            Map<String, Long> cuisineCount = history.stream()
                .filter(i -> i.getRecetteEntity().getCuisine() != null)
                .collect(Collectors.groupingBy(
                    i -> i.getRecetteEntity().getCuisine(),
                    Collectors.counting()
                ));
            
            Map<String, Long> difficultyCount = history.stream()
                .collect(Collectors.groupingBy(
                    i -> i.getRecetteEntity().getDifficulte().toString(),
                    Collectors.counting()
                ));
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("user_id", userId);
            stats.put("total_interactions", history.size());
            stats.put("favorite_types", typeCount);
            stats.put("favorite_cuisines", cuisineCount);
            stats.put("difficulty_preference", difficultyCount);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Erreur lors du calcul des statistiques", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors du calcul des statistiques"
            ));
        }
    }
    
    // ========== Méthodes utilitaires ==========
    
    /**
     * Récupère les recettes populaires (basé sur le nombre d'interactions)
     */
    private List<RecetteResponseDTO> getPopularRecipes(int limit) {
        List<RecetteInteraction> allInteractions = interactionRepo.findAll(PageRequest.of(0, 100)).getContent();
        
        // ⭐ CORRECTION : Filtrer les interactions avec recetteEntity null
        List<RecetteInteraction> validInteractions = allInteractions.stream()
            .filter(i -> i.getRecetteEntity() != null)
            .collect(Collectors.toList());
        
        // Si aucune interaction valide, retourner les recettes les plus récentes
        if (validInteractions.isEmpty()) {
            logger.info("Aucune interaction valide, retour des recettes récentes");
            return recetteRepo.findAll().stream()
                .sorted((r1, r2) -> r2.getId().compareTo(r1.getId()))
                .limit(limit)
                .map(this::toDTO)
                .collect(Collectors.toList());
        }
        
        // Compter les interactions par recette
        Map<Long, Long> recipeInteractionCount = validInteractions.stream()
            .collect(Collectors.groupingBy(
                i -> i.getRecetteEntity().getId(),
                Collectors.counting()
            ));
        
        // Trier par popularité
        List<Long> popularRecipeIds = recipeInteractionCount.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Récupérer les recettes
        return recetteRepo.findAllById(popularRecipeIds).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Conversion Entity vers DTO
     */
    private RecetteResponseDTO toDTO(RecetteEntity entity) {
    	RecetteResponseDTO dto = new RecetteResponseDTO();
        dto.setId(entity.getId());
        dto.setTitre(entity.getTitre());
        dto.setDescription(entity.getDescription());
        dto.setTempsPreparation(entity.getTempsPreparation());
        dto.setTempsCuisson(entity.getTempsCuisson());
        dto.setDifficulte(entity.getDifficulte());
        dto.setTypeRecette(entity.getTypeRecette());
        dto.setCuisine(entity.getCuisine());
        dto.setImageUrl(entity.getImageUrl());
        dto.setVegetarien(entity.getVegetarien());
        return dto;
    }
}