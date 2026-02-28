package com.example.demo.servicesMongoDB;

import org.springframework.stereotype.Service;

import com.example.demo.DTO.RecetteResponseDTO;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entiesMongodb.RecetteInteraction;
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
import java.util.function.Function;
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

    // ─────────────────────────────────────────────────────────────────
    //  MÉTHODE UTILITAIRE CENTRALE
    //  Charge en une seule requête MySQL toutes les RecetteEntity
    //  référencées par une liste d'interactions (via idRecette Long).
    // ─────────────────────────────────────────────────────────────────

    
    private Map<Long, RecetteEntity> loadRecettesFromInteractions(List<RecetteInteraction> interactions) {
        List<Long> ids = interactions.stream()
                .map(RecetteInteraction::getIdRecette)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return recetteRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(RecetteEntity::getId, Function.identity()));
    }

    // ─────────────────────────────────────────────────────────────────
    //  RECOMMANDATIONS PERSONNALISÉES
    // ─────────────────────────────────────────────────────────────────

    public List<RecetteResponseDTO> getPersonalizedRecommendations(
            Long userId,
            List<RecetteInteraction> userHistory,
            List<RecetteEntity> allRecipes,
            List<NoteDocument> userRatings) {

        logger.info("Début recommandation IA pour utilisateur: {}", userId);

        try {
            Set<Long> viewedRecipeIds = userHistory.stream()
                    .map(RecetteInteraction::getIdRecette)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<RecetteEntity> candidates = allRecipes.stream()
                    .filter(r -> !viewedRecipeIds.contains(r.getId()))
                    .limit(150)
                    .collect(Collectors.toList());

            if (candidates.isEmpty()) {
                return getFallbackRecommendations(allRecipes);
            }

            Map<Long, RecetteEntity> recettesMap = loadRecettesFromInteractions(userHistory);
            String userProfile = analyzeUserProfile(userId, userHistory, userRatings, recettesMap);
            List<Long> recommendedIds = fetchIdsFromAI(userProfile, candidates);

            return allRecipes.stream()
                    .filter(r -> recommendedIds.contains(r.getId()))
                    .map(this::toDTO)
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
                        r.getTypeRecette() != null ? r.getTypeRecette() : "N/A",
                        r.getDifficulte() != null ? r.getDifficulte() : "N/A",
                        (r.getTempsPreparation() != null ? r.getTempsPreparation() : 0)
                                + (r.getTempsCuisson() != null ? r.getTempsCuisson() : 0),
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
            return parseJsonIds(ResponseHandler.getText(response));
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

    // ─────────────────────────────────────────────────────────────────
    //  ANALYSE DU PROFIL UTILISATEUR
    // ─────────────────────────────────────────────────────────────────

    
    private String analyzeUserProfile(
            Long userId,
            List<RecetteInteraction> history,
            List<NoteDocument> ratings,
            Map<Long, RecetteEntity> recettesMap) throws Exception {

        try (VertexAI vertexAI = new VertexAI(projectId, location)) {

            GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);

            StringBuilder prompt = new StringBuilder();
            prompt.append("Analyse ce profil utilisateur de recettes et identifie ses préférences culinaires.\n\n");

            Map<String, Long> typeRecetteCount = history.stream()
                    .map(i -> recettesMap.get(i.getIdRecette()))
                    .filter(r -> r != null && r.getTypeRecette() != null)
                    .collect(Collectors.groupingBy(RecetteEntity::getTypeRecette, Collectors.counting()));

            prompt.append("TYPES DE RECETTES CONSULTÉES:\n");
            typeRecetteCount.forEach((type, count) ->
                    prompt.append(String.format("  - %s: %d fois\n", type, count)));

            double avgPrepTime = history.stream()
                    .map(i -> recettesMap.get(i.getIdRecette()))
                    .filter(r -> r != null && r.getTempsPreparation() != null)
                    .mapToInt(RecetteEntity::getTempsPreparation)
                    .average().orElse(30);

            double avgCookTime = history.stream()
                    .map(i -> recettesMap.get(i.getIdRecette()))
                    .filter(r -> r != null && r.getTempsCuisson() != null)
                    .mapToInt(RecetteEntity::getTempsCuisson)
                    .average().orElse(30);

            prompt.append(String.format("\nTEMPS DE PRÉPARATION MOYEN: %.0f minutes\n", avgPrepTime));
            prompt.append(String.format("TEMPS DE CUISSON MOYEN: %.0f minutes\n", avgCookTime));

            Map<String, Long> difficultyCount = history.stream()
                    .map(i -> recettesMap.get(i.getIdRecette()))
                    .filter(r -> r != null && r.getDifficulte() != null)
                    .collect(Collectors.groupingBy(RecetteEntity::getDifficulte, Collectors.counting()));

            prompt.append("\nNIVEAUX DE DIFFICULTÉ:\n");
            difficultyCount.forEach((diff, count) ->
                    prompt.append(String.format("  - %s: %d fois\n", diff, count)));

            Map<String, Long> cuisineCount = history.stream()
                    .map(i -> recettesMap.get(i.getIdRecette()))
                    .filter(r -> r != null && r.getCuisine() != null)
                    .collect(Collectors.groupingBy(RecetteEntity::getCuisine, Collectors.counting()));

            if (!cuisineCount.isEmpty()) {
                prompt.append("\nTYPES DE CUISINE:\n");
                cuisineCount.forEach((cuisine, count) ->
                        prompt.append(String.format("  - %s: %d fois\n", cuisine, count)));
            }

            if (!ratings.isEmpty()) {
                double avgRating = ratings.stream()
                        .mapToInt(NoteDocument::getValeur).average().orElse(3.0);
                prompt.append(String.format("\nNOTE MOYENNE DONNÉE: %.1f/5\n", avgRating));
            }

            long recentInteractions = history.stream()
                    .filter(i -> i.getDateInteraction() != null &&
                            ChronoUnit.DAYS.between(i.getDateInteraction(), LocalDateTime.now()) <= 7)
                    .count();
            prompt.append(String.format("\nINTERACTIONS RÉCENTES (7 derniers jours): %d\n", recentInteractions));

            Map<String, Long> interactionTypes = history.stream()
                    .filter(i -> i.getTypeInteraction() != null)
                    .collect(Collectors.groupingBy(RecetteInteraction::getTypeInteraction, Collectors.counting()));

            prompt.append("\nTYPES D'INTERACTIONS:\n");
            interactionTypes.forEach((type, count) ->
                    prompt.append(String.format("  - %s: %d fois\n", type, count)));

            prompt.append("\nBASÉ SUR CES DONNÉES, résume en 3-4 phrases:\n");
            prompt.append("1. Les préférences culinaires principales de cet utilisateur\n");
            prompt.append("2. Son niveau d'engagement et d'expertise\n");
            prompt.append("3. Les types de recettes qui lui conviendraient le mieux\n");

            GenerateContentResponse response = model.generateContent(prompt.toString());
            String analysis = ResponseHandler.getText(response);
            logger.debug("Analyse Gemini du profil: {}", analysis);
            return analysis;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  RECETTES SIMILAIRES
    // ─────────────────────────────────────────────────────────────────

    public List<RecetteResponseDTO> findSimilarRecipes(
            RecetteEntity targetRecipe,
            List<RecetteEntity> allRecipes) {

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
                        (targetRecipe.getTempsPreparation() != null ? targetRecipe.getTempsPreparation() : 0)
                                + (targetRecipe.getTempsCuisson() != null ? targetRecipe.getTempsCuisson() : 0)));
                prompt.append(String.format("  - Cuisine: %s\n", targetRecipe.getCuisine()));

                if (targetRecipe.getDescription() != null && !targetRecipe.getDescription().isEmpty()) {
                    prompt.append(String.format("  - Description: %s\n",
                            targetRecipe.getDescription().substring(0, Math.min(200, targetRecipe.getDescription().length()))));
                }

                prompt.append("\nRECETTES DISPONIBLES:\n");

                List<RecetteEntity> candidates = allRecipes.stream()
                        .filter(r -> !r.getId().equals(targetRecipe.getId()))
                        .limit(100)
                        .collect(Collectors.toList());

                for (RecetteEntity recipe : candidates) {
                    prompt.append(String.format(
                            "ID:%d | %s | %s | %s | %dmin\n",
                            recipe.getId(),
                            recipe.getTitre(),
                            recipe.getTypeRecette() != null ? recipe.getTypeRecette() : "N/A",
                            recipe.getDifficulte() != null ? recipe.getDifficulte() : "N/A",
                            (recipe.getTempsPreparation() != null ? recipe.getTempsPreparation() : 0)
                                    + (recipe.getTempsCuisson() != null ? recipe.getTempsCuisson() : 0)
                    ));
                }

                prompt.append("\nRecommande 10 recettes SIMILAIRES. Réponds UNIQUEMENT avec les IDs séparés par des virgules.\n");
                prompt.append("Format: 12,45,78,23,56,89,34,67,91,15\n");

                GenerateContentResponse response = model.generateContent(prompt.toString());
                List<Long> similarIds = parseRecommendedIds(ResponseHandler.getText(response));

                return allRecipes.stream()
                        .filter(r -> similarIds.contains(r.getId()))
                        .limit(MAX_RECOMMENDATIONS)
                        .map(this::toDTO)
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            logger.error("Erreur lors de la recherche de recettes similaires", e);
            return getSameTypeRecipes(targetRecipe, allRecipes);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  DÉTECTION DES TENDANCES
    // ─────────────────────────────────────────────────────────────────

    /**
     * ✅ CORRECTION : chargement batch des RecetteEntity depuis les idRecette (Long)
     *    pour remplacer les anciens appels i.getRecetteId().getTypeRecette() etc.
     */
    public Map<String, Object> detectTrends(List<RecetteInteraction> allInteractions) {

        logger.info("Détection des tendances sur {} interactions", allInteractions.size());

        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

            List<RecetteInteraction> recentInteractions = allInteractions.stream()
                    .filter(i -> i.getDateInteraction() != null &&
                            i.getDateInteraction().isAfter(thirtyDaysAgo))
                    .collect(Collectors.toList());

            Map<Long, RecetteEntity> recettesMap = loadRecettesFromInteractions(recentInteractions);

            Map<String, Long> typeRecetteTrends = recentInteractions.stream()
                    .map(i -> recettesMap.get(i.getIdRecette()))
                    .filter(r -> r != null && r.getTypeRecette() != null)
                    .collect(Collectors.groupingBy(RecetteEntity::getTypeRecette, Collectors.counting()));

            Map<String, Long> cuisineTrends = recentInteractions.stream()
                    .map(i -> recettesMap.get(i.getIdRecette()))
                    .filter(r -> r != null && r.getCuisine() != null)
                    .collect(Collectors.groupingBy(RecetteEntity::getCuisine, Collectors.counting()));

            Map<String, Long> difficultyTrends = recentInteractions.stream()
                    .map(i -> recettesMap.get(i.getIdRecette()))
                    .filter(r -> r != null && r.getDifficulte() != null)
                    .collect(Collectors.groupingBy(RecetteEntity::getDifficulte, Collectors.counting()));

            try (VertexAI vertexAI = new VertexAI(projectId, location)) {

                GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);

                StringBuilder prompt = new StringBuilder();
                prompt.append("Analyse ces tendances de consultations de recettes sur les 30 derniers jours:\n\n");

                prompt.append("TENDANCES PAR TYPE DE RECETTE:\n");
                typeRecetteTrends.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .forEach(e -> prompt.append(String.format("  - %s: %d consultations\n", e.getKey(), e.getValue())));

                prompt.append("\nTENDANCES PAR TYPE DE CUISINE:\n");
                cuisineTrends.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(10)
                        .forEach(e -> prompt.append(String.format("  - %s: %d consultations\n", e.getKey(), e.getValue())));

                prompt.append("\nTENDANCES PAR DIFFICULTÉ:\n");
                difficultyTrends.forEach((diff, count) ->
                        prompt.append(String.format("  - %s: %d consultations\n", diff, count)));

                prompt.append("\nTotal d'interactions: ").append(recentInteractions.size()).append("\n");

                prompt.append("\nIDENTIFIE et EXPLIQUE:\n");
                prompt.append("1. Les 3 tendances principales\n");
                prompt.append("2. Les préférences culinaires dominantes\n");
                prompt.append("3. Le niveau d'expertise général\n");
                prompt.append("4. Les opportunités à développer\n");

                GenerateContentResponse response = model.generateContent(prompt.toString());
                String analysis = ResponseHandler.getText(response);

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

    // ─────────────────────────────────────────────────────────────────
    //  SEGMENTATION RFM
    // ─────────────────────────────────────────────────────────────────

    public Map<String, Object> segmentUsers(List<Map<String, Object>> userStatistics) {

        logger.info("Segmentation de {} utilisateurs", userStatistics.size());

        try {
            try (VertexAI vertexAI = new VertexAI(projectId, location)) {

                GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);

                StringBuilder prompt = new StringBuilder();
                prompt.append("Segmente ces utilisateurs selon leur comportement RFM:\n\n");

                userStatistics.stream().limit(50).forEach(stat ->
                        prompt.append(String.format(
                                "User %s: Recency=%d jours, Frequency=%d, Engagement=%s\n",
                                stat.get("user_id"), stat.get("recency"),
                                stat.get("frequency"), stat.get("engagement_score"))));

                prompt.append("\nCrée 5 segments (Champions, Fidèles, Nouveaux, À Risque, Perdus).\n");
                prompt.append("Pour chaque segment: critères de segmentation + actions recommandées.\n");

                GenerateContentResponse response = model.generateContent(prompt.toString());

                Map<String, Object> result = new HashMap<>();
                result.put("total_users", userStatistics.size());
                result.put("segmentation_analysis", ResponseHandler.getText(response));
                result.put("segment_distribution", calculateSegmentDistribution(userStatistics));
                result.put("generated_at", LocalDateTime.now());

                return result;
            }

        } catch (Exception e) {
            logger.error("Erreur lors de la segmentation des utilisateurs", e);
            return Map.of("error", "Impossible de générer la segmentation");
        }
    }

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
            if (recency <= 7 && frequency >= 10)       segment = "Champions";
            else if (recency <= 30 && frequency >= 5)  segment = "Fidèles";
            else if (recency <= 7 && frequency < 5)    segment = "Nouveaux";
            else if (recency > 30 && frequency >= 5)   segment = "À Risque";
            else                                        segment = "Perdus";

            distribution.put(segment, distribution.get(segment) + 1);
        }

        return distribution;
    }

    // ─────────────────────────────────────────────────────────────────
    //  UTILITAIRES
    // ─────────────────────────────────────────────────────────────────

    private List<Long> parseRecommendedIds(String response) {
        List<Long> ids = new ArrayList<>();
        try {
            String cleaned = response.replaceAll("[^0-9,]", "").trim();
            for (String part : cleaned.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && trimmed.matches("\\d+")) {
                    ids.add(Long.parseLong(trimmed));
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors du parsing des IDs: {}", response, e);
        }
        return ids;
    }

    private List<RecetteResponseDTO> getFallbackRecommendations(List<RecetteEntity> allRecipes) {
        logger.info("Utilisation des recommandations de fallback (popularité)");
        return allRecipes.stream()
                .sorted((r1, r2) -> Integer.compare(
                        r2.getImageUrl() != null ? 1 : 0,
                        r1.getImageUrl() != null ? 1 : 0))
                .limit(MAX_RECOMMENDATIONS)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private List<RecetteResponseDTO> getSameTypeRecipes(RecetteEntity targetRecipe, List<RecetteEntity> allRecipes) {
        return allRecipes.stream()
                .filter(r -> !r.getId().equals(targetRecipe.getId()))
                .filter(r -> Objects.equals(r.getTypeRecette(), targetRecipe.getTypeRecette()))
                .limit(MAX_RECOMMENDATIONS)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ CORRECTION : ajout des nouveaux champs (popularite, categorie, saison, typeCuisine)
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
        dto.setPopularite(entity.getPopularite());
        dto.setCategorie(entity.getCategorie());
        dto.setSaison(entity.getSaison());
        dto.setTypeCuisine(entity.getTypeCuisine());
        return dto;
    }
}