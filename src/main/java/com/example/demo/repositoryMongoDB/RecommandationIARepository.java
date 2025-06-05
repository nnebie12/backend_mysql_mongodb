package com.example.demo.repositoryMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entiesMongodb.RecommandationIA;

@Repository
public interface RecommandationIARepository extends MongoRepository<RecommandationIA, String> {
    
    List<RecommandationIA> findByUserId(Long userId);
    List<RecommandationIA> findByUserIdAndType(Long userId, String type);
    List<RecommandationIA> findByUserIdAndEstUtilise(Long userId, Boolean estUtilise);
    List<RecommandationIA> findByType(String type);
    List<RecommandationIA> findByUserIdAndDateRecommandationAfter(Long userId, LocalDateTime date);
    List<RecommandationIA> findByUserIdAndScoreGreaterThan(Long userId, Double score);
    List<RecommandationIA> findByUserIdAndTypeAndEstUtilise(Long userId, String type, Boolean estUtilise);
    List<RecommandationIA> findByEstUtiliseFalse();
    List<RecommandationIA> findByUserIdOrderByScoreDesc(Long userId);
    List<RecommandationIA> findByUserIdOrderByDateRecommandationDesc(Long userId);
    
    // Méthodes pour l'intégration avec ComportementUtilisateur
    List<RecommandationIA> findByUserIdAndProfilUtilisateurCible(Long userId, String profilUtilisateurCible);
    List<RecommandationIA> findByUserIdAndCreneauCible(Long userId, String creneauCible);
    List<RecommandationIA> findByUserIdAndScoreEngagementReferenceGreaterThan(Long userId, Double scoreEngagement);
    List<RecommandationIA> findByUserIdAndCategoriesRecommandees(Long userId, String categorie);
    List<RecommandationIA> findByComportementUtilisateurId(String comportementUtilisateurId);
    List<RecommandationIA> findByUserIdAndComportementUtilisateurId(Long userId, String comportementUtilisateurId);
    
    // Méthodes de recherche avancée
    List<RecommandationIA> findByUserIdAndTypeAndProfilUtilisateurCible(Long userId, String type, String profilUtilisateurCible);
    List<RecommandationIA> findByUserIdAndCreneauCibleAndEstUtilise(Long userId, String creneauCible, Boolean estUtilise);
    List<RecommandationIA> findByUserIdAndDateRecommandationBetween(Long userId, LocalDateTime dateDebut, LocalDateTime dateFin);
    List<RecommandationIA> findByUserIdAndScoreBetween(Long userId, Double scoreMin, Double scoreMax);
    
    // Méthodes de tri et limitation
    List<RecommandationIA> findTop5ByUserIdOrderByScoreDesc(Long userId);
    List<RecommandationIA> findTop10ByUserIdAndEstUtiliseFalseOrderByScoreDesc(Long userId);
    List<RecommandationIA> findByUserIdAndTypeOrderByDateRecommandationDesc(Long userId, String type);
    
    // Méthodes de comptage
    Long countByUserId(Long userId);
    Long countByUserIdAndEstUtilise(Long userId, Boolean estUtilise);
    Long countByUserIdAndType(Long userId, String type);
    Long countByUserIdAndProfilUtilisateurCible(Long userId, String profilUtilisateurCible);
    Long countByUserIdAndCreneauCible(Long userId, String creneauCible);
    
    // Méthodes de suppression
    void deleteByUserId(Long userId);
    void deleteByUserIdAndType(Long userId, String type);
    void deleteByUserIdAndEstUtilise(Long userId, Boolean estUtilise);
    void deleteByComportementUtilisateurId(String comportementUtilisateurId);
    
    // Méthodes d'existence
    boolean existsByUserIdAndType(Long userId, String type);
    boolean existsByUserIdAndComportementUtilisateurId(Long userId, String comportementUtilisateurId);
}