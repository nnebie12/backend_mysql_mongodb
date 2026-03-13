package com.example.demo.servicesImplMongoDB;

import com.example.demo.DTO.RecetteResponseDTO;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entiesMongodb.RecetteInteraction;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.servicesMongoDB.OllamaService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service Ollama — moteur IA local (http://localhost:11434).
 * Pas d'interface propre : injecté directement dans RecommandationIAServiceImpl.
 */
@Service
public class OllamaRecommendationServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(OllamaRecommendationServiceImpl.class);

    @Autowired
    private OllamaService ollamaService;

    public List<RecetteResponseDTO> getPersonalizedRecommendations(
            Long userId,
            List<RecetteInteraction> history,
            List<RecetteEntity> allRecipes,
            List<NoteDocument> userRatings) {

        if (allRecipes == null || allRecipes.isEmpty()) return List.of();

        Set<Long> seenIds = history == null ? new HashSet<>() :
            history.stream().map(RecetteInteraction::getIdRecette)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Set<String> preferredTypes    = topKeys(buildTypeFrequency(history, allRecipes), 3);
        Set<String> preferredCuisines = topKeys(buildCuisineFrequency(history, allRecipes), 3);

        List<RecetteEntity> candidates = allRecipes.stream()
            .filter(r -> !seenIds.contains(r.getId())).collect(Collectors.toList());
        if (candidates.isEmpty()) candidates = new ArrayList<>(allRecipes);

        Map<RecetteEntity, Double> scored = new LinkedHashMap<>();
        for (RecetteEntity r : candidates) {
            double score = 0.0;
            if (r.getTypeRecette() != null && preferredTypes.contains(r.getTypeRecette()))   score += 3.0;
            if (r.getCuisine()     != null && preferredCuisines.contains(r.getCuisine()))    score += 2.0;
            if (userRatings != null) {
                score += userRatings.stream()
                    .filter(n -> String.valueOf(r.getId()).equals(String.valueOf(n.getRecetteId())))
                    .mapToInt(NoteDocument::getValeur).average().orElse(0.0);
            }
            scored.put(r, score);
        }

        List<RecetteEntity> top20 = scored.entrySet().stream()
            .sorted(Map.Entry.<RecetteEntity, Double>comparingByValue().reversed())
            .limit(20).map(Map.Entry::getKey).collect(Collectors.toList());

        return rankWithOllama(top20, preferredTypes, preferredCuisines)
            .stream().limit(10).map(this::toDTO).collect(Collectors.toList());
    }

    public List<RecetteResponseDTO> findSimilarRecipes(RecetteEntity target,
                                                        List<RecetteEntity> allRecipes) {
        if (target == null || allRecipes == null) return List.of();
        return allRecipes.stream()
            .filter(r -> !r.getId().equals(target.getId()))
            .sorted(Comparator.comparingDouble(r -> -structuralSimilarity(target, r)))
            .limit(10).map(this::toDTO).collect(Collectors.toList());
    }

    public Map<String, Object> detectTrends(List<RecetteInteraction> allInteractions) {
        Map<String, Object> trends = new LinkedHashMap<>();

        if (allInteractions == null || allInteractions.isEmpty()) {
            trends.put("trending_categories", List.of("Général"));
            trends.put("message", "Pas assez de données");
            return trends;
        }

        Map<Long, Long> recetteCount = allInteractions.stream()
            .filter(i -> i.getIdRecette() != null)
            .collect(Collectors.groupingBy(RecetteInteraction::getIdRecette, Collectors.counting()));

        Map<String, Long> typeCount = allInteractions.stream()
            .filter(i -> i.getTypeInteraction() != null)
            .collect(Collectors.groupingBy(RecetteInteraction::getTypeInteraction, Collectors.counting()));

        trends.put("top_recipe_ids", recetteCount.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(10).map(Map.Entry::getKey).collect(Collectors.toList()));
        trends.put("interaction_by_type", typeCount);
        trends.put("total_interactions",  allInteractions.size());
        trends.put("trending_categories", List.of("Populaire", "Récent", "Tendance"));

        if (ollamaService.isAvailable() && !typeCount.isEmpty()) {
            try {
                String raw = ollamaService.generate(
                    "En une phrase courte en français, décris les tendances culinaires d'après : " + typeCount);
                if (!raw.isBlank()) trends.put("ai_summary", raw);
            } catch (Exception e) {
                logger.debug("Ollama indisponible pour résumé tendances : {}", e.getMessage());
            }
        }
        return trends;
    }

    // ── Privé ────────────────────────────────────────────────────────────────

    private List<RecetteEntity> rankWithOllama(List<RecetteEntity> candidates,
                                                Set<String> types, Set<String> cuisines) {
        if (!ollamaService.isAvailable() || candidates.isEmpty()) return candidates;
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < candidates.size(); i++) {
                RecetteEntity r = candidates.get(i);
                sb.append(i).append(". ").append(r.getTitre())
                  .append(" (type=").append(r.getTypeRecette())
                  .append(", cuisine=").append(r.getCuisine() != null ? r.getCuisine() : "N/A")
                  .append(", difficulté=").append(r.getDifficulte()).append(")\n");
            }
            String prompt = String.format(
                "L'utilisateur préfère les types %s et les cuisines %s.\nRecettes :\n%s\n" +
                "Donne les 10 indices (0 à %d) les plus pertinents séparés par des virgules.\n" +
                "Réponds UNIQUEMENT avec les indices. Exemple : 3,0,7,2,5",
                types, cuisines, sb, candidates.size() - 1);

            List<Integer> indices = Arrays.stream(ollamaService.generate(prompt).split(","))
                .map(String::trim).filter(s -> s.matches("\\d+")).map(Integer::parseInt)
                .filter(i -> i >= 0 && i < candidates.size()).distinct().collect(Collectors.toList());

            if (indices.isEmpty()) return candidates;

            Set<Integer> used = new HashSet<>(indices);
            List<RecetteEntity> ranked = new ArrayList<>();
            indices.forEach(i -> ranked.add(candidates.get(i)));
            for (int i = 0; i < candidates.size(); i++) if (!used.contains(i)) ranked.add(candidates.get(i));
            return ranked;
        } catch (Exception e) {
            logger.debug("Ollama ranking indisponible : {}", e.getMessage());
            return candidates;
        }
    }

    private double structuralSimilarity(RecetteEntity a, RecetteEntity b) {
        double s = 0;
        if (a.getTypeRecette() != null && a.getTypeRecette().equals(b.getTypeRecette())) s += 3;
        if (a.getCuisine()     != null && a.getCuisine().equals(b.getCuisine()))         s += 2;
        if (a.getDifficulte()  != null && a.getDifficulte().equals(b.getDifficulte()))   s += 1;
        if (a.getVegetarien()  != null && a.getVegetarien().equals(b.getVegetarien()))   s += 1;
        if (a.getRecetteIngredients() != null && b.getRecetteIngredients() != null) {
            Set<Long> ia = a.getRecetteIngredients().stream()
                .map(ri -> ri.getIngredientEntity().getId()).collect(Collectors.toSet());
            Set<Long> ib = b.getRecetteIngredients().stream()
                .map(ri -> ri.getIngredientEntity().getId()).collect(Collectors.toSet());
            Set<Long> inter = new HashSet<>(ia); inter.retainAll(ib);
            if (!ia.isEmpty()) s += (double) inter.size() / ia.size() * 2;
        }
        return s;
    }

    private Map<String, Long> buildTypeFrequency(List<RecetteInteraction> h, List<RecetteEntity> all) {
        if (h == null) return Map.of();
        Set<Long> ids = h.stream().map(RecetteInteraction::getIdRecette)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        return all.stream().filter(r -> ids.contains(r.getId()) && r.getTypeRecette() != null)
            .collect(Collectors.groupingBy(RecetteEntity::getTypeRecette, Collectors.counting()));
    }

    private Map<String, Long> buildCuisineFrequency(List<RecetteInteraction> h, List<RecetteEntity> all) {
        if (h == null) return Map.of();
        Set<Long> ids = h.stream().map(RecetteInteraction::getIdRecette)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        return all.stream().filter(r -> ids.contains(r.getId()) && r.getCuisine() != null)
            .collect(Collectors.groupingBy(RecetteEntity::getCuisine, Collectors.counting()));
    }

    private Set<String> topKeys(Map<String, Long> freq, int n) {
        return freq.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(n).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private RecetteResponseDTO toDTO(RecetteEntity e) {
        RecetteResponseDTO dto = new RecetteResponseDTO();
        dto.setId(e.getId()); dto.setTitre(e.getTitre()); dto.setDescription(e.getDescription());
        dto.setTempsPreparation(e.getTempsPreparation()); dto.setTempsCuisson(e.getTempsCuisson());
        dto.setDifficulte(e.getDifficulte()); dto.setTypeRecette(e.getTypeRecette());
        dto.setCuisine(e.getCuisine()); dto.setImageUrl(e.getImageUrl());
        dto.setVegetarien(e.getVegetarien()); dto.setPopularite(e.getPopularite());
        dto.setCategorie(e.getCategorie()); dto.setSaison(e.getSaison());
        dto.setTypeCuisine(e.getTypeCuisine());
        return dto;
    }
}