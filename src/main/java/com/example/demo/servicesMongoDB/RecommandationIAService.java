package com.example.demo.servicesMongoDB;

import java.util.List;
import java.util.Map;
import com.example.demo.DTO.RecetteResponseDTO;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.entiesMongodb.RecommandationIA.RecommandationDetail;
import com.example.demo.entiesMongodb.ComportementUtilisateur;
import com.example.demo.entiesMongodb.RecetteInteraction;
import com.example.demo.entitiesMysql.RecetteEntity;

public interface RecommandationIAService {
    
    RecommandationIA addRecommandation(Long userId, String type, List<RecommandationDetail> recommandation, Double score);
    List<RecommandationIA> getRecommandationsByUserId(Long userId);
    List<RecommandationIA> getRecommandationsByUserIdAndType(Long userId, String type);
    RecommandationIA markAsUsed(String recommandationId);
    void deleteRecommandationsUser(Long userId);
    
    RecommandationIA genererRecommandationHybride(Long userId);
    RecommandationIA genererRecommandationPersonnalisee(Long userId);
    RecommandationIA genererRecommandationSaisonniere(Long userId);
    RecommandationIA genererRecommandationHabitudes(Long userId);
    RecommandationIA genererRecommandationCreneauActuel(Long userId);
    RecommandationIA mettreAJourScoreRecommandation(String recommandationId, ComportementUtilisateur comportement);
    List<RecommandationIA> getRecommandationsParProfil(Long userId);
    RecommandationIA genererRecommandationEngagement(Long userId);
    List<RecommandationIA> getAllRecommandations();
    List<RecommandationIA> getRecommandationsAvecScore(Long userId);
    String suggererMeilleurTypeRecommandation(Long userId);


    /**
     * Recommandations personnalisées basées sur l'historique d'interactions
     */
    List<RecetteResponseDTO> getPersonalizedRecommendations(
            Long userId,
            List<RecetteInteraction> userHistory,
            List<RecetteEntity> allRecipes,
            List<NoteDocument> userRatings);

    /**
     * Recettes similaires à une recette cible
     */
    List<RecetteResponseDTO> findSimilarRecipes(
            RecetteEntity targetRecipe,
            List<RecetteEntity> allRecipes);

    /**
     * Détection des tendances sur les interactions récentes
     */
    Map<String, Object> detectTrends(List<RecetteInteraction> allInteractions);
}