package com.example.demo.repositoryMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entiesMongodb.HistoriqueRecherche;

@Repository
public interface HistoriqueRechercheRepository extends MongoRepository<HistoriqueRecherche, String> {

    List<HistoriqueRecherche> findByUserIdOrderByDateRechercheDesc(Long userId);
    List<HistoriqueRecherche> findByTermeContaining(String terme);
    List<HistoriqueRecherche> findByUserId(Long userId);

    //  méthodes pour l'analyse comportementale
    List<HistoriqueRecherche> findByUserIdAndDateRechercheAfter(Long userId, LocalDateTime date);

    // Recherches fréquentes par utilisateur avec pagination
    Page<HistoriqueRecherche> findByUserIdOrderByDateRechercheDesc(Long userId, Pageable pageable);

    List<HistoriqueRecherche> findByUserIdAndRechercheFructueuse(Long userId, Boolean fructueuse);
    
    List<HistoriqueRecherche> findByUserIdAndDateRechercheBetween(Long userId, 
                                                                  LocalDateTime debut, 
                                                                  LocalDateTime fin);

    // Recherches similaires améliorées
    List<HistoriqueRecherche> findByTermeContainingIgnoreCase(String terme);
    List<HistoriqueRecherche> findByTermeIgnoreCase(String terme);
    List<HistoriqueRecherche> findByTermeStartingWithIgnoreCase(String terme);
    List<HistoriqueRecherche> findByTermeEndingWithIgnoreCase(String terme);

    // Nettoyage des anciennes recherches
    void deleteByDateRechercheBefore(LocalDateTime date);
    void deleteByUserIdAndDateRechercheBefore(Long userId, LocalDateTime date);

    // Comptages pour statistiques
    long countByUserId(Long userId);
    long countByUserIdAndRechercheFructueuse(Long userId, Boolean fructueuse);
    long countByUserIdAndDateRechercheAfter(Long userId, LocalDateTime date);
    long countByUserIdAndDateRechercheBetween(Long userId, LocalDateTime debut, LocalDateTime fin);
    
    // Méthodes de recherche avancées sans @Query
    List<HistoriqueRecherche> findByUserIdAndTermeContaining(Long userId, String terme);
    List<HistoriqueRecherche> findByUserIdAndTermeContainingIgnoreCase(Long userId, String terme);
    List<HistoriqueRecherche> findByDateRechercheAfter(LocalDateTime date);
    List<HistoriqueRecherche> findByDateRechercheBefore(LocalDateTime date);
    List<HistoriqueRecherche> findByDateRechercheBetween(LocalDateTime debut, LocalDateTime fin);
    
    // Recherches pour l'analyse comportementale
    List<HistoriqueRecherche> findByUserIdAndNombreResultatsGreaterThan(Long userId, Integer nombreResultats);
    List<HistoriqueRecherche> findByUserIdOrderByNombreResultatsDesc(Long userId);
    List<HistoriqueRecherche> findByRechercheFructueuse(Boolean fructueuse);
    
    // Pagination et tri
    Page<HistoriqueRecherche> findByUserIdAndRechercheFructueuse(Long userId, Boolean fructueuse, Pageable pageable);
    Page<HistoriqueRecherche> findByUserIdAndDateRechercheAfter(Long userId, LocalDateTime date, Pageable pageable);
    Page<HistoriqueRecherche> findByTermeContainingIgnoreCase(String terme, Pageable pageable);
    
    // Top recherches
    List<HistoriqueRecherche> findTop10ByUserIdOrderByDateRechercheDesc(Long userId);
    List<HistoriqueRecherche> findTop5ByUserIdAndRechercheFructueuseOrderByDateRechercheDesc(Long userId, Boolean fructueuse);
    
    // Existence et vérifications
    boolean existsByUserIdAndTerme(Long userId, String terme);
    boolean existsByUserIdAndTermeIgnoreCase(Long userId, String terme);
    boolean existsByUserIdAndDateRechercheAfter(Long userId, LocalDateTime date);
	List<HistoriqueRecherche> findAllByDateRechercheAfter(LocalDateTime uneSemaineAgo);
}