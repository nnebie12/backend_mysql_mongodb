package com.example.demo.servicesImplMongoDB;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.example.demo.entiesMongodb.HistoriqueRecherche;
import com.example.demo.repositoryMongoDB.HistoriqueRechercheRepository;
import com.example.demo.servicesMongoDB.HistoriqueRechercheService;

@Service
public class HistoriqueRechercheServiceImpl implements HistoriqueRechercheService {
    
    private final HistoriqueRechercheRepository historiqueRepository;
    
    public HistoriqueRechercheServiceImpl(HistoriqueRechercheRepository historiqueRepository) {
        this.historiqueRepository = historiqueRepository;
    }
    
    @Override
    public HistoriqueRecherche recordSearch(Long userId, String terme, 
                                                  List<HistoriqueRecherche.Filtre> filtres) {
        return recordCompleteSearch(userId, terme, filtres, null, null, 
                                          "navigation", "web");
    }
    
    @Override
    public HistoriqueRecherche recordCompleteSearch(Long userId, String terme,
                                                          List<HistoriqueRecherche.Filtre> filtres,
                                                          Integer nombreResultats,
                                                          Boolean rechercheFructueuse,
                                                          String contexteRecherche,
                                                          String sourceRecherche) {
        HistoriqueRecherche historique = HistoriqueRecherche.builder()
            .userId(userId)
            .terme(terme.trim().toLowerCase())
            .filtres(filtres != null ? filtres : new ArrayList<>())
            .dateRecherche(LocalDateTime.now())
            .nombreResultats(nombreResultats)
            .rechercheFructueuse(rechercheFructueuse)
            .contexteRecherche(contexteRecherche)
            .sourceRecherche(sourceRecherche)
            .categorieRecherche(determinerCategorieRecherche(terme))
            .build();
        
        return historiqueRepository.save(historique);
    }
    
    @Override
    public List<HistoriqueRecherche> getHistoryByUserId(Long userId) {
        return historiqueRepository.findByUserIdOrderByDateRechercheDesc(userId);
    }
    
    @Override
    public List<HistoriqueRecherche> getSimilarSearches(String terme) {
        return historiqueRepository.findByTermeContainingIgnoreCase(terme);
    }
    
    @Override
    public void deleteUserHistory(Long userId) {
        List<HistoriqueRecherche> historiques = historiqueRepository.findByUserId(userId);
        historiqueRepository.deleteAll(historiques);
    }
    
    @Override
    public Map<String, Long> getSearchStatistics(Long userId) {
        List<HistoriqueRecherche> historiques = getHistoryByUserId(userId);
        Map<String, Long> stats = new HashMap<>();
        
        stats.put("total", (long) historiques.size());
        stats.put("fructueuses", historiques.stream()
            .filter(HistoriqueRecherche::estRechercheFructueuse)
            .count());
        stats.put("recentes", historiques.stream()
            .filter(HistoriqueRecherche::estRechercheRecente)
            .count());
        
        return stats;
    }
    
    @Override
    public List<String> getFrequentTerms(Long userId, int limite) {
        List<HistoriqueRecherche> historiques = getHistoryByUserId(userId);
        
        return historiques.stream()
            .collect(Collectors.groupingBy(
                HistoriqueRecherche::getTerme,
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limite)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    @Override
    public Double getSuccessfulSearchRate(Long userId) {
        long total = historiqueRepository.countByUserId(userId);
        if (total == 0) return 0.0;
        
        long fructueuses = historiqueRepository.countByUserIdAndRechercheFructueuse(userId, true);
        return (double) fructueuses / total * 100;
    }
    
    @Override
    public List<HistoriqueRecherche> getSearchesByPeriod(Long userId, LocalDateTime debut, LocalDateTime fin) {
        return historiqueRepository.findByUserIdAndDateRechercheBetween(userId, debut, fin);
    }
    
    @Override
    public List<String> getSearchSuggestions(Long userId) {
        List<String> termesFrequents = getFrequentTerms(userId, 5);
        List<String> suggestions = new ArrayList<>(termesFrequents);
        
        // Ajouter des suggestions basées sur les termes similaires
        for (String terme : termesFrequents) {
            List<HistoriqueRecherche> similaires = getSimilarSearches(terme);
            suggestions.addAll(similaires.stream()
                .map(HistoriqueRecherche::getTerme)
                .filter(t -> !suggestions.contains(t))
                .limit(2)
                .collect(Collectors.toList()));
        }
        
        return suggestions.stream().distinct().limit(10).collect(Collectors.toList());
    }
    
    @Override
    public List<String> getTrendingTerms() {
        LocalDateTime uneSemaineAgo = LocalDateTime.now().minusDays(7);
        List<HistoriqueRecherche> recherchesRecentes = 
            historiqueRepository.findAllByDateRechercheAfter(uneSemaineAgo);
        
        return recherchesRecentes.stream()
            .collect(Collectors.groupingBy(
                HistoriqueRecherche::getTerme,
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    @Override
    public void cleanOldSearches(int joursRetention) {
        LocalDateTime dateLimit = LocalDateTime.now().minusDays(joursRetention);
        historiqueRepository.deleteByDateRechercheBefore(dateLimit);
    }
    
    private String determinerCategorieRecherche(String terme) {
        String termeLower = terme.toLowerCase();
        
        if (termeLower.matches(".*\\b(cuire|bouillir|frire|griller|rôtir)\\b.*")) {
            return "technique";
        } else if (termeLower.matches(".*\\b(pomme|carotte|tomate|ail|oignon)\\b.*")) {
            return "ingredient";
        } else if (termeLower.matches(".*\\b(gâteau|soupe|salade|pâtes|pizza)\\b.*")) {
            return "recette";
        } else {
            return "general";
        }
    }
}