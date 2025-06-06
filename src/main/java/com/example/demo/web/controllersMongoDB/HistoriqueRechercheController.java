package com.example.demo.web.controllersMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.entiesMongodb.HistoriqueRecherche;
import com.example.demo.servicesMongoDB.HistoriqueRechercheService;

@RestController
@RequestMapping("/api/v1/historique-recherche")
public class HistoriqueRechercheController {
    
    private final HistoriqueRechercheService historiqueService;
    
    public HistoriqueRechercheController(HistoriqueRechercheService historiqueService) {
        this.historiqueService = historiqueService;
    }
    
    @PostMapping
    public ResponseEntity<HistoriqueRecherche> recordSearch(
            @RequestParam Long userId,
            @RequestParam String terme,
            @RequestBody(required = false) List<HistoriqueRecherche.Filtre> filtres) {
        HistoriqueRecherche historique = historiqueService.recordSearch(userId, terme, filtres);
        return new ResponseEntity<>(historique, HttpStatus.CREATED);
    }
    
    @PostMapping("/complete")
    public ResponseEntity<HistoriqueRecherche> recordCompleteSearch(
            @RequestParam Long userId,
            @RequestParam String terme,
            @RequestBody(required = false) List<HistoriqueRecherche.Filtre> filtres,
            @RequestParam(required = false) Integer nombreResultats,
            @RequestParam(required = false) Boolean rechercheFructueuse,
            @RequestParam(defaultValue = "navigation") String contexteRecherche,
            @RequestParam(defaultValue = "web") String sourceRecherche) {
        
        HistoriqueRecherche historique = historiqueService.recordCompleteSearch(
            userId, terme, filtres, nombreResultats, rechercheFructueuse, 
            contexteRecherche, sourceRecherche);
        return new ResponseEntity<>(historique, HttpStatus.CREATED);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<HistoriqueRecherche>> getHistoryByUserId(@PathVariable Long userId) {
        List<HistoriqueRecherche> historique = historiqueService.getHistoryByUserId(userId);
        return ResponseEntity.ok(historique);
    }
    
    @GetMapping("/user/{userId}/statistiques")
    public ResponseEntity<Map<String, Long>> getSearchStatistics(@PathVariable Long userId) {
        Map<String, Long> stats = historiqueService.getSearchStatistics(userId);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/user/{userId}/termes-frequents")
    public ResponseEntity<List<String>> getFrequentTerms(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limite) {
        List<String> termes = historiqueService.getFrequentTerms(userId, limite);
        return ResponseEntity.ok(termes);
    }
    
    @GetMapping("/user/{userId}/suggestions")
    public ResponseEntity<List<String>> getSearchSuggestions(@PathVariable Long userId) {
        List<String> suggestions = historiqueService.getSearchSuggestions(userId);
        return ResponseEntity.ok(suggestions);
    }
    
    @GetMapping("/similaires")
    public ResponseEntity<List<HistoriqueRecherche>> getSimilarSearches(@RequestParam String terme) {
        List<HistoriqueRecherche> historiques = historiqueService.getSimilarSearches(terme);
        return ResponseEntity.ok(historiques);
    }
    
    @GetMapping("/tendances")
    public ResponseEntity<List<String>> getTrendingTerms() {
        List<String> tendances = historiqueService.getTrendingTerms();
        return ResponseEntity.ok(tendances);
    }
    
    @GetMapping("/user/{userId}/periode")
    public ResponseEntity<List<HistoriqueRecherche>> getSearchesByPeriod(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<HistoriqueRecherche> recherches = historiqueService.getSearchesByPeriod(userId, debut, fin);
        return ResponseEntity.ok(recherches);
    }
    
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteUserHistory(@PathVariable Long userId) {
        historiqueService.deleteUserHistory(userId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/maintenance")
    public ResponseEntity<Void> cleanOldSearches(
            @RequestParam(defaultValue = "90") int joursRetention) {
        historiqueService.cleanOldSearches(joursRetention);
        return ResponseEntity.noContent().build();
    }
}