package com.example.demo.servicesMongoDB;

import java.util.List;
import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.entiesMongodb.RecommandationIA.RecommandationDetail;
import com.example.demo.entiesMongodb.ComportementUtilisateur;

public interface RecommandationIAService {
    
    RecommandationIA addRecommandation(Long userId, String type, List<RecommandationDetail> recommandation, Double score);
    List<RecommandationIA> getRecommandationsByUserId(Long userId);
    List<RecommandationIA> getRecommandationsByUserIdAndType(Long userId, String type);
    RecommandationIA markAsUsed(String recommandationId);
    void deleteRecommandationsUser(Long userId);
    
    
    /**
     * Génère des recommandations personnalisées basées sur le comportement utilisateur
     * @param userId ID de l'utilisateur
     * @return RecommandationIA générée
     */
    RecommandationIA genererRecommandationPersonnalisee(Long userId);
    
    /**
     * Génère des recommandations saisonnières basées sur les préférences
     * @param userId ID de l'utilisateur
     * @return RecommandationIA pour la saison actuelle
     */
    RecommandationIA genererRecommandationSaisonniere(Long userId);
    
    /**
     * Génère des recommandations basées sur les habitudes de navigation
     * @param userId ID de l'utilisateur
     * @return RecommandationIA ciblée sur les habitudes
     */
    RecommandationIA genererRecommandationHabitudes(Long userId);
    
    /**
     * Génère des recommandations pour le créneau horaire actuel
     * @param userId ID de l'utilisateur
     * @return RecommandationIA adaptée au moment
     */
    RecommandationIA genererRecommandationCreneauActuel(Long userId);
    
    /**
     * Met à jour le score d'une recommandation basé sur l'utilisation
     * @param recommandationId ID de la recommandation
     * @param comportement Comportement utilisateur actuel
     * @return RecommandationIA mise à jour
     */
    RecommandationIA mettreAJourScoreRecommandation(String recommandationId, ComportementUtilisateur comportement);
    
    /**
     * Obtient des recommandations adaptées au profil utilisateur
     * @param userId ID de l'utilisateur
     * @return Liste de recommandations ciblées
     */
    List<RecommandationIA> getRecommandationsParProfil(Long userId);
    
    /**
     * Génère des recommandations d'amélioration d'engagement
     * @param userId ID de l'utilisateur
     * @return RecommandationIA pour améliorer l'engagement
     */
    RecommandationIA genererRecommandationEngagement(Long userId);
}