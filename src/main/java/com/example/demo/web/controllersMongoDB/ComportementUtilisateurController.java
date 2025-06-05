package com.example.demo.web.controllersMongoDB;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.entiesMongodb.ComportementUtilisateur;
import com.example.demo.entiesMongodb.HistoriqueRecherche;
import com.example.demo.servicesMongoDB.ComportementUtilisateurService;

@RestController
@RequestMapping("/api/v1/comportement-utilisateur")
public class ComportementUtilisateurController {
    
    private final ComportementUtilisateurService comportementService;
    
    public ComportementUtilisateurController(ComportementUtilisateurService comportementService) {
        this.comportementService = comportementService;
    }
    
    @PostMapping
    public ResponseEntity<ComportementUtilisateur> createBehavior(@RequestParam Long userId) {
    	ComportementUtilisateur comportement = comportementService.createBehavior(userId);
        return new ResponseEntity<>(comportement, HttpStatus.CREATED);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ComportementUtilisateur> getBehaviorByUserId(@PathVariable Long userId) {
        Optional<ComportementUtilisateur> comportement = comportementService.getBehaviorByUserId(userId);
        return comportement.map(c -> new ResponseEntity<>(c, HttpStatus.OK))
                          .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/user/{userId}/or-create")
    public ResponseEntity<ComportementUtilisateur> getOrCreateBehavior(@PathVariable Long userId) {
        ComportementUtilisateur comportement = comportementService.getOrCreateBehavior(userId);
        return new ResponseEntity<>(comportement, HttpStatus.OK);
    }
    
    @PutMapping
    public ResponseEntity<ComportementUtilisateur> updateBehavior(
            @RequestBody ComportementUtilisateur comportement) {
        ComportementUtilisateur comportementMisAJour = comportementService.updateBehavior(comportement);
        return new ResponseEntity<>(comportementMisAJour, HttpStatus.OK);
    }
    
    @PostMapping("/user/{userId}/refresh-metrics")
    public ResponseEntity<Void> updateMetrics(@PathVariable Long userId) {
        comportementService.updateMetrics(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    @PostMapping("/user/{userId}/record-search")
    public ResponseEntity<ComportementUtilisateur> recordSearch(
            @PathVariable Long userId,
            @RequestParam String terme,
            @RequestParam(required = false) Integer nombreResultats,
            @RequestParam(required = false) Boolean rechercheFructueuse,
            @RequestBody(required = false) List<HistoriqueRecherche.Filtre> filtres) {
        
        ComportementUtilisateur comportement = comportementService.recordSearch(
            userId, terme, filtres, nombreResultats, rechercheFructueuse);
        return new ResponseEntity<>(comportement, HttpStatus.OK);
    }
    
    @GetMapping("/user/{userId}/frequent-terms")
    public ResponseEntity<List<String>> getFrequentSearchTerms(@PathVariable Long userId) {
        List<String> termes = comportementService.getFrequentSearchTerms(userId);
        return new ResponseEntity<>(termes, HttpStatus.OK);
    }
    
    @GetMapping("/profil/{profil}")
    public ResponseEntity<List<ComportementUtilisateur>> getUsersByProfile(@PathVariable String profil) {
        List<ComportementUtilisateur> utilisateurs = comportementService.getUsersByProfile(profil);
        return new ResponseEntity<>(utilisateurs, HttpStatus.OK);
    }
    
    @GetMapping("/engaged")
    public ResponseEntity<List<ComportementUtilisateur>> getEngagedUsers(
            @RequestParam(defaultValue = "50.0") Double scoreMinimum) {
        List<ComportementUtilisateur> utilisateurs = comportementService.getEngagedUsers(scoreMinimum);
        return new ResponseEntity<>(utilisateurs, HttpStatus.OK);
    }
    
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteUserBehavior(@PathVariable Long userId) {
        comportementService.deleteUserBehavior(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}