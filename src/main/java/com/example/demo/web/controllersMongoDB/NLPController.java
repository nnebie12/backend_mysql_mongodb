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

import com.example.demo.DTO.NLPUserInsightsDTO;
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

    // ─────────────────────────────────────────────────────────────────
    //  ✅ CORRIGÉ : appelle vraiment nlpService.getUserNLPInsights()
    // ─────────────────────────────────────────────────────────────────

    /**
     * Analyse NLP complète d'un utilisateur :
     * sentiment, mots-clés, topics, profil sémantique.
     *
     * AVANT (bugué) :
     *   insights.put("cacheSize", nlpService.getCacheSize()); // ← ne faisait rien d'utile
     *
     * APRÈS (corrigé) :
     *   Appel réel à getUserNLPInsights() qui analyse les commentaires
     *   et l'historique de recherche de l'utilisateur.
     */
    @GetMapping("/users/{userId}/insights")
    public ResponseEntity<?> getUserNLPInsights(@PathVariable Long userId) {
        logger.info("Génération des insights NLP pour l'utilisateur {}", userId);
        return execute(() -> {
            NLPUserInsightsDTO insights = nlpService.getUserNLPInsights(userId);
            return ResponseEntity.ok(insights);
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Recherche sémantique
    // ─────────────────────────────────────────────────────────────────

    @PostMapping("/search/semantic")
    public ResponseEntity<?> semanticSearch(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La requête ne peut pas être vide"));
        }
        return execute(() -> {
            List<RecetteEntity> allRecipes = recetteRepo.findAll();
            if (allRecipes.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "query", query,
                        "total_results", 0,
                        "message", "La base de données MySQL est vide."
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

    // ─────────────────────────────────────────────────────────────────
    //  Sentiment
    // ─────────────────────────────────────────────────────────────────

    @PostMapping("/sentiment")
    public ResponseEntity<?> analyzeSentiment(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le texte ne peut pas être vide"));
        }
        return execute(() -> {
            double score = nlpService.analyzeSentiment(text);
            String label;
            if (score > 0.5)       label = "Très positif";
            else if (score > 0.2)  label = "Positif";
            else if (score > -0.2) label = "Neutre";
            else if (score > -0.5) label = "Négatif";
            else                   label = "Très négatif";
            return ResponseEntity.ok(Map.of(
                    "text", text,
                    "sentiment_score", Math.round(score * 100) / 100.0,
                    "sentiment_label", label
            ));
        });
    }

    @GetMapping("/sentiment/recipe/{recipeId}")
    public ResponseEntity<?> getRecipeSentiment(@PathVariable Long recipeId) {
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
            double avg = nlpService.calculateAverageSentiment(commentaires);
            String label = avg > 0.5 ? "Très apprécié"
                    : avg > 0.2 ? "Apprécié"
                    : avg > -0.2 ? "Mitigé"
                    : "Peu apprécié";
            return ResponseEntity.ok(Map.of(
                    "recipe_id", recipeId,
                    "recipe_titre", recipe.getTitre(),
                    "total_comments", commentaires.size(),
                    "average_sentiment", Math.round(avg * 100) / 100.0,
                    "sentiment_label", label
            ));
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Mots-clés & catégories
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/keywords/{recipeId}")
    public ResponseEntity<?> extractKeywords(@PathVariable Long recipeId) {
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
    public ResponseEntity<?> batchAutoCategorize(
            @RequestParam(defaultValue = "100") int limit) {
        return execute(() -> {
            List<RecetteEntity> recipes = recetteRepo.findAll().stream()
                    .limit(limit)
                    .collect(Collectors.toList());
            for (RecetteEntity recipe : recipes) {
                List<String> cats = nlpService.autoDetectCategories(recipe);
                logger.info("Recette '{}' : {}", recipe.getTitre(), cats);
            }
            return ResponseEntity.ok(Map.of(
                    "total_processed", recipes.size(),
                    "message", "Catégorisation terminée"
            ));
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Similarité
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/similar/{recipeId}")
    public ResponseEntity<?> findSimilarByEmbeddings(
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "10") int limit) {
        return execute(() -> {
            RecetteEntity target = recetteRepo.findById(recipeId)
                    .orElseThrow(() -> new RuntimeException("Recette non trouvée"));
            List<RecetteEntity> all = recetteRepo.findAll();
            List<RecetteEntity> similar = nlpService.findMostSimilarRecipes(target, all, limit);
            List<Map<String, Object>> withScores = similar.stream().map(r -> {
                double sim = nlpService.calculateCosineSimilarity(target, r);
                Map<String, Object> res = new HashMap<>();
                res.put("recipe", recetteMapper.toResponseDto(r));
                res.put("similarity_score", Math.round(sim * 100) / 100.0);
                return res;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of(
                    "reference_recipe", Map.of("id", target.getId(), "titre", target.getTitre()),
                    "total_similar", withScores.size(),
                    "similar_recipes", withScores
            ));
        });
    }

    @GetMapping("/similarity/{recipeId1}/{recipeId2}")
    public ResponseEntity<?> calculateSimilarity(
            @PathVariable Long recipeId1,
            @PathVariable Long recipeId2) {
        return execute(() -> {
            RecetteEntity r1 = recetteRepo.findById(recipeId1)
                    .orElseThrow(() -> new RuntimeException("Recette 1 non trouvée"));
            RecetteEntity r2 = recetteRepo.findById(recipeId2)
                    .orElseThrow(() -> new RuntimeException("Recette 2 non trouvée"));
            double similarity = nlpService.calculateCosineSimilarity(r1, r2);
            String level = similarity > 0.8 ? "Très similaire"
                    : similarity > 0.6 ? "Similaire"
                    : similarity > 0.4 ? "Modérément similaire"
                    : similarity > 0.2 ? "Peu similaire"
                    : "Très différent";
            return ResponseEntity.ok(Map.of(
                    "recipe1", Map.of("id", r1.getId(), "titre", r1.getTitre()),
                    "recipe2", Map.of("id", r2.getId(), "titre", r2.getTitre()),
                    "similarity_score", Math.round(similarity * 100) / 100.0,
                    "similarity_level", level
            ));
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Cache & Stats
    // ─────────────────────────────────────────────────────────────────

    @DeleteMapping("/cache")
    public ResponseEntity<?> clearCache() {
        return execute(() -> {
            int size = nlpService.getCacheSize();
            nlpService.clearEmbeddingsCache();
            return ResponseEntity.ok(Map.of("message", "Cache nettoyé", "embeddings_cleared", size));
        });
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getNLPStats() {
        return execute(() -> {
            int cacheSize = nlpService.getCacheSize();
            int totalRecipes = (int) recetteRepo.count();
            double hitRate = totalRecipes > 0 ? (double) cacheSize / totalRecipes * 100 : 0.0;
            return ResponseEntity.ok(Map.of(
                    "cache_size", cacheSize,
                    "total_recipes", totalRecipes,
                    "cache_hit_rate", Math.round(hitRate * 100) / 100.0 + "%",
                    "nlp_features", List.of(
                            "Recherche sémantique",
                            "Analyse sentiments",
                            "Extraction mots-clés",
                            "Auto-catégorisation",
                            "Similarité embeddings",
                            "Insights utilisateur NLP"    
                    )
            ));
        });
    }
}