package com.example.demo.servicesMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.example.demo.entiesMongodb.HistoriqueRecherche;

public interface HistoriqueRechercheService {
    
    // Méthodes existantes
    HistoriqueRecherche enregistrerRecherche(Long userId, String terme, 
                                           List<HistoriqueRecherche.Filtre> filtres);
    
    List<HistoriqueRecherche> getHistoriqueByUserId(Long userId);
    List<HistoriqueRecherche> getRecherchesSimilaires(String terme);
    void supprimerHistoriqueUtilisateur(Long userId);
    
    // Nouvelles méthodes enrichies
    HistoriqueRecherche enregistrerRechercheComplete(Long userId, String terme,
                                                   List<HistoriqueRecherche.Filtre> filtres,
                                                   Integer nombreResultats,
                                                   Boolean rechercheFructueuse,
                                                   String contexteRecherche,
                                                   String sourceRecherche);
    
    // Analyse et statistiques
    Map<String, Long> getStatistiquesRecherche(Long userId);
    List<String> getTermesFrequents(Long userId, int limite);
    Double getTauxRecherchesFructueuses(Long userId);
    List<HistoriqueRecherche> getRecherchesPeriode(Long userId, LocalDateTime debut, LocalDateTime fin);
    
    // Recommandations basées sur l'historique
    List<String> getSuggestionsRecherche(Long userId);
    List<String> getTermesTendance();
    
    // Nettoyage et maintenance
    void nettoyerAnciennesRecherches(int joursRetention);
}