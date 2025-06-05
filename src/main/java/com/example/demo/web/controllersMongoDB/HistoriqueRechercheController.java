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
    public ResponseEntity<HistoriqueRecherche> enregistrerRecherche(
            @RequestParam Long userId,
            @RequestParam String terme,
            @RequestBody(required = false) List<HistoriqueRecherche.Filtre> filtres) {
        HistoriqueRecherche historique = historiqueService.enregistrerRecherche(userId, terme, filtres);
        return new ResponseEntity<>(historique, HttpStatus.CREATED);
    }
    
    @PostMapping("/complete")
    public ResponseEntity<HistoriqueRecherche> enregistrerRechercheComplete(
            @RequestParam Long userId,
            @RequestParam String terme,
            @RequestBody(required = false) List<HistoriqueRecherche.Filtre> filtres,
            @RequestParam(required = false) Integer nombreResultats,
            @RequestParam(required = false) Boolean rechercheFructueuse,
            @RequestParam(defaultValue = "navigation") String contexteRecherche,
            @RequestParam(defaultValue = "web") String sourceRecherche) {
        
        HistoriqueRecherche historique = historiqueService.enregistrerRechercheComplete(
            userId, terme, filtres, nombreResultats, rechercheFructueuse, 
            contexteRecherche, sourceRecherche);
        return new ResponseEntity<>(historique, HttpStatus.CREATED);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<HistoriqueRecherche>> getHistoriqueByUserId(@PathVariable Long userId) {
        List<HistoriqueRecherche> historique = historiqueService.getHistoriqueByUserId(userId);
        return ResponseEntity.ok(historique);
    }
    
    @GetMapping("/user/{userId}/statistiques")
    public ResponseEntity<Map<String, Long>> getStatistiquesRecherche(@PathVariable Long userId) {
        Map<String, Long> stats = historiqueService.getStatistiquesRecherche(userId);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/user/{userId}/termes-frequents")
    public ResponseEntity<List<String>> getTermesFrequents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limite) {
        List<String> termes = historiqueService.getTermesFrequents(userId, limite);
        return ResponseEntity.ok(termes);
    }
    
    @GetMapping("/user/{userId}/suggestions")
    public ResponseEntity<List<String>> getSuggestionsRecherche(@PathVariable Long userId) {
        List<String> suggestions = historiqueService.getSuggestionsRecherche(userId);
        return ResponseEntity.ok(suggestions);
    }
    
    @GetMapping("/similaires")
    public ResponseEntity<List<HistoriqueRecherche>> getRecherchesSimilaires(@RequestParam String terme) {
        List<HistoriqueRecherche> historiques = historiqueService.getRecherchesSimilaires(terme);
        return ResponseEntity.ok(historiques);
    }
    
    @GetMapping("/tendances")
    public ResponseEntity<List<String>> getTermesTendance() {
        List<String> tendances = historiqueService.getTermesTendance();
        return ResponseEntity.ok(tendances);
    }
    
    @GetMapping("/user/{userId}/periode")
    public ResponseEntity<List<HistoriqueRecherche>> getRecherchesPeriode(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<HistoriqueRecherche> recherches = historiqueService.getRecherchesPeriode(userId, debut, fin);
        return ResponseEntity.ok(recherches);
    }
    
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> supprimerHistoriqueUtilisateur(@PathVariable Long userId) {
        historiqueService.supprimerHistoriqueUtilisateur(userId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/maintenance")
    public ResponseEntity<Void> nettoyerAnciennesRecherches(
            @RequestParam(defaultValue = "90") int joursRetention) {
        historiqueService.nettoyerAnciennesRecherches(joursRetention);
        return ResponseEntity.noContent().build();
    }
}