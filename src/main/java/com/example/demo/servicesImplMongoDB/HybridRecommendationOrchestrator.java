package com.example.demo.servicesImplMongoDB;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.DTO.RecetteResponseDTO;
import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.entiesMongodb.RecetteInteraction;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.repositoryMysql.RecetteRepository;
import com.example.demo.servicesMongoDB.GeminiRecommendationService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HybridRecommendationOrchestrator {

    @Autowired
    private RecommandationIAServiceImpl votreSysteme;

    @Autowired
    private GeminiRecommendationService aiSysteme;

    @Autowired
    private RecetteRepository recetteRepository;

    public List<RecommandationIA> getRecommendations(Long userId, String strategy) {
        switch (strategy) {
            case "CLASSIC":
                return votreSysteme.getRecommandationsAvecScore(userId);
            case "AI_ONLY":
                return getPureAIRecommendations(userId);
            case "HYBRID":
            default:
                return combineRecommendations(userId);
        }
    }

    private List<RecommandationIA> getPureAIRecommendations(Long userId) {
        List<RecetteInteraction> history = new ArrayList<>();
        List<RecetteEntity> allRecipes = recetteRepository.findAll();
        List<NoteDocument> ratings = new ArrayList<>();

        List<RecetteResponseDTO> aiRecipes = aiSysteme.getPersonalizedRecommendations(
                userId, history, allRecipes, ratings);

        return convertToRecommandationIA(aiRecipes, userId);
    }

    private List<RecommandationIA> combineRecommendations(Long userId) {
        List<RecommandationIA> classic = votreSysteme.getRecommandationsAvecScore(userId);

        List<RecetteEntity> allRecipes = recetteRepository.findAll();
        List<RecetteResponseDTO> aiRecipes = aiSysteme.getPersonalizedRecommendations(
                userId, new ArrayList<>(), allRecipes, new ArrayList<>());

        // Booster les scores si l'IA valide la recommandation classique
        Set<Long> aiRecipeIds = aiRecipes.stream()
                .map(RecetteResponseDTO::getId)
                .collect(Collectors.toSet());

        for (RecommandationIA recom : classic) {
            double aiBoost = calculateAIBoost(recom, aiRecipeIds);
            if (recom.getScore() != null) {
                recom.setScore(recom.getScore() * (1 + aiBoost));
            }
        }

        List<RecommandationIA> combined = new ArrayList<>(classic);
        combined.addAll(filterUniqueAIRecommendations(aiRecipes, classic, userId));

        combined.sort((a, b) -> Double.compare(
                b.getScore() != null ? b.getScore() : 0.0,
                a.getScore() != null ? a.getScore() : 0.0));

        return combined.subList(0, Math.min(10, combined.size()));
    }

    // ─────────────────────────────────────────────────────────────────

    
    private List<RecommandationIA> filterUniqueAIRecommendations(
            List<RecetteResponseDTO> aiRecipes,
            List<RecommandationIA> classic,
            Long userId) {

        Set<Long> classicRecetteIds = classic.stream()
                .map(RecommandationIA::getRecetteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<RecommandationIA> uniqueAI = new ArrayList<>();
        for (RecetteResponseDTO aiDto : aiRecipes) {
            if (!classicRecetteIds.contains(aiDto.getId())) {
                RecommandationIA rec = new RecommandationIA();
                rec.setRecetteId(aiDto.getId());
                rec.setUserId(userId);
                rec.setScore(0.70);
                uniqueAI.add(rec);
            }
        }
        return uniqueAI;
    }

   
    private List<RecommandationIA> convertToRecommandationIA(List<RecetteResponseDTO> dtos, Long userId) {
        List<RecommandationIA> results = new ArrayList<>();
        for (RecetteResponseDTO dto : dtos) {
            RecommandationIA r = new RecommandationIA();
            r.setRecetteId(dto.getId());
            r.setUserId(userId);
            r.setScore(0.80);
            results.add(r);
        }
        return results;
    }

    
    private double calculateAIBoost(RecommandationIA classic, Set<Long> aiRecipeIds) {
        return (classic.getRecetteId() != null && aiRecipeIds.contains(classic.getRecetteId()))
                ? 0.25
                : 0.0;
    }
}