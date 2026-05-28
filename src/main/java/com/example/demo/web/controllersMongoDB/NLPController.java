package com.example.demo.web.controllersMongoDB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTO.RecetteResponseDTO;
import com.example.demo.entiesMongodb.CommentaireDocument;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.repositoryMongoDB.CommentaireMongoRepository;
import com.example.demo.repositoryMysql.RecetteRepository;
import com.example.demo.servicesImplMongoDB.RecipeNLPService;
import com.example.demo.web.mapper.RecetteMapper;

@RestController
@RequestMapping("/api/v1/nlp")
public class NLPController {

    private static final Logger logger = LoggerFactory.getLogger(NLPController.class);

    private final RecipeNLPService nlpService;
    private final RecetteRepository recetteRepo;
    private final RecetteMapper recetteMapper;
    private final CommentaireMongoRepository commentaireRepo;

    public NLPController(RecipeNLPService nlpService,
                         RecetteRepository recetteRepo,
                         RecetteMapper recetteMapper,
                         CommentaireMongoRepository commentaireRepo) {
        this.nlpService = nlpService;
        this.recetteRepo = recetteRepo;
        this.recetteMapper = recetteMapper;
        this.commentaireRepo = commentaireRepo;
    }

    private <T> ResponseEntity<T> execute(Supplier<ResponseEntity<T>> action) {
        try {
            return action.get();
        } catch (Exception e) {
            logger.error("NLP operation failed: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/search/semantic")
    public ResponseEntity<?> semanticSearch(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La requête ne peut pas être vide"));
        }
        return execute(() -> {
            List<RecetteEntity> allRecipes = recetteRepo.findAll();
            logger.info("Nombre de recettes récupérées pour NLP : {}", allRecipes.size());
            if (allRecipes.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "query", query,
                    "total_results", 0,
                    "message", "La base de données MySQL est vide. Importez le CSV d'abord."
                ));
            }
            List<RecetteEntity> results = nlpService.semanticSearch(query, allRecipes, 10);
            List<RecetteResponseDTO> dtos = results.stream()
                .map(recetteMapper::toResponseDto)
                .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of(
                "query", query,
                "total_results", dtos.size(),
                "results", dtos
            ));
        });
    }

    @GetMapping("/users/{userId}/insights")
    public ResponseEntity<?> getUserNLPInsights(@PathVariable Long userId) {
        return execute(() -> {
            Map<String, Object> insights = new HashMap<>();
            insights.put("userId", userId);
            insights.put("cacheSize", nlpService.getCacheSize());
            return ResponseEntity.ok(insights);
        });
    }

    @GetMapping("/similar/{recipeId}")
    public ResponseEntity<?> findSimilarByEmbeddings(
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "10") int limit) {
        logger.info("Recherche recettes similaires (NLP) à la recette {}", recipeId);
        return execute(() -> {
            RecetteEntity targetRecipe = recetteRepo.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recette non trouvée"));
            List<RecetteEntity> allRecipes = recetteRepo.findAll();
            List<RecetteEntity> similar = nlpService.findMostSimilarRecipes(targetRecipe, allRecipes, limit);
            List<Map<String, Object>> resultsWithScores = similar.stream()
                .map(recipe -> {
                    double similarity = nlpService.calculateCosineSimilarity(targetRecipe, recipe);
                    Map<String, Object> result = new HashMap<>();
                    result.put("recipe", recetteMapper.toResponseDto(recipe));
                    result.put("similarity_score", Math.round(similarity * 100) / 100.0);
                    return result;
                })
                .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of(
                "reference_recipe", Map.of("id", targetRecipe.getId(), "titre", targetRecipe.getTitre()),
                "total_similar", resultsWithScores.size(),
                "similar_recipes", resultsWithScores
            ));
        });
    }

    @PostMapping("/sentiment")
    public ResponseEntity<?> analyzeSentiment(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le texte ne peut pas être vide"));
        }
        logger.info("Analyse de sentiment");
        return execute(() -> {
            double sentimentScore = nlpService.analyzeSentiment(text);
            String sentiment;
            if (sentimentScore > 0.5)       sentiment = "Très positif";
            else if (sentimentScore > 0.2)  sentiment = "Positif";
            else if (sentimentScore > -0.2) sentiment = "Neutre";
            else if (sentimentScore > -0.5) sentiment = "Négatif";
            else                            sentiment = "Très négatif";
            return ResponseEntity.ok(Map.of(
                "text", text,
                "sentiment_score", Math.round(sentimentScore * 100) / 100.0,
                "sentiment_label", sentiment
            ));
        });
    }

    @GetMapping("/sentiment/recipe/{recipeId}")
    public ResponseEntity<?> getRecipeSentiment(@PathVariable Long recipeId) {
        logger.info("Calcul sentiment pour recette {}", recipeId);
        return execute(() -> {
            RecetteEntity recipe = recetteRepo.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recette non trouvée"));
            List<CommentaireDocument> commentaires = commentaireRepo.findByRecetteId(recipeId);
            if (commentaires.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "recipe_id", recipeId,
                    "recipe_titre", recipe.getTitre(),
                    "total_comments", 0,
                    "average_sentiment", 0.0,
                    "sentiment_label", "Aucun commentaire"
                ));
            }
            double avgSentiment = nlpService.calculateAverageSentiment(commentaires);
            String sentimentLabel;
            if (avgSentiment > 0.5)       sentimentLabel = "Très apprécié";
            else if (avgSentiment > 0.2)  sentimentLabel = "Apprécié";
            else if (avgSentiment > -0.2) sentimentLabel = "Mitigé";
            else                          sentimentLabel = "Peu apprécié";
            return ResponseEntity.ok(Map.of(
                "recipe_id", recipeId,
                "recipe_titre", recipe.getTitre(),
                "total_comments", commentaires.size(),
                "average_sentiment", Math.round(avgSentiment * 100) / 100.0,
                "sentiment_label", sentimentLabel
            ));
        });
    }

    @GetMapping("/keywords/{recipeId}")
    public ResponseEntity<?> extractKeywords(@PathVariable Long recipeId) {
        logger.info("Extraction mots-clés pour recette {}", recipeId);
        return execute(() -> {
            RecetteEntity recipe = recetteRepo.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recette non trouvée"));
            List<String> keywords = nlpService.extractKeywords(recipe);
            return ResponseEntity.ok(Map.of(
                "recipe_id", recipeId,
                "recipe_titre", recipe.getTitre(),
                "keywords", keywords
            ));
        });
    }

    @GetMapping("/auto-categorize/{recipeId}")
    public ResponseEntity<?> autoCategorize(@PathVariable Long recipeId) {
        logger.info("Auto-catégorisation pour recette {}", recipeId);
        return execute(() -> {
            RecetteEntity recipe = recetteRepo.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recette non trouvée"));
            List<String> categories = nlpService.autoDetectCategories(recipe);
            return ResponseEntity.ok(Map.of(
                "recipe_id", recipeId,
                "recipe_titre", recipe.getTitre(),
                "suggested_categories", categories
            ));
        });
    }

    @PostMapping("/batch-categorize")
    public ResponseEntity<?> batchAutoCategorize(@RequestParam(defaultValue = "100") int limit) {
        logger.info("Catégorisation en masse de {} recettes", limit);
        return execute(() -> {
            List<RecetteEntity> recipes = recetteRepo.findAll().stream()
                .limit(limit)
                .collect(Collectors.toList());
            int processed = 0;
            for (RecetteEntity recipe : recipes) {
                List<String> categories = nlpService.autoDetectCategories(recipe);
                logger.info("Recette {}: {}", recipe.getTitre(), categories);
                processed++;
            }
            return ResponseEntity.ok(Map.of(
                "total_processed", processed,
                "message", "Catégorisation terminée"
            ));
        });
    }

    @GetMapping("/similarity/{recipeId1}/{recipeId2}")
    public ResponseEntity<?> calculateSimilarity(
            @PathVariable Long recipeId1,
            @PathVariable Long recipeId2) {
        logger.info("Calcul similarité entre recettes {} et {}", recipeId1, recipeId2);
        return execute(() -> {
            RecetteEntity recipe1 = recetteRepo.findById(recipeId1)
                .orElseThrow(() -> new RuntimeException("Recette 1 non trouvée"));
            RecetteEntity recipe2 = recetteRepo.findById(recipeId2)
                .orElseThrow(() -> new RuntimeException("Recette 2 non trouvée"));
            double similarity = nlpService.calculateCosineSimilarity(recipe1, recipe2);
            String similarityLevel;
            if (similarity > 0.8)      similarityLevel = "Très similaire";
            else if (similarity > 0.6) similarityLevel = "Similaire";
            else if (similarity > 0.4) similarityLevel = "Modérément similaire";
            else if (similarity > 0.2) similarityLevel = "Peu similaire";
            else                       similarityLevel = "Très différent";
            return ResponseEntity.ok(Map.of(
                "recipe1", Map.of("id", recipe1.getId(), "titre", recipe1.getTitre()),
                "recipe2", Map.of("id", recipe2.getId(), "titre", recipe2.getTitre()),
                "similarity_score", Math.round(similarity * 100) / 100.0,
                "similarity_level", similarityLevel
            ));
        });
    }

    @DeleteMapping("/cache")
    public ResponseEntity<?> clearCache() {
        logger.info("Nettoyage du cache NLP");
        return execute(() -> {
            int cacheSize = nlpService.getCacheSize();
            nlpService.clearEmbeddingsCache();
            return ResponseEntity.ok(Map.of(
                "message", "Cache nettoyé",
                "embeddings_cleared", cacheSize
            ));
        });
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getNLPStats() {
        return execute(() -> {
            int cacheSize = nlpService.getCacheSize();
            int totalRecipes = (int) recetteRepo.count();
            double cacheHitRate = totalRecipes > 0 ? (double) cacheSize / totalRecipes * 100 : 0.0;
            return ResponseEntity.ok(Map.of(
                "cache_size", cacheSize,
                "total_recipes", totalRecipes,
                "cache_hit_rate", Math.round(cacheHitRate * 100) / 100.0 + "%",
                "nlp_features", List.of(
                    "Recherche sémantique",
                    "Similarité par embeddings",
                    "Analyse de sentiments",
                    "Extraction de mots-clés",
                    "Auto-catégorisation"
                )
            ));
        });
    }
}
