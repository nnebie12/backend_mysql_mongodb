package com.example.demo.servicesMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.example.demo.entiesMongodb.HistoriqueRecherche;

public interface HistoriqueRechercheService {
    
    HistoriqueRecherche recordSearch(Long userId, String terme, 
                                           List<HistoriqueRecherche.Filtre> filtres);
    
    List<HistoriqueRecherche> getHistoryByUserId(Long userId);
    List<HistoriqueRecherche> getSimilarSearches(String terme);
    void deleteUserHistory(Long userId);
    
    HistoriqueRecherche recordCompleteSearch(Long userId, String terme,
                                                   List<HistoriqueRecherche.Filtre> filtres,
                                                   Integer nombreResultats,
                                                   Boolean rechercheFructueuse,
                                                   String contexteRecherche,
                                                   String sourceRecherche);
    
    // Analyse et statistiques
    Map<String, Long> getSearchStatistics(Long userId);
    List<String> getFrequentTerms(Long userId, int limite);
    Double getSuccessfulSearchRate(Long userId);
    List<HistoriqueRecherche> getSearchesByPeriod(Long userId, LocalDateTime debut, LocalDateTime fin);
    
    // Recommandations basées sur l'historique
    List<String> getSearchSuggestions(Long userId);
    List<String> getTrendingTerms();
    
    // Nettoyage et maintenance
    void cleanOldSearches(int joursRetention);
}