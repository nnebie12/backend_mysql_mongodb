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
import java.util.Objects;
import java.util.Set;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/recommendations")
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

    @GetMapping("/personalized/{userId}")
    public ResponseEntity<?> getPersonalizedRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        logger.info("Requête de recommandations personnalisées pour l'utilisateur {}", userId);

        try {
            List<RecetteInteraction> history = interactionRepo.findByIdUser(userId);

            if (history.isEmpty()) {
                logger.info("Pas d'historique pour l'utilisateur {}, recommandations génériques", userId);
                return ResponseEntity.ok(Map.of(
                    "message", "Aucun historique trouvé, voici des recommandations populaires",
                    "recommendations", getPopularRecipes(limit)
                ));
            }

            List<NoteDocument> userRatings = noteRepo.findByUserId(userId);

            // ✅ findAllWithUser() — un seul SELECT avec JOIN sur userEntity
            List<RecetteEntity> allRecipes = recetteRepo.findAllWithUser();

            List<RecetteResponseDTO> recommendations =
                aiService.getPersonalizedRecommendations(userId, history, allRecipes, userRatings);

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

    @GetMapping("/similar/{recipeId}")
    public ResponseEntity<?> getSimilarRecipes(
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "10") int limit) {

        logger.info("Recherche de recettes similaires à la recette {}", recipeId);

        try {
            RecetteEntity recipe = recetteRepo.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recette non trouvée"));

            // ✅ findAllWithUser()
            List<RecetteEntity> allRecipes = recetteRepo.findAllWithUser();

            List<RecetteResponseDTO> similar = aiService.findSimilarRecipes(recipe, allRecipes);
            similar = similar.stream().limit(limit).collect(Collectors.toList());

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

    @GetMapping("/trends")
    public ResponseEntity<?> getTrends() {
        logger.info("Requête d'analyse des tendances");
        try {
            List<RecetteInteraction> allInteractions = interactionRepo.findAll();
            Map<String, Object> trends = aiService.detectTrends(allInteractions);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            logger.warn("Gemini Error, switching to basic trends: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "trending_categories", List.of("Général"),
                "message", "Tendances basiques (IA indisponible)"
            ));
        }
    }

    @GetMapping("/for-you/{userId}")
    public ResponseEntity<?> getForYouRecommendations(@PathVariable Long userId) {
        logger.info("Génération de recommandations 'Pour vous' pour l'utilisateur {}", userId);
        try {
            List<RecetteInteraction> history = interactionRepo.findByIdUser(userId);

            // ✅ findAllWithUser()
            List<RecetteEntity> allRecipes = recetteRepo.findAllWithUser();
            List<NoteDocument> userRatings = noteRepo.findByUserId(userId);

            Map<String, Object> forYou = new HashMap<>();

            if (!history.isEmpty()) {
                List<RecetteResponseDTO> personalized =
                    aiService.getPersonalizedRecommendations(userId, history, allRecipes, userRatings);
                forYou.put("personalized", personalized.stream().limit(5).collect(Collectors.toList()));
            }

            forYou.put("popular", getPopularRecipes(5));
            forYou.put("newest", allRecipes.stream()
                .sorted((r1, r2) -> r2.getId().compareTo(r1.getId()))
                .limit(5)
                .map(this::toDTO)
                .collect(Collectors.toList()));
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

    @GetMapping("/explore/{userId}")
    public ResponseEntity<?> getExploreRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            List<RecetteInteraction> history = interactionRepo.findByIdUser(userId);

            Set<Long> viewedIds = history.stream()
                    .map(RecetteInteraction::getIdRecette)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<RecetteEntity> unseenRecipes = recetteRepo.findAll().stream()
                    .filter(r -> !viewedIds.contains(r.getId()))
                    .collect(Collectors.toList());

            Collections.shuffle(unseenRecipes);

            List<RecetteResponseDTO> explore = unseenRecipes.stream()
                    .limit(limit).map(this::toDTO).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "user_id", userId,
                    "total_unseen", unseenRecipes.size(),
                    "recipes", explore));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> recordRecommendationFeedback(
            @RequestBody Map<String, Object> feedbackData) {
        try {
            Long userId   = Long.parseLong(feedbackData.get("userId").toString());
            Long recipeId = Long.parseLong(feedbackData.get("recipeId").toString());
            String action = feedbackData.get("action").toString();
            logger.info("Feedback reçu: User {} - Recipe {} - Action {}", userId, recipeId, action);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Feedback enregistré"));
        } catch (Exception e) {
            logger.error("Erreur lors de l'enregistrement du feedback", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors de l'enregistrement du feedback"
            ));
        }
    }

    @GetMapping("/stats/{userId}")
    public ResponseEntity<?> getRecommendationStats(@PathVariable Long userId) {
        try {
            List<RecetteInteraction> history = interactionRepo.findByIdUser(userId);

           
            List<Long> recetteIds = history.stream()
                    .map(RecetteInteraction::getIdRecette)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            List<RecetteEntity> recettes = recetteRepo.findAllById(recetteIds);

            Map<String, Long> typeCount = recettes.stream()
                    .filter(r -> r.getTypeRecette() != null)
                    .collect(Collectors.groupingBy(RecetteEntity::getTypeRecette, Collectors.counting()));

            Map<String, Long> cuisineCount = recettes.stream()
                    .filter(r -> r.getCuisine() != null)
                    .collect(Collectors.groupingBy(RecetteEntity::getCuisine, Collectors.counting()));

            Map<String, Long> difficultyCount = recettes.stream()
                    .filter(r -> r.getDifficulte() != null)
                    .collect(Collectors.groupingBy(RecetteEntity::getDifficulte, Collectors.counting()));

            return ResponseEntity.ok(Map.of(
                    "user_id", userId,
                    "total_interactions", history.size(),
                    "favorite_types", typeCount,
                    "favorite_cuisines", cuisineCount,
                    "difficulty_preference", difficultyCount));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur calcul statistiques"));
        }
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    private List<RecetteResponseDTO> getPopularRecipes(int limit) {
        List<RecetteInteraction> allInteractions = interactionRepo.findAll(PageRequest.of(0, 100)).getContent();

        List<RecetteInteraction> validInteractions = allInteractions.stream()
                .filter(i -> i.getIdRecette() != null)
                .collect(Collectors.toList());

        if (validInteractions.isEmpty()) {
            return recetteRepo.findAll().stream()
                    .sorted(Comparator.comparing(RecetteEntity::getId).reversed())
                    .limit(limit).map(this::toDTO).collect(Collectors.toList());
        }

        Map<Long, Long> recipeInteractionCount = validInteractions.stream()
                .collect(Collectors.groupingBy(RecetteInteraction::getIdRecette, Collectors.counting()));

        List<Long> popularRecipeIds = recipeInteractionCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit).map(Map.Entry::getKey).collect(Collectors.toList());

        return recetteRepo.findAllById(popularRecipeIds).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

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
        
        dto.setPopularite(entity.getPopularite());
        dto.setCategorie(entity.getCategorie());
        dto.setSaison(entity.getSaison());
        dto.setTypeCuisine(entity.getTypeCuisine());
        return dto;
    }
}