package com.example.demo.web.controllersMongoDB;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTO.RecetteResponseDTO;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entiesMongodb.RecetteInteraction;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.repositoryMongoDB.NoteMongoRepository;
import com.example.demo.repositoryMongoDB.RecetteInteractionRepository;
import com.example.demo.repositoryMysql.RecetteRepository;
import com.example.demo.servicesMongoDB.RecommandationIAService;
import com.example.demo.servicesMongoDB.RecetteInteractionService;
import com.example.demo.web.mapper.RecetteMapper;

@RestController
@RequestMapping({"/api/v1/recommendations", "/api/v1/ai/recommendations"})
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    private final RecommandationIAService recommandationService;
    private final RecetteInteractionRepository interactionRepo;
    private final RecetteRepository recetteRepo;
    private final RecetteMapper recetteMapper;
    private final NoteMongoRepository noteRepo;
    private final RecetteInteractionService recetteInteractionService;

    public RecommendationController(RecommandationIAService recommandationService,
                                    RecetteInteractionRepository interactionRepo,
                                    RecetteRepository recetteRepo,
                                    RecetteMapper recetteMapper,
                                    NoteMongoRepository noteRepo,
                                    RecetteInteractionService recetteInteractionService) {
        this.recommandationService = recommandationService;
        this.interactionRepo = interactionRepo;
        this.recetteRepo = recetteRepo;
        this.recetteMapper = recetteMapper;
        this.noteRepo = noteRepo;
        this.recetteInteractionService = recetteInteractionService;
    }

    private <T> ResponseEntity<T> execute(Supplier<ResponseEntity<T>> action) {
        try {
            return action.get();
        } catch (Exception e) {
            logger.error("Recommendation operation failed: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/personalized/{userId}")
    public ResponseEntity<?> getPersonalizedRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        logger.info("Requête de recommandations personnalisées pour l'utilisateur {}", userId);
        return execute(() -> {
            List<RecetteInteraction> history = interactionRepo.findByIdUser(userId);
            if (history.isEmpty()) {
                logger.info("Pas d'historique pour l'utilisateur {}, recommandations génériques", userId);
                return ResponseEntity.ok(Map.of(
                    "message", "Aucun historique trouvé, voici des recommandations populaires",
                    "recommendations", getPopularRecipes(limit)
                ));
            }
            List<NoteDocument> userRatings = noteRepo.findByUserId(userId);
            List<RecetteEntity> allRecipes = recetteRepo.findAllWithUser();
            List<RecetteResponseDTO> recommendations =
                recommandationService.getPersonalizedRecommendations(userId, history, allRecipes, userRatings)
                    .stream().limit(limit).collect(Collectors.toList());
            logger.info("Retour de {} recommandations pour l'utilisateur {}", recommendations.size(), userId);
            return ResponseEntity.ok(Map.of(
                "user_id", userId,
                "total_recommendations", recommendations.size(),
                "based_on_interactions", history.size(),
                "recommendations", recommendations
            ));
        });
    }

    // Alias POST pour la compatibilité avec le frontend aiRecommendationService.js
    // qui appelle POST /ai/recommendations/personalized/{userId}
    @PostMapping("/personalized/{userId}")
    public ResponseEntity<?> getPersonalizedRecommendationsPost(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return getPersonalizedRecommendations(userId, limit);
    }

    // Alias frontend seasonal/habit-based exposés via le préfixe /api/v1/ai/recommendations
    @PostMapping("/seasonal/{userId}")
    public ResponseEntity<?> getSeasonalRecommendationsAiAlias(@PathVariable Long userId) {
        return execute(() -> new ResponseEntity<>(
            recommandationService.genererRecommandationSaisonniere(userId),
            HttpStatus.CREATED
        ));
    }

    @PostMapping("/habit-based/{userId}")
    public ResponseEntity<?> getHabitBasedRecommendationsAiAlias(@PathVariable Long userId) {
        return execute(() -> new ResponseEntity<>(
            recommandationService.genererRecommandationHabitudes(userId),
            HttpStatus.CREATED
        ));
    }

    @GetMapping("/similar/{recipeId}")
    public ResponseEntity<?> getSimilarRecipes(
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "10") int limit) {
        logger.info("Recherche de recettes similaires à la recette {}", recipeId);
        return execute(() -> {
            RecetteEntity recipe = recetteRepo.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recette non trouvée"));
            List<RecetteEntity> allRecipes = recetteRepo.findAllWithUser();
            List<RecetteResponseDTO> similar = recommandationService.findSimilarRecipes(recipe, allRecipes)
                .stream().limit(limit).collect(Collectors.toList());
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
        });
    }

    @GetMapping("/trends")
    public ResponseEntity<?> getTrends() {
        logger.info("Requête d'analyse des tendances");
        return execute(() -> {
            List<RecetteInteraction> allInteractions = interactionRepo.findAll();
            Map<String, Object> trends = recommandationService.detectTrends(allInteractions);
            return ResponseEntity.ok(trends);
        });
    }

    @GetMapping("/for-you/{userId}")
    public ResponseEntity<?> getForYouRecommendations(@PathVariable Long userId) {
        logger.info("Génération de recommandations 'Pour vous' pour l'utilisateur {}", userId);
        return execute(() -> {
            List<RecetteInteraction> history = interactionRepo.findByIdUser(userId);
            List<RecetteEntity> allRecipes = recetteRepo.findAllWithUser();
            List<NoteDocument> userRatings = noteRepo.findByUserId(userId);
            Map<String, Object> forYou = new HashMap<>();
            if (!history.isEmpty()) {
                List<RecetteResponseDTO> personalized =
                    recommandationService.getPersonalizedRecommendations(userId, history, allRecipes, userRatings);
                forYou.put("personalized", personalized.stream().limit(5).collect(Collectors.toList()));
            }
            forYou.put("popular", getPopularRecipes(5));
            forYou.put("newest", allRecipes.stream()
                .sorted((r1, r2) -> r2.getId().compareTo(r1.getId()))
                .limit(5)
                .map(recetteMapper::toResponseDto)
                .collect(Collectors.toList()));
            forYou.put("user_id", userId);
            forYou.put("generated_at", java.time.LocalDateTime.now());
            return ResponseEntity.ok(forYou);
        });
    }

    @GetMapping("/explore/{userId}")
    public ResponseEntity<?> getExploreRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        return execute(() -> {
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
                .limit(limit).map(recetteMapper::toResponseDto).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of(
                "user_id", userId,
                "total_unseen", unseenRecipes.size(),
                "recipes", explore));
        });
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> recordRecommendationFeedback(
            @RequestBody Map<String, Object> feedbackData) {
        return execute(() -> {
            Long userId   = Long.valueOf(feedbackData.get("userId").toString());
            Long recipeId = Long.valueOf(feedbackData.get("recipeId").toString());
            String action = feedbackData.get("action").toString();
            RecetteInteraction interaction = new RecetteInteraction();
            interaction.setIdUser(userId);
            interaction.setIdRecette(recipeId);
            interaction.setTypeInteraction(action.toUpperCase());
            interaction.setDateInteraction(java.time.LocalDateTime.now());
            interaction.setSourceInteraction("RECOMMENDATION");
            recetteInteractionService.save(interaction);
            logger.info("Feedback persisté : User {} - Recipe {} - Action {}", userId, recipeId, action);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Feedback enregistré"));
        });
    }

    @GetMapping("/stats/{userId}")
    public ResponseEntity<?> getRecommendationStats(@PathVariable Long userId) {
        return execute(() -> {
            List<RecetteInteraction> history = interactionRepo.findByIdUser(userId);
            List<Long> recetteIds = history.stream()
                .map(RecetteInteraction::getIdRecette)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
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
        });
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
                    .limit(limit).map(recetteMapper::toResponseDto).collect(Collectors.toList());
        }

        Map<Long, Long> recipeInteractionCount = validInteractions.stream()
                .collect(Collectors.groupingBy(RecetteInteraction::getIdRecette, Collectors.counting()));

        List<Long> popularRecipeIds = recipeInteractionCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit).map(Map.Entry::getKey).collect(Collectors.toList());

        return recetteRepo.findAllById(popularRecipeIds).stream()
                .map(recetteMapper::toResponseDto).collect(Collectors.toList());
    }
}