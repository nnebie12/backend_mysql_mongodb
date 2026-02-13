package com.example.demo.web.controllersMongoDB;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entiesMongodb.InteractionUtilisateur;
import com.example.demo.servicesMongoDB.InteractionUtilisateurService;

@RestController
@RequestMapping("/api/v1/interactions")
public class InteractionUtilisateurController {

    private final InteractionUtilisateurService interactionService;

    public InteractionUtilisateurController(InteractionUtilisateurService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping
    public ResponseEntity<InteractionUtilisateur> enregistrerInteraction(
            @RequestParam Long userId,
            @RequestParam String typeInteraction, 
            @RequestParam Long entiteId,
            @RequestParam(required = false) Integer dureeConsultation) {
        
        InteractionUtilisateur interaction = interactionService.addInteractionUtilisateur(
                userId, typeInteraction, entiteId, dureeConsultation);
        return new ResponseEntity<>(interaction, HttpStatus.CREATED);
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<InteractionUtilisateur>> getAllInteractions() {
        List<InteractionUtilisateur> interactions = interactionService.getAllInteractions(); 
        return new ResponseEntity<>(interactions, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InteractionUtilisateur>> getInteractionsByUserId(@PathVariable Long userId) {
        List<InteractionUtilisateur> interactions = interactionService.getInteractionsByUserId(userId);
        return new ResponseEntity<>(interactions, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}/type/{typeInteraction}")
    public ResponseEntity<List<InteractionUtilisateur>> getInteractionsByUserIdAndType(
            @PathVariable Long userId, @PathVariable String typeInteraction) {
        List<InteractionUtilisateur> interactions = interactionService.getInteractionsByUserIdAndType(userId, typeInteraction);
        return new ResponseEntity<>(interactions, HttpStatus.OK);
    }

    @GetMapping("/entite/{entiteId}/type/{typeInteraction}")
    public ResponseEntity<List<InteractionUtilisateur>> getInteractionsByEntiteIdAndType(
            @PathVariable Long entiteId, @PathVariable String typeInteraction) {
        List<InteractionUtilisateur> interactions = interactionService.getInteractionsByEntiteIdAndType(entiteId, typeInteraction);
        return new ResponseEntity<>(interactions, HttpStatus.OK);
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> supprimerInteractionsUtilisateur(@PathVariable Long userId) {
        interactionService.deleteInteractionsUtilisateur(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}