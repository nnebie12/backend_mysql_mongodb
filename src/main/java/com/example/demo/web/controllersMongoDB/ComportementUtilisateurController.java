package com.example.demo.web.controllersMongoDB;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.DTO.ComportementUtilisateurRequestDTO;
import com.example.demo.DTO.ComportementUtilisateurResponseDTO;
import com.example.demo.entiesMongodb.ComportementUtilisateur;
import com.example.demo.entiesMongodb.HistoriqueRecherche; 
import com.example.demo.entiesMongodb.enums.ProfilUtilisateur; 

import com.example.demo.servicesMongoDB.ComportementUtilisateurService;
import com.example.demo.web.mapper.ComportementUtilisateurMapper; 

@RestController
@RequestMapping("/api/v1/comportement-utilisateur")
public class ComportementUtilisateurController {
    
    private final ComportementUtilisateurService comportementService;
    private final ComportementUtilisateurMapper mapper; 

    public ComportementUtilisateurController(ComportementUtilisateurService comportementService, ComportementUtilisateurMapper mapper) {
        this.comportementService = comportementService;
        this.mapper = mapper;
    }
    
    @PostMapping
    public ResponseEntity<ComportementUtilisateurResponseDTO> createBehavior(@RequestParam Long userId) {
        ComportementUtilisateur comportement = comportementService.createBehavior(userId);
        return new ResponseEntity<>(mapper.toResponseDto(comportement), HttpStatus.CREATED);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ComportementUtilisateurResponseDTO> getBehaviorByUserId(@PathVariable Long userId) {
        Optional<ComportementUtilisateur> comportement = comportementService.getBehaviorByUserId(userId);
        return comportement.map(c -> new ResponseEntity<>(mapper.toResponseDto(c), HttpStatus.OK))
                           .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/user/{userId}/or-create")
    public ResponseEntity<ComportementUtilisateurResponseDTO> getOrCreateBehavior(@PathVariable Long userId) {
        ComportementUtilisateur comportement = comportementService.getOrCreateBehavior(userId);
        return new ResponseEntity<>(mapper.toResponseDto(comportement), HttpStatus.OK);
    }
    
    @PutMapping
    public ResponseEntity<ComportementUtilisateurResponseDTO> updateBehavior(
            @RequestBody ComportementUtilisateurRequestDTO comportementDTO) { 

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST); 
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<ComportementUtilisateurResponseDTO> updateBehaviorPartial(
            @PathVariable Long userId,
            @RequestBody ComportementUtilisateurRequestDTO comportementDTO) {
        Optional<ComportementUtilisateur> existingComportementOpt = comportementService.getBehaviorByUserId(userId);
        if (existingComportementOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        ComportementUtilisateur existingComportement = existingComportementOpt.get();
        
        mapper.updateEntityFromRequestDto(comportementDTO, existingComportement);
        
        ComportementUtilisateur comportementMisAJour = comportementService.updateBehavior(existingComportement);
        
        return new ResponseEntity<>(mapper.toResponseDto(comportementMisAJour), HttpStatus.OK);
    }
    
    @PostMapping("/user/{userId}/refresh-metrics")
    public ResponseEntity<Void> updateMetrics(@PathVariable Long userId) {
        comportementService.updateMetrics(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    @PostMapping("/user/{userId}/record-search")
    public ResponseEntity<ComportementUtilisateurResponseDTO> recordSearch( 
            @PathVariable Long userId,
            @RequestParam String terme,
            @RequestParam(required = false) Integer nombreResultats,
            @RequestParam(required = false) Boolean rechercheFructueuse,
            @RequestBody(required = false) List<HistoriqueRecherche.Filtre> filtres) {
        
        ComportementUtilisateur comportement = comportementService.recordSearch(
            userId, terme, filtres, nombreResultats, rechercheFructueuse);
        return new ResponseEntity<>(mapper.toResponseDto(comportement), HttpStatus.OK);
    }
    
    @GetMapping("/user/{userId}/frequent-terms")
    public ResponseEntity<List<String>> getFrequentSearchTerms(@PathVariable Long userId) {
        List<String> termes = comportementService.getFrequentSearchTerms(userId);
        return new ResponseEntity<>(termes, HttpStatus.OK);
    }
    
    @GetMapping("/profil/{profil}")
    public ResponseEntity<List<ComportementUtilisateurResponseDTO>> getUsersByProfile(@PathVariable ProfilUtilisateur profil) { 
        List<ComportementUtilisateur> utilisateurs = comportementService.getUsersByProfile(profil); 
        return new ResponseEntity<>(mapper.toResponseDtoList(utilisateurs), HttpStatus.OK);
    }
    
    @GetMapping("/engaged")
    public ResponseEntity<List<ComportementUtilisateurResponseDTO>> getEngagedUsers( 
            @RequestParam(defaultValue = "50.0") Double scoreMinimum) {
        List<ComportementUtilisateur> utilisateurs = comportementService.getEngagedUsers(scoreMinimum);
        return new ResponseEntity<>(mapper.toResponseDtoList(utilisateurs), HttpStatus.OK);
    }
    
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteUserBehavior(@PathVariable Long userId) {
        comportementService.deleteUserBehavior(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}