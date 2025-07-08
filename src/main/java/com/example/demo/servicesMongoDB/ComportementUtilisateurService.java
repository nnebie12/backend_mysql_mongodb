package com.example.demo.servicesMongoDB;

import java.util.List;
import java.util.Optional;

import com.example.demo.DTO.AnalysePatternsDTO;
import com.example.demo.entiesMongodb.ComportementUtilisateur;
import com.example.demo.entiesMongodb.HistoriqueRecherche;
import com.example.demo.entiesMongodb.enums.ProfilUtilisateur; 

/**
 * Service pour la gestion du comportement utilisateur
 * Gère l'analyse et le suivi des patterns de comportement des utilisateurs
 */
public interface ComportementUtilisateurService {
    
    /**
     * Crée un nouveau profil de comportement pour un utilisateur
     * @param userId ID de l'utilisateur
     * @return ComportementUtilisateur créé (retourne toujours l'entité car c'est la couche métier)
     */
    ComportementUtilisateur createBehavior(Long userId);
    
    /**
     * Analyse les patterns comportementaux d'un utilisateur
     * @param userId ID de l'utilisateur
     * @return AnalysePatternsDTO contenant l'analyse des patterns
     */
    AnalysePatternsDTO analyserPatternsDTO(Long userId);    
    /**
     * Récupère le comportement d'un utilisateur par son ID
     * @param userId ID de l'utilisateur
     * @return Optional contenant le comportement ou vide si non trouvé (retourne toujours l'entité)
     */
    Optional<ComportementUtilisateur> getBehaviorByUserId(Long userId);
    
    /**
     * Récupère ou crée le comportement d'un utilisateur
     * @param userId ID de l'utilisateur
     * @return ComportementUtilisateur existant ou nouvellement créé (retourne toujours l'entité)
     */
    ComportementUtilisateur getOrCreateBehavior(Long userId);
    
    /**
     * Met à jour le comportement utilisateur
     * @param comportement Comportement à mettre à jour (accepte l'entité)
     * @return ComportementUtilisateur mis à jour (retourne toujours l'entité)
     */
    ComportementUtilisateur updateBehavior(ComportementUtilisateur comportement);
    
    /**
     * Met à jour les métriques comportementales depuis les autres entités
     * @param userId ID de l'utilisateur
     */
    void updateMetrics(Long userId);
    
    /**
     * Enregistre une nouvelle recherche et met à jour le comportement
     * @param userId ID de l'utilisateur
     * @param terme Terme de recherche
     * @param filtres Filtres appliqués
     * @param nombreResultats Nombre de résultats obtenus
     * @param rechercheFructueuse Indique si la recherche a été fructueuse
     * @return ComportementUtilisateur mis à jour (retourne toujours l'entité)
     */
    ComportementUtilisateur recordSearch(Long userId, String terme,
                                         List<HistoriqueRecherche.Filtre> filtres,
                                         Integer nombreResultats,
                                         Boolean rechercheFructueuse);
    
    /**
     * Récupère les termes de recherche les plus fréquents pour un utilisateur
     * @param userId ID de l'utilisateur
     * @return Liste des termes fréquents (limité à 10)
     */
    List<String> getFrequentSearchTerms(Long userId);
    
    /**
     * Récupère tous les utilisateurs ayant un profil spécifique
     * @param profil Type de profil recherché (accepte l'enum ProfilUtilisateur)
     * @return Liste des utilisateurs correspondants (retourne toujours l'entité)
     */
    List<ComportementUtilisateur> getUsersByProfile(ProfilUtilisateur profil); 
    
    /**
     * Récupère les utilisateurs ayant un score d'engagement supérieur au minimum
     * @param scoreMinimum Score minimum d'engagement
     * @return Liste des utilisateurs engagés (retourne toujours l'entité)
     */
    List<ComportementUtilisateur> getEngagedUsers(Double scoreMinimum);
    
    /**
     * Supprime le comportement d'un utilisateur
     * @param userId ID de l'utilisateur
     */
    void deleteUserBehavior(Long userId);
    
    /**
     * Enregistre une interaction utilisateur et met à jour le comportement
     * @param userId ID de l'utilisateur
     * @param typeInteraction Type d'interaction (VIEW, SEARCH, FAVORITE, etc.)
     * @param contenu Contenu de l'interaction
     * @param contexte Contexte additionnel (optionnel)
     * @return ComportementUtilisateur mis à jour (retourne toujours l'entité)
     */
    ComportementUtilisateur enregistrerInteraction(Long userId, String typeInteraction, 
                                                   String contenu, String contexte);
    
    /**
     * Analyse et met à jour les patterns comportementaux
     * @param userId ID de l'utilisateur
     * @return ComportementUtilisateur avec patterns mis à jour (retourne toujours l'entité)
     */
    ComportementUtilisateur analyserPatterns(Long userId);
    
    /**
     * Récupère les statistiques comportementales d'un utilisateur
     * @param userId ID de l'utilisateur
     * @return Map contenant les statistiques
     */
    java.util.Map<String, Object> obtenirStatistiquesComportement(Long userId);
}