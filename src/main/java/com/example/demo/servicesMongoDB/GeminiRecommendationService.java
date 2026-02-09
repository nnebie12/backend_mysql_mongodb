package com.example.demo.servicesMongoDB;


import org.springframework.stereotype.Service;

import com.example.demo.DTO.RecetteResponseDTO;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entiesMongodb.RecetteInteraction;
import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.repositoryMysql.RecetteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@Service
public class GeminiRecommendationService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiRecommendationService.class);
    
    @Value("${google.cloud.project-id}")
    private String projectId;
    
    @Value("${google.cloud.location}")
    private String location;
    
    @Autowired
    private RecetteRepository recetteRepository;

    
    private static final String MODEL_NAME = "gemini-1.5-flash";
    private static final int MAX_RECOMMENDATIONS = 10;
    
    /**
     * Génère des recommandations personnalisées basées sur l'historique utilisateur
     * Utilise Gemini pour analyser les patterns et suggérer des recettes
     */
    public List<RecetteResponseDTO> getPersonalizedRecommendations(
            Long userId, 
            List<RecetteInteraction> userHistory,
            List<RecetteEntity> allRecipes,
            List<NoteDocument> userRatings
    ) {
        logger.info("Début recommandation IA pour utilisateur: {}", userId);
        
        try {
            // 1. Identification des recettes déjà vues
        	Set<Long> viewedRecipeIds = userHistory.stream()
                    .map(RecetteInteraction::getIdRecette)
                    .collect(Collectors.toSet());

            // 2. Définition des candidats : Recettes que l'utilisateur n'a PAS encore vues
            List<RecetteEntity> candidates = allRecipes.stream()
                    .filter(r -> !viewedRecipeIds.contains(r.getId()))
                    .limit(150) // Sécurité pour le contexte de l'IA
                    .collect(Collectors.toList());

            // Si aucune recette candidate, on utilise le fallback
            if (candidates.isEmpty()) {
                return getFallbackRecommendations(allRecipes);
            }

            // 3. Analyse profil et recommandations
            String userProfile = analyzeUserProfile(userId, userHistory, userRatings);
            List<Long> recommendedIds = fetchIdsFromAI(userProfile, candidates);
            
            // 4. Mapping final vers DTO
            return allRecipes.stream()
                .filter(r -> recommendedIds.contains(r.getId()))
                .map((RecetteEntity r) -> this.toDTO(r))
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Erreur Gemini, basculement vers fallback", e);
            return getFallbackRecommendations(allRecipes);
        }
    }

    private List<Long> fetchIdsFromAI(String userProfile, List<RecetteEntity> candidates) throws Exception {

        try (VertexAI vertexAI = new VertexAI(projectId, location)) {

            GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);

            StringBuilder prompt = new StringBuilder();
            prompt.append("""
                Tu es un moteur de recommandation culinaire.
                Tu dois sélectionner EXACTEMENT 10 recettes adaptées au profil utilisateur.
                
                PROFIL UTILISATEUR :
                """).append(userProfile).append("\n\n");

            prompt.append("RECETTES CANDIDATES (JSON):\n[\n");

            for (RecetteEntity r : candidates) {
                prompt.append(String.format(
                    "  {\"id\": %d, \"type\": \"%s\", \"difficulte\": \"%s\", \"temps\": %d, \"cuisine\": \"%s\"},\n",
                    r.getId(),
                    r.getTypeRecette(),
                    r.getDifficulte(),
                    r.getTempsPreparation() + r.getTempsCuisson(),
                    r.getCuisine() != null ? r.getCuisine() : "N/A"
                ));
            }

            prompt.append("""
                ]

                RÈGLES :
                - Sélectionne 10 IDs distincts
                - Favorise la cohérence avec le profil
                - Autorise 20% de découverte

                FORMAT DE RÉPONSE STRICT :
                { "ids": [1,2,3,4,5,6,7,8,9,10] }
                """);

            GenerateContentResponse response = model.generateContent(prompt.toString());
            String json = ResponseHandler.getText(response);

            return parseJsonIds(json);
        }
    }

    private List<Long> parseJsonIds(String json) {
        try {
            String clean = json.replaceAll("```json|```", "").trim();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(clean);

            if (!node.has("ids") || !node.get("ids").isArray()) {
                throw new IllegalArgumentException("Format JSON invalide");
            }

            List<Long> ids = new ArrayList<>();
            node.get("ids").forEach(n -> ids.add(n.asLong()));
            return ids;

        } catch (Exception e) {
            logger.error("Erreur parsing Gemini JSON: {}", json, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Analyse le profil utilisateur avec Gemini
     * Identifie les préférences, patterns et caractéristiques de l'utilisateur
     */
    private String analyzeUserProfile(
            Long userId, 
            List<RecetteInteraction> history,
            List<NoteDocument> ratings
    ) throws Exception {
        
        try (VertexAI vertexAI = new VertexAI(projectId, location)) {
            
            GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);
            
            StringBuilder prompt = new StringBuilder();
            prompt.append("Analyse ce profil utilisateur de recettes et identifie ses préférences culinaires.\n\n");
            
            // Ajouter les statistiques d'interactions
            Map<String, Long> typeRecetteCount = history.stream()
                .collect(Collectors.groupingBy(
                    i -> i.getRecetteEntity().getTypeRecette().toString(),
                    Collectors.counting()
                ));
            
            prompt.append("TYPES DE RECETTES CONSULTÉES:\n");
            typeRecetteCount.forEach((type, count) -> 
                prompt.append(String.format("  - %s: %d fois\n", type, count))
            );
            
            // Temps moyen de préparation préféré
            double avgPrepTime = history.stream()
                .mapToInt(i -> i.getRecetteEntity().getTempsPreparation())
                .average()
                .orElse(30);
            
            double avgCookTime = history.stream()
                .mapToInt(i -> i.getRecetteEntity().getTempsCuisson())
                .average()
                .orElse(30);
            
            prompt.append(String.format("\nTEMPS DE PRÉPARATION MOYEN: %.0f minutes\n", avgPrepTime));
            prompt.append(String.format("TEMPS DE CUISSON MOYEN: %.0f minutes\n", avgCookTime));
            
            // Difficulté préférée
            Map<String, Long> difficultyCount = history.stream()
                .collect(Collectors.groupingBy(
                    i -> i.getRecetteEntity().getDifficulte().toString(),
                    Collectors.counting()
                ));
            
            prompt.append("\nNIVEAUX DE DIFFICULTÉ:\n");
            difficultyCount.forEach((diff, count) -> 
                prompt.append(String.format("  - %s: %d fois\n", diff, count))
            );
            
            // Type de cuisine préféré
            Map<String, Long> cuisineCount = history.stream()
                .filter(i -> i.getRecetteEntity().getCuisine() != null)
                .collect(Collectors.groupingBy(
                    i -> i.getRecetteEntity().getCuisine(),
                    Collectors.counting()
                ));
            
            if (!cuisineCount.isEmpty()) {
                prompt.append("\nTYPES DE CUISINE:\n");
                cuisineCount.forEach((cuisine, count) -> 
                    prompt.append(String.format("  - %s: %d fois\n", cuisine, count))
                );
            }
            
            // Analyse des notes
            if (!ratings.isEmpty()) {
                double avgRating = ratings.stream()
                    .mapToInt(NoteDocument::getValeur)
                    .average()
                    .orElse(3.0);
                
                prompt.append(String.format("\nNOTE MOYENNE DONNÉE: %.1f/5\n", avgRating));
            }
            
            // Pattern temporel (si disponible)
            long recentInteractions = history.stream()
                .filter(i -> {
                    LocalDateTime interactionDate = i.getDateInteraction();
                    return interactionDate != null && 
                           ChronoUnit.DAYS.between(interactionDate, LocalDateTime.now()) <= 7;
                })
                .count();
            
            prompt.append(String.format("\nINTERACTIONS RÉCENTES (7 derniers jours): %d\n", recentInteractions));
            
            // Type d'interactions
            Map<String, Long> interactionTypes = history.stream()
                .collect(Collectors.groupingBy(
                    RecetteInteraction::getTypeInteraction,
                    Collectors.counting()
                ));
            
            prompt.append("\nTYPES D'INTERACTIONS:\n");
            interactionTypes.forEach((type, count) -> 
                prompt.append(String.format("  - %s: %d fois\n", type, count))
            );
            
            // Question finale pour Gemini
            prompt.append("\nBASÉ SUR CES DONNÉES, résume en 3-4 phrases:\n");
            prompt.append("1. Les préférences culinaires principales de cet utilisateur\n");
            prompt.append("2. Son niveau d'engagement et d'expertise\n");
            prompt.append("3. Les types de recettes qui lui conviendraient le mieux\n");
            
            // Appel à Gemini
            GenerateContentResponse response = model.generateContent(prompt.toString());
            String analysis = ResponseHandler.getText(response);
            
            logger.debug("Analyse Gemini du profil: {}", analysis);
            
            return analysis;
        }
    }
    
    /**
     * Génère les recommandations avec l'IA Gemini
     */
    private String generateRecommendationsWithAI(
            String userProfile, 
            List<RecetteEntity> candidateRecipes,
            List<RecetteInteraction> history
    ) throws Exception {
        
        try (VertexAI vertexAI = new VertexAI(projectId, location)) {
            
            GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);
            
            StringBuilder prompt = new StringBuilder();
            prompt.append("Tu es un expert en recommandations de recettes culinaires.\n\n");
            
            prompt.append("PROFIL UTILISATEUR:\n");
            prompt.append(userProfile);
            prompt.append("\n\n");
            
            prompt.append("RECETTES DISPONIBLES (parmi lesquelles recommander):\n");
            
            
            List<RecetteEntity> limitedRecipes = candidateRecipes.stream()
                .limit(100)
                .collect(Collectors.toList());
            
            for (RecetteEntity recipe : limitedRecipes) {
                prompt.append(String.format(
                    "ID:%d | %s | Type:%s | Difficulté:%s | Temps:%dmin | Cuisine:%s\n",
                    recipe.getId(),
                    recipe.getTitre(),
                    recipe.getTypeRecette(),
                    recipe.getDifficulte(),
                    recipe.getTempsPreparation() + recipe.getTempsCuisson(),
                    recipe.getCuisine() != null ? recipe.getCuisine() : "N/A"
                ));
            }
            
            prompt.append("\nTÂCHE:\n");
            prompt.append("Recommande exactement 10 recettes qui correspondent le MIEUX au profil utilisateur.\n");
            prompt.append("Considère:\n");
            prompt.append("  - Les préférences de type de recette\n");
            prompt.append("  - Le niveau de difficulté approprié\n");
            prompt.append("  - Le temps de préparation préféré\n");
            prompt.append("  - La variété des types de cuisine\n");
            prompt.append("  - L'exploration de nouvelles options similaires\n\n");
            
            prompt.append("IMPORTANT: Réponds UNIQUEMENT avec les IDs des 10 recettes recommandées, séparés par des virgules.\n");
            prompt.append("Format exact attendu: 12,45,78,23,56,89,34,67,91,15\n");
            prompt.append("Ne fournis AUCUN autre texte, explication ou formatage.\n");
            
            // Appel à Gemini
            GenerateContentResponse response = model.generateContent(prompt.toString());
            String recommendations = ResponseHandler.getText(response).trim();
            
            logger.debug("Recommandations brutes de Gemini: {}", recommendations);
            
            return recommendations;
        }
    }
    
    /**
     * Parse les IDs recommandés depuis la réponse de Gemini
     */
    private List<Long> parseRecommendedIds(String response) {
        List<Long> ids = new ArrayList<>();
        
        try {
            // Nettoyer la réponse
            String cleaned = response
                .replaceAll("[^0-9,]", "") 
                .trim();
            
            // Extraire les IDs
            String[] parts = cleaned.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && trimmed.matches("\\d+")) {
                    ids.add(Long.parseLong(trimmed));
                }
            }
            
            logger.debug("IDs parsés: {}", ids);
            
        } catch (Exception e) {
            logger.error("Erreur lors du parsing des IDs: {}", response, e);
        }
        
        return ids;
    }
    
    /**
     * Trouve des recettes similaires à une recette donnée
     * Utilise le content-based filtering avec assistance Gemini
     */
    public List<RecetteResponseDTO> findSimilarRecipes(
            RecetteEntity targetRecipe,
            List<RecetteEntity> allRecipes
    ) {
        
        logger.info("Recherche de recettes similaires à: {}", targetRecipe.getTitre());
        
        try {
            try (VertexAI vertexAI = new VertexAI(projectId, location)) {
                
                GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);
                
                StringBuilder prompt = new StringBuilder();
                prompt.append("Trouve les recettes les plus similaires à cette recette de référence:\n\n");
                
                prompt.append("RECETTE DE RÉFÉRENCE:\n");
                prompt.append(String.format("  - Titre: %s\n", targetRecipe.getTitre()));
                prompt.append(String.format("  - Type: %s\n", targetRecipe.getTypeRecette()));
                prompt.append(String.format("  - Difficulté: %s\n", targetRecipe.getDifficulte()));
                prompt.append(String.format("  - Temps: %dmin\n", 
                    targetRecipe.getTempsPreparation() + targetRecipe.getTempsCuisson()));
                prompt.append(String.format("  - Cuisine: %s\n", targetRecipe.getCuisine()));
                
                if (targetRecipe.getDescription() != null && !targetRecipe.getDescription().isEmpty()) {
                    prompt.append(String.format("  - Description: %s\n", 
                        targetRecipe.getDescription().substring(0, Math.min(200, targetRecipe.getDescription().length()))));
                }
                
                prompt.append("\nRECETTES DISPONIBLES:\n");
                
                // Filtrer pour exclure la recette cible
                List<RecetteEntity> candidates = allRecipes.stream()
                    .filter(r -> !r.getId().equals(targetRecipe.getId()))
                    .limit(100)
                    .collect(Collectors.toList());
                
                for (RecetteEntity recipe : candidates) {
                    prompt.append(String.format(
                        "ID:%d | %s | %s | %s | %dmin\n",
                        recipe.getId(),
                        recipe.getTitre(),
                        recipe.getTypeRecette(),
                        recipe.getDifficulte(),
                        recipe.getTempsPreparation() + recipe.getTempsCuisson()
                    ));
                }
                
                prompt.append("\nRecommande 10 recettes SIMILAIRES basées sur:\n");
                prompt.append("  - Même type de recette ou type complémentaire\n");
                prompt.append("  - Niveau de difficulté similaire\n");
                prompt.append("  - Temps de préparation comparable\n");
                prompt.append("  - Même type de cuisine si applicable\n\n");
                
                prompt.append("Réponds UNIQUEMENT avec les IDs séparés par des virgules.\n");
                prompt.append("Format: 12,45,78,23,56,89,34,67,91,15\n");
                
                GenerateContentResponse response = model.generateContent(prompt.toString());
                String similarRecipesText = ResponseHandler.getText(response);
                
                List<Long> similarIds = parseRecommendedIds(similarRecipesText);
                
                return allRecipes.stream()
                    .filter(r -> similarIds.contains(r.getId()))
                    .limit(MAX_RECOMMENDATIONS)
                    .map((RecetteEntity r) -> this.toDTO(r))
                    .collect(Collectors.toList());
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche de recettes similaires", e);
            // Fallback: recettes du même type
            return getSameTypeRecipes(targetRecipe, allRecipes);
        }
    }
    
    /**
     * Détecte les tendances culinaires avec analyse IA
     */
    public Map<String, Object> detectTrends(List<RecetteInteraction> allInteractions) {
        
        logger.info("Détection des tendances sur {} interactions", allInteractions.size());
        
        try {
            try (VertexAI vertexAI = new VertexAI(projectId, location)) {
                
                GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);
                
                // Filtrer les interactions des 30 derniers jours
                LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
                
                List<RecetteInteraction> recentInteractions = allInteractions.stream()
                    .filter(i -> i.getDateInteraction() != null && 
                                i.getDateInteraction().isAfter(thirtyDaysAgo))
                    .collect(Collectors.toList());
                
                // Analyser par type de recette
                Map<String, Long> typeRecetteTrends = recentInteractions.stream()
                    .collect(Collectors.groupingBy(
                        i -> i.getRecetteEntity().getTypeRecette().toString(),
                        Collectors.counting()
                    ));
                
                // Analyser par type de cuisine
                Map<String, Long> cuisineTrends = recentInteractions.stream()
                    .filter(i -> i.getRecetteEntity().getCuisine() != null)
                    .collect(Collectors.groupingBy(
                        i -> i.getRecetteEntity().getCuisine(),
                        Collectors.counting()
                    ));
                
                // Analyser par difficulté
                Map<String, Long> difficultyTrends = recentInteractions.stream()
                    .collect(Collectors.groupingBy(
                        i -> i.getRecetteEntity().getDifficulte().toString(),
                        Collectors.counting()
                    ));
                
                // Construire le prompt pour Gemini
                StringBuilder prompt = new StringBuilder();
                prompt.append("Analyse ces tendances de consultations de recettes sur les 30 derniers jours:\n\n");
                
                prompt.append("TENDANCES PAR TYPE DE RECETTE:\n");
                typeRecetteTrends.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> prompt.append(String.format("  - %s: %d consultations\n", 
                        entry.getKey(), entry.getValue())));
                
                prompt.append("\nTENDANCES PAR TYPE DE CUISINE:\n");
                cuisineTrends.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry -> prompt.append(String.format("  - %s: %d consultations\n", 
                        entry.getKey(), entry.getValue())));
                
                prompt.append("\nTENDANCES PAR DIFFICULTÉ:\n");
                difficultyTrends.forEach((diff, count) -> 
                    prompt.append(String.format("  - %s: %d consultations\n", diff, count)));
                
                prompt.append("\nTotal d'interactions: ").append(recentInteractions.size()).append("\n");
                
                prompt.append("\nIDENTIFIE et EXPLIQUE:\n");
                prompt.append("1. Les 3 tendances principales (types de recettes en hausse)\n");
                prompt.append("2. Les préférences culinaires dominantes\n");
                prompt.append("3. Le niveau d'expertise général (basé sur les difficultés)\n");
                prompt.append("4. Les opportunités (types de recettes à développer)\n");
                
                GenerateContentResponse response = model.generateContent(prompt.toString());
                String analysis = ResponseHandler.getText(response);
                
                // Construire la réponse
                Map<String, Object> result = new HashMap<>();
                result.put("period", "30 jours");
                result.put("total_interactions", recentInteractions.size());
                result.put("type_recette_trends", typeRecetteTrends);
                result.put("cuisine_trends", cuisineTrends);
                result.put("difficulty_trends", difficultyTrends);
                result.put("ai_analysis", analysis);
                result.put("generated_at", LocalDateTime.now());
                
                logger.info("Analyse des tendances générée avec succès");
                
                return result;
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors de la détection des tendances", e);
            return Map.of("error", "Impossible de générer l'analyse des tendances");
        }
    }
    
    /**
     * Segmente les utilisateurs avec analyse IA (RFM)
     */
    public Map<String, Object> segmentUsers(List<Map<String, Object>> userStatistics) {
        
        logger.info("Segmentation de {} utilisateurs", userStatistics.size());
        
        try {
            try (VertexAI vertexAI = new VertexAI(projectId, location)) {
                
                GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);
                
                StringBuilder prompt = new StringBuilder();
                prompt.append("Segmente ces utilisateurs selon leur comportement RFM (Recency, Frequency, Monetary):\n\n");
                
                // Ajouter les statistiques (limiter pour le contexte)
                userStatistics.stream()
                    .limit(50)
                    .forEach(stat -> {
                        prompt.append(String.format(
                            "User %s: Recency=%d jours, Frequency=%d, Engagement=%s\n",
                            stat.get("user_id"),
                            stat.get("recency"),
                            stat.get("frequency"),
                            stat.get("engagement_score")
                        ));
                    });
                
                prompt.append("\nCrée 5 segments d'utilisateurs:\n");
                prompt.append("1. Champions (très actifs, récents)\n");
                prompt.append("2. Fidèles (actifs réguliers)\n");
                prompt.append("3. Nouveaux (récents mais peu d'activité)\n");
                prompt.append("4. À Risque (actifs auparavant, inactifs récemment)\n");
                prompt.append("5. Perdus (inactifs depuis longtemps)\n\n");
                
                prompt.append("Pour chaque segment, fournis:\n");
                prompt.append("- Critères de segmentation (seuils Recency/Frequency)\n");
                prompt.append("- Actions recommandées pour chaque segment\n");
                
                GenerateContentResponse response = model.generateContent(prompt.toString());
                String segmentationAnalysis = ResponseHandler.getText(response);
                
                // Construire la réponse
                Map<String, Object> result = new HashMap<>();
                result.put("total_users", userStatistics.size());
                result.put("segmentation_analysis", segmentationAnalysis);
                result.put("generated_at", LocalDateTime.now());
                
                // Calculer la distribution réelle basée sur des règles simples
                Map<String, Integer> distribution = calculateSegmentDistribution(userStatistics);
                result.put("segment_distribution", distribution);
                
                logger.info("Segmentation utilisateurs générée");
                
                return result;
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors de la segmentation des utilisateurs", e);
            return Map.of("error", "Impossible de générer la segmentation");
        }
    }
    
    /**
     * Calcule la distribution des segments basée sur des règles RFM
     */
    private Map<String, Integer> calculateSegmentDistribution(List<Map<String, Object>> userStats) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("Champions", 0);
        distribution.put("Fidèles", 0);
        distribution.put("Nouveaux", 0);
        distribution.put("À Risque", 0);
        distribution.put("Perdus", 0);
        
        for (Map<String, Object> stat : userStats) {
            int recency = (int) stat.get("recency");
            int frequency = (int) stat.get("frequency");
            
            String segment;
            if (recency <= 7 && frequency >= 10) {
                segment = "Champions";
            } else if (recency <= 30 && frequency >= 5) {
                segment = "Fidèles";
            } else if (recency <= 7 && frequency < 5) {
                segment = "Nouveaux";
            } else if (recency > 30 && frequency >= 5) {
                segment = "À Risque";
            } else {
                segment = "Perdus";
            }
            
            distribution.put(segment, distribution.get(segment) + 1);
        }
        
        return distribution;
    }
    
    /**
     * Recommandations de fallback basées sur la popularité
     */
    private List<RecetteResponseDTO> getFallbackRecommendations(List<RecetteEntity> allRecipes) {
        logger.info("Utilisation des recommandations de fallback (popularité)");
        
        // Simuler un score de popularité simple
        return allRecipes.stream()
            .sorted((r1, r2) -> {
                // Prioriser les recettes avec images
                int scoreR1 = (r1.getImageUrl() != null ? 1 : 0);
                int scoreR2 = (r2.getImageUrl() != null ? 1 : 0);
                return Integer.compare(scoreR2, scoreR1);
            })
            .limit(MAX_RECOMMENDATIONS)
            .map((RecetteEntity r) -> this.toDTO(r))
            .collect(Collectors.toList());
    }
    
    /**
     * Recettes du même type (fallback pour similarité)
     */
    private List<RecetteResponseDTO> getSameTypeRecipes(RecetteEntity targetRecipe, List<RecetteEntity> allRecipes) {
        return allRecipes.stream()
            .filter(r -> !r.getId().equals(targetRecipe.getId()))
            .filter(r -> r.getTypeRecette().equals(targetRecipe.getTypeRecette()))
            .limit(MAX_RECOMMENDATIONS)
            .map((RecetteEntity r) -> this.toDTO(r))
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