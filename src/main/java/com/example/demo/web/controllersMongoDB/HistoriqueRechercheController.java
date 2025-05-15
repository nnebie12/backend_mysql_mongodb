package com.example.demo.web.controllersMongoDB;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entiesMongodb.HistoriqueRecherche;
import com.example.demo.servicesMongoDB.HistoriqueRechercheService;


@RestController
@RequestMapping("/ap/iv1/HistoriqueRecherche")
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
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<HistoriqueRecherche>> getHistoriqueByUserId(@PathVariable Long userId) {
        List<HistoriqueRecherche> historique = historiqueService.getHistoriqueByUserId(userId);
        return new ResponseEntity<>(historique, HttpStatus.OK);
    }
    
    @GetMapping("/similaires")
    public ResponseEntity<List<HistoriqueRecherche>> getRecherchesSimilaires(@RequestParam String terme) {
        List<HistoriqueRecherche> historiques = historiqueService.getRecherchesSimilaires(terme);
        return new ResponseEntity<>(historiques, HttpStatus.OK);
    }
    
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> supprimerHistoriqueUtilisateur(@PathVariable Long userId) {
        historiqueService.supprimerHistoriqueUtilisateur(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
