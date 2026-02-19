package com.example.demo.web.controllersMongoDB;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.DTO.RecetteResponseDTO;
import com.example.demo.entiesMongodb.CommentaireDocument;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.repositoryMongoDB.CommentaireMongoRepository;
import com.example.demo.repositoryMysql.RecetteRepository;
import com.example.demo.servicesImplMongoDB.RecipeNLPService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Controller pour les fonctionnalités NLP
 * Endpoints pour recherche sémantique, analyse de sentiments, similarité
 */
@RestController
@RequestMapping("/api/v1/nlp")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class NLPController {
    
    private static final Logger logger = LoggerFactory.getLogger(NLPController.class);
    
    @Autowired
    private RecipeNLPService nlpService;
    
    @Autowired
    private RecetteRepository recetteRepo;
    
    @Autowired
    private CommentaireMongoRepository commentaireRepo;
    
    /**
     * POST /api/v1/nlp/search/semantic
     * Recherche sémantique : trouve des recettes basées sur une description en langage naturel
     * 
     * Body: { "query": "quelque chose de léger et frais pour l'été" }
     */
    @PostMapping("/search/semantic")
    public ResponseEntity<?> semanticSearch(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La requête ne peut pas être vide"));
        }

        try {
            List<RecetteEntity> allRecipes = recetteRepo.findAll();
            logger.info("Nombre de recettes récupérées pour NLP : {}", allRecipes.size());
            
            // Alerte si la base est vide
            if (allRecipes.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "query", query,
                    "total_results", 0,
                    "message", "La base de données MySQL est vide. Importez le CSV d'abord."
                ));
            }

            List<RecetteEntity> results = nlpService.semanticSearch(query, allRecipes, 10);
            
            List<RecetteResponseDTO> dtos = results.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "query", query,
                "total_results", dtos.size(),
                "results", dtos
            ));
        } catch (Exception e) {
            logger.error("NLP Error: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Erreur moteur NLP: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/v1/nlp/users/{userId}/insights
     * Analyse NLP complète pour un utilisateur
     */
    @GetMapping("/users/{userId}/insights")
    public ResponseEntity<?> getUserNLPInsights(@PathVariable Long userId) {
        try {
            // Exemple : retourner des stats NLP pour cet utilisateur
            Map<String, Object> insights = new HashMap<>();
            insights.put("userId", userId);
            insights.put("cacheSize", nlpService.getCacheSize());
            // Ajoutez ici la logique métier souhaitée
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            logger.error("Erreur insights NLP pour userId {}", userId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * GET /api/v1/nlp/similar/{recipeId}
     * Trouve les recettes les plus similaires basées sur les embeddings NLP
     */
    @GetMapping("/similar/{recipeId}")
    public ResponseEntity<?> findSimilarByEmbeddings(
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("Recherche recettes similaires (NLP) à la recette {}", recipeId);
        
        try {
            RecetteEntity targetRecipe = recetteRepo.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recette non trouvée"));
            
            List<RecetteEntity> allRecipes = recetteRepo.findAll();
            
            List<RecetteEntity> similar = nlpService.findMostSimilarRecipes(
                targetRecipe, 
                allRecipes, 
                limit
            );
            
            // Calculer les scores de similarité
            List<Map<String, Object>> resultsWithScores = similar.stream()
                .map(recipe -> {
                    double similarity = nlpService.calculateCosineSimilarity(targetRecipe, recipe);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("recipe", toDTO(recipe));
                    result.put("similarity_score", Math.round(similarity * 100) / 100.0);
                    
                    return result;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "reference_recipe", Map.of(
                    "id", targetRecipe.getId(),
                    "titre", targetRecipe.getTitre()
                ),
                "total_similar", resultsWithScores.size(),
                "similar_recipes", resultsWithScores
            ));
            
        } catch (Exception e) {
            logger.error("Erreur recherche similarité NLP", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors de la recherche de recettes similaires"
            ));
        }
    }
    
    /**
     * POST /api/v1/nlp/sentiment
     * Analyse le sentiment d'un commentaire
     * 
     * Body: { "text": "Délicieux ! Mes enfants ont adoré" }
     */
    @PostMapping("/sentiment")
    public ResponseEntity<?> analyzeSentiment(@RequestBody Map<String, String> request) {
        
        String text = request.get("text");
        
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Le texte ne peut pas être vide"
            ));
        }
        
        logger.info("Analyse de sentiment");
        
        try {
            double sentimentScore = nlpService.analyzeSentiment(text);
            
            String sentiment;
            if (sentimentScore > 0.5) {
                sentiment = "Très positif";
            } else if (sentimentScore > 0.2) {
                sentiment = "Positif";
            } else if (sentimentScore > -0.2) {
                sentiment = "Neutre";
            } else if (sentimentScore > -0.5) {
                sentiment = "Négatif";
            } else {
                sentiment = "Très négatif";
            }
            
            return ResponseEntity.ok(Map.of(
                "text", text,
                "sentiment_score", Math.round(sentimentScore * 100) / 100.0,
                "sentiment_label", sentiment
            ));
            
        } catch (Exception e) {
            logger.error("Erreur analyse sentiment", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors de l'analyse de sentiment"
            ));
        }
    }
    
    /**
     * GET /api/v1/nlp/sentiment/recipe/{recipeId}
     * Calcule le sentiment moyen basé sur tous les commentaires d'une recette
     */
    @GetMapping("/sentiment/recipe/{recipeId}")
    public ResponseEntity<?> getRecipeSentiment(@PathVariable Long recipeId) {
        
        logger.info("Calcul sentiment pour recette {}", recipeId);
        
        try {
            RecetteEntity recipe = recetteRepo.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recette non trouvée"));
            
            List<CommentaireDocument> commentaires = commentaireRepo.findByRecetteEntityId(recipeId);
            
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
            if (avgSentiment > 0.5) {
                sentimentLabel = "Très apprécié";
            } else if (avgSentiment > 0.2) {
                sentimentLabel = "Apprécié";
            } else if (avgSentiment > -0.2) {
                sentimentLabel = "Mitigé";
            } else {
                sentimentLabel = "Peu apprécié";
            }
            
            return ResponseEntity.ok(Map.of(
                "recipe_id", recipeId,
                "recipe_titre", recipe.getTitre(),
                "total_comments", commentaires.size(),
                "average_sentiment", Math.round(avgSentiment * 100) / 100.0,
                "sentiment_label", sentimentLabel
            ));
            
        } catch (Exception e) {
            logger.error("Erreur calcul sentiment recette", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors du calcul du sentiment"
            ));
        }
    }
    
    /**
     * GET /api/v1/nlp/keywords/{recipeId}
     * Extrait les mots-clés d'une recette avec NLP
     */
    @GetMapping("/keywords/{recipeId}")
    public ResponseEntity<?> extractKeywords(@PathVariable Long recipeId) {
        
        logger.info("Extraction mots-clés pour recette {}", recipeId);
        
        try {
            RecetteEntity recipe = recetteRepo.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recette non trouvée"));
            
            List<String> keywords = nlpService.extractKeywords(recipe);
            
            return ResponseEntity.ok(Map.of(
                "recipe_id", recipeId,
                "recipe_titre", recipe.getTitre(),
                "keywords", keywords
            ));
            
        } catch (Exception e) {
            logger.error("Erreur extraction mots-clés", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors de l'extraction des mots-clés"
            ));
        }
    }
    
    /**
     * GET /api/v1/nlp/auto-categorize/{recipeId}
     * Détecte automatiquement les catégories/tags d'une recette
     */
    @GetMapping("/auto-categorize/{recipeId}")
    public ResponseEntity<?> autoCategorize(@PathVariable Long recipeId) {
        
        logger.info("Auto-catégorisation pour recette {}", recipeId);
        
        try {
            RecetteEntity recipe = recetteRepo.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recette non trouvée"));
            
            List<String> categories = nlpService.autoDetectCategories(recipe);
            
            return ResponseEntity.ok(Map.of(
                "recipe_id", recipeId,
                "recipe_titre", recipe.getTitre(),
                "suggested_categories", categories
            ));
            
        } catch (Exception e) {
            logger.error("Erreur auto-catégorisation", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors de l'auto-catégorisation"
            ));
        }
    }
    
    /**
     * POST /api/v1/nlp/batch-categorize
     * Catégorise automatiquement toutes les recettes sans catégories
     */
    @PostMapping("/batch-categorize")
    public ResponseEntity<?> batchAutoCategorize(
            @RequestParam(defaultValue = "100") int limit) {
        
        logger.info("Catégorisation en masse de {} recettes", limit);
        
        try {
            List<RecetteEntity> recipes = recetteRepo.findAll().stream()
                .limit(limit)
                .collect(Collectors.toList());
            
            int processed = 0;
            
            for (RecetteEntity recipe : recipes) {
                List<String> categories = nlpService.autoDetectCategories(recipe);
                
                // Sauvegarder les catégories (adapter selon votre modèle)
                logger.info("Recette {}: {}", recipe.getTitre(), categories);
                
                processed++;
                
                // Pause pour ne pas surcharger l'API
                Thread.sleep(500);
            }
            
            return ResponseEntity.ok(Map.of(
                "total_processed", processed,
                "message", "Catégorisation terminée"
            ));
            
        } catch (Exception e) {
            logger.error("Erreur catégorisation en masse", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors de la catégorisation en masse"
            ));
        }
    }
    
    /**
     * GET /api/v1/nlp/similarity/{recipeId1}/{recipeId2}
     * Calcule la similarité entre deux recettes spécifiques
     */
    @GetMapping("/similarity/{recipeId1}/{recipeId2}")
    public ResponseEntity<?> calculateSimilarity(
            @PathVariable Long recipeId1,
            @PathVariable Long recipeId2) {
        
        logger.info("Calcul similarité entre recettes {} et {}", recipeId1, recipeId2);
        
        try {
            RecetteEntity recipe1 = recetteRepo.findById(recipeId1)
                .orElseThrow(() -> new RuntimeException("Recette 1 non trouvée"));
            
            RecetteEntity recipe2 = recetteRepo.findById(recipeId2)
                .orElseThrow(() -> new RuntimeException("Recette 2 non trouvée"));
            
            double similarity = nlpService.calculateCosineSimilarity(recipe1, recipe2);
            
            String similarityLevel;
            if (similarity > 0.8) {
                similarityLevel = "Très similaire";
            } else if (similarity > 0.6) {
                similarityLevel = "Similaire";
            } else if (similarity > 0.4) {
                similarityLevel = "Modérément similaire";
            } else if (similarity > 0.2) {
                similarityLevel = "Peu similaire";
            } else {
                similarityLevel = "Très différent";
            }
            
            return ResponseEntity.ok(Map.of(
                "recipe1", Map.of("id", recipe1.getId(), "titre", recipe1.getTitre()),
                "recipe2", Map.of("id", recipe2.getId(), "titre", recipe2.getTitre()),
                "similarity_score", Math.round(similarity * 100) / 100.0,
                "similarity_level", similarityLevel
            ));
            
        } catch (Exception e) {
            logger.error("Erreur calcul similarité", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors du calcul de similarité"
            ));
        }
    }
    
    /**
     * DELETE /api/v1/nlp/cache
     * Nettoie le cache des embeddings
     */
    @DeleteMapping("/cache")
    public ResponseEntity<?> clearCache() {
        
        logger.info("Nettoyage du cache NLP");
        
        try {
            int cacheSize = nlpService.getCacheSize();
            nlpService.clearEmbeddingsCache();
            
            return ResponseEntity.ok(Map.of(
                "message", "Cache nettoyé",
                "embeddings_cleared", cacheSize
            ));
            
        } catch (Exception e) {
            logger.error("Erreur nettoyage cache", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors du nettoyage du cache"
            ));
        }
    }
    
    /**
     * GET /api/v1/nlp/stats
     * Statistiques du service NLP
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getNLPStats() {
        
        try {
            int cacheSize = nlpService.getCacheSize();
            int totalRecipes = (int) recetteRepo.count();
            
            double cacheHitRate = totalRecipes > 0 
                ? (double) cacheSize / totalRecipes * 100 
                : 0.0;
            
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
            
        } catch (Exception e) {
            logger.error("Erreur statistiques NLP", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erreur lors de la récupération des statistiques"
            ));
        }
    }
    
    // Conversion vers DTO
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