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

// Importez vos repositories MongoDB ici (ex: InteractionRepository, NoteRepository)
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

    // Ajoutez ici vos repositories pour MongoDB pour récupérer history et ratings
    // @Autowired private InteractionRepository interactionRepo;

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
        // 1. Préparer les données pour Gemini (Simulé ici, utilisez vos repos)
        List<RecetteInteraction> history = new ArrayList<>(); // TODO: repo.findByUserId(userId)
        List<RecetteEntity> allRecipes = recetteRepository.findAll();
        List<NoteDocument> ratings = new ArrayList<>(); // TODO: repo.findByUserId(userId)

        // 2. Appel au service avec les bons arguments
        List<RecetteResponseDTO> aiRecipes = aiSysteme.getPersonalizedRecommendations(
            userId, history, allRecipes, ratings
        );
        return convertToRecommandationIA(aiRecipes);
    }

    private List<RecommandationIA> combineRecommendations(Long userId) {
        List<RecommandationIA> classic = votreSysteme.getRecommandationsAvecScore(userId);
        
        // Récupération pour l'IA
        List<RecetteEntity> allRecipes = recetteRepository.findAll();
        List<RecetteResponseDTO> aiRecipes = aiSysteme.getPersonalizedRecommendations(
            userId, new ArrayList<>(), allRecipes, new ArrayList<>()
        );

        // Booster les scores si l'IA est d'accord
        for (RecommandationIA recom : classic) {
            double aiBoost = calculateAIBoost(recom, aiRecipes);
            recom.setScore(recom.getScore() * (1 + aiBoost));
        }

        List<RecommandationIA> combined = new ArrayList<>(classic);
        combined.addAll(filterUniqueAIRecommendations(aiRecipes, classic));
        
        combined.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return combined.subList(0, Math.min(10, combined.size()));
    }

    // --- MÉTHODES DE FILTRAGE ET CONVERSION ---

    private List<RecommandationIA> filterUniqueAIRecommendations(List<RecetteResponseDTO> aiRecipes, List<RecommandationIA> classic) {
        // On récupère les IDs des recettes déjà présentes dans le système classique
        Set<Long> classicIds = classic.stream()
                .map(r -> r.getRecetteEntity().getId())
                .collect(Collectors.toSet());

        List<RecommandationIA> uniqueAI = new ArrayList<>();
        for (RecetteResponseDTO aiDto : aiRecipes) {
            if (!classicIds.contains(aiDto.getId())) {
                // CORRECTION : Chercher l'entité par l'ID
                recetteRepository.findById(aiDto.getId()).ifPresent(entity -> {
                    RecommandationIA rec = new RecommandationIA();
                    rec.setRecetteEntity(entity); // On passe l'entité, pas le Long
                    rec.setScore(0.70);
                    uniqueAI.add(rec);
                });
            }
        }
        return uniqueAI;
    }

    private List<RecommandationIA> convertToRecommandationIA(List<RecetteResponseDTO> dtos) {
        List<RecommandationIA> results = new ArrayList<>();
        for (RecetteResponseDTO dto : dtos) {
            // CORRECTION : Conversion ID -> Entité
            recetteRepository.findById(dto.getId()).ifPresent(entity -> {
                RecommandationIA r = new RecommandationIA();
                r.setRecetteEntity(entity);
                r.setScore(0.80);
                results.add(r);
            });
        }
        return results;
    }

    private double calculateAIBoost(RecommandationIA classic, List<RecetteResponseDTO> aiRecipes) {
        return aiRecipes.stream().anyMatch(ai -> ai.getId().equals(classic.getRecetteEntity())) ? 0.25 : 0.0;
    }
}