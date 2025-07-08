package com.example.demo.repositoryMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entiesMongodb.ComportementUtilisateur;
import com.example.demo.entiesMongodb.enums.ProfilUtilisateur; 
import com.example.demo.entiesMongodb.enums.Saison; 

/**
 * Repository pour la gestion des données de comportement utilisateur en MongoDB
 */
@Repository
public interface ComportementUtilisateurRepository extends MongoRepository<ComportementUtilisateur, String> {
    
    /**
     * Trouve un comportement par l'ID utilisateur
     * @param userId ID de l'utilisateur
     * @return Optional contenant le comportement ou vide
     */
    Optional<ComportementUtilisateur> findByUserId(Long userId);
    
    /**
     * Trouve tous les utilisateurs ayant un profil spécifique
     * @param profilUtilisateur Type de profil (maintenant ProfilUtilisateur enum)
     * @return Liste des comportements correspondants
     */
    List<ComportementUtilisateur> findByMetriques_ProfilUtilisateur(ProfilUtilisateur profilUtilisateur);
    
    /**
     * Trouve les utilisateurs avec un score d'engagement supérieur au seuil
     * @param scoreEngagement Score minimum
     * @return Liste des comportements correspondants
     */
    List<ComportementUtilisateur> findByMetriques_ScoreEngagementGreaterThan(Double scoreEngagement);
    
    /**
     * Trouve les utilisateurs avec un score d'engagement dans une plage
     * @param scoreMin Score minimum
     * @param scoreMax Score maximum
     * @return Liste des comportements correspondants
     */
    List<ComportementUtilisateur> findByMetriques_ScoreEngagementBetween(Double scoreMin, Double scoreMax);
    
    /**
     * Trouve les utilisateurs actifs depuis une date donnée
     * @param dateLimit Date limite
     * @return Liste des comportements correspondants
     */
    List<ComportementUtilisateur> findByDateMiseAJourAfter(LocalDateTime dateLimit);
    
    /**
     * Trouve les utilisateurs par saison préférée
     * @param saison Saison préférée (maintenant Saison enum)
     * @return Liste des comportements correspondants
     */
    List<ComportementUtilisateur> findByPreferencesSaisonnieres_SaisonPreferee(Saison saison); // Change le type
    
    /**
     * Trouve les utilisateurs ayant un type de recette préféré
     * @param typeRecette Type de recette
     * @return Liste des comportements correspondants
     */
    List<ComportementUtilisateur> findByHabitudesNavigation_TypeRecettePreferee(String typeRecette);
    
    /**
     * Trouve les utilisateurs avec un nombre minimum de connexions par jour
     * @param nombreMin Nombre minimum de connexions
     * @return Liste des comportements correspondants
     */
    List<ComportementUtilisateur> findByHabitudesNavigation_NombreConnexionsParJourGreaterThanEqual(Integer nombreMin);
    
    /**
     * Trouve les comportements avec pagination
     * @param profil Profil utilisateur (optionnel, maintenant ProfilUtilisateur enum)
     * @param pageable Paramètres de pagination
     * @return Page de comportements
     */
    Page<ComportementUtilisateur> findByMetriques_ProfilUtilisateur(ProfilUtilisateur profil, Pageable pageable);
    
    /**
     * Compte les utilisateurs par profil
     * @param profil Type de profil (maintenant ProfilUtilisateur enum)
     * @return Nombre d'utilisateurs
     */
    long countByMetriques_ProfilUtilisateur(ProfilUtilisateur profil);
    
    /**
     * Vérifie l'existence d'un comportement pour un utilisateur
     * @param userId ID de l'utilisateur
     * @return true si existe, false sinon
     */
    boolean existsByUserId(Long userId);
    
    /**
     * Supprime un comportement par l'ID utilisateur
     * @param userId ID de l'utilisateur
     */
    void deleteByUserId(Long userId);
    
    /**
     * Supprime tous les comportements inactifs depuis une date
     * @param dateLimit Date limite d'inactivité
     * @return Nombre de documents supprimés
     */
    long deleteByDateMiseAJourBefore(LocalDateTime dateLimit);
    
    /**
     * Recherche les utilisateurs les plus actifs par score d'engagement
     * @param pageable Paramètres de pagination avec tri
     * @return Liste des utilisateurs les plus actifs
     */
    List<ComportementUtilisateur> findAllByOrderByMetriques_ScoreEngagementDesc(Pageable pageable);
    
    /**
     * Trouve les utilisateurs par créneau d'activité le plus actif
     * @param creneau Créneau le plus actif
     * @return Liste des comportements correspondants
     */
    List<ComportementUtilisateur> findByCyclesActivite_CreneauLePlusActif(String creneau);
    
    /**
     * Trouve les utilisateurs ayant un streak de connexion minimum
     * @param streakMin Streak minimum
     * @return Liste des comportements correspondants
     */
    List<ComportementUtilisateur> findByMetriques_StreakConnexionGreaterThanEqual(Integer streakMin);
    
    /**
     * Trouve les utilisateurs avec une consistance horaire élevée
     * @param consistanceMin Consistance minimum (0-100)
     * @return Liste des comportements correspondants
     */
    List<ComportementUtilisateur> findByCyclesActivite_ConsistanceHoraireGreaterThanEqual(Double consistanceMin);
    
    /**
     * Trouve les utilisateurs ayant des ingrédients spécifiques dans leurs préférences saisonnières
     * Note: Cette méthode nécessitera une implémentation personnalisée dans le service
     * car Spring Data ne supporte pas nativement les recherches OR sur plusieurs listes
     * @param ingredient Nom de l'ingrédient
     * @return Liste des comportements correspondants
     */
}