package com.example.demo.web.controllersMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entiesMongodb.RecetteInteraction;
import com.example.demo.servicesMongoDB.RecetteInteractionService;

@RestController
@RequestMapping("/api/v1/recette-interactions")
public class RecetteInteractionController {
    
    private final RecetteInteractionService service;

    public RecetteInteractionController(RecetteInteractionService service) {
        this.service = service;
    }

    private <T> ResponseEntity<T> execute(Supplier<ResponseEntity<T>> action) {
        try {
            return action.get();
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<RecetteInteraction>> getAllInteractions() {
        return execute(() -> {
            List<RecetteInteraction> interactions = service.findAll();
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RecetteInteraction> getInteractionById(@PathVariable String id) {
        return execute(() -> {
            Optional<RecetteInteraction> interaction = service.findById(id);
            return interaction.map(i -> new ResponseEntity<>(i, HttpStatus.OK))
                             .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        });
    }
    
    @GetMapping("/user/{idUser}")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsByUtilisateur(@PathVariable Long idUser) {
        return execute(() -> {
            List<RecetteInteraction> interactions = service.findByIdUser(idUser);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/recette/{idRecette}")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsByRecette(@PathVariable Long idRecette) {
        return execute(() -> {
            List<RecetteInteraction> interactions = service.findByIdRecette(idRecette);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/type/{typeInteraction}")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsByType(@PathVariable String typeInteraction) {
        return execute(() -> {
            List<RecetteInteraction> interactions = service.findByTypeInteraction(typeInteraction);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsBySession(@PathVariable String sessionId) {
        return execute(() -> {
            List<RecetteInteraction> interactions = service.findParSessionId(sessionId);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/user/{idUser}/recette/{idRecette}")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsByUserAndRecette(
            @PathVariable Long idUser, 
            @PathVariable Long idRecette) {
        return execute(() -> {
            List<RecetteInteraction> interactions = service.findByIdUserAndIdRecette(idUser, idRecette);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/device/{deviceType}")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsByDevice(
            @PathVariable String deviceType,
            @RequestParam(required = false) String depuis) {
        return execute(() -> {
            LocalDateTime depuisDate = depuis != null ? LocalDateTime.parse(depuis) : LocalDateTime.now().minusDays(30);
            List<RecetteInteraction> interactions = service.findParDeviceType(deviceType, depuisDate);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/periode")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsByPeriode(
            @RequestParam String debut,
            @RequestParam String fin) {
        return execute(() -> {
            LocalDateTime dateDebut = LocalDateTime.parse(debut);
            LocalDateTime dateFin = LocalDateTime.parse(fin);
            List<RecetteInteraction> interactions = service.findInteractionsParPeriode(dateDebut, dateFin);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/user/{idUser}/recentes")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsRecentesUtilisateur(
            @PathVariable Long idUser,
            @RequestParam(required = false) String depuis) {
        return execute(() -> {
            LocalDateTime depuisDate = depuis != null ? LocalDateTime.parse(depuis) : LocalDateTime.now().minusDays(7);
            List<RecetteInteraction> interactions = service.findInteractionsRecentesUser(idUser, depuisDate);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/populaires")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsPopulaires(
            @RequestParam String typeInteraction,
            @RequestParam(required = false) String depuis,
            @RequestParam(defaultValue = "10") int limit) {
        return execute(() -> {
            LocalDateTime depuisDate = depuis != null ? LocalDateTime.parse(depuis) : LocalDateTime.now().minusDays(30);
            List<RecetteInteraction> interactions = service.findInteractionsPopulaires(typeInteraction, depuisDate, limit);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/user/{idUser}/non-comptabilisees")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsNonComptabilisees(@PathVariable Long idUser) {
        return execute(() -> {
            List<RecetteInteraction> interactions = service.findInteractionsNonComptabilisees(idUser);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        });
    }
    
    @PostMapping
    public ResponseEntity<RecetteInteraction> createInteraction(@RequestBody RecetteInteraction interaction) {
        return execute(() -> {
            RecetteInteraction savedInteraction = service.save(interaction);
            return new ResponseEntity<>(savedInteraction, HttpStatus.CREATED);
        });
    }
    
    @PostMapping("/registrer")
    public ResponseEntity<RecetteInteraction> enregistrerInteraction(
            @RequestParam Long idUser,
            @RequestParam Long idRecette,
            @RequestParam String typeInteraction,
            @RequestParam String sessionId) {
        return execute(() -> {
            RecetteInteraction interaction = service.enregistrerInteraction(idUser, idRecette, typeInteraction, sessionId);
            return new ResponseEntity<>(interaction, HttpStatus.CREATED);
        });
    }
    
    @PostMapping("/registrer-complete")
    public ResponseEntity<RecetteInteraction> enregistrerInteractionComplete(
            @RequestParam Long idUser,
            @RequestParam Long idRecette,
            @RequestParam String typeInteraction,
            @RequestParam(required = false) Integer duree,
            @RequestParam(required = false) String deviceType,
            @RequestParam String sessionId,
            @RequestParam(required = false) String sourceInteraction,
            @RequestBody(required = false) Map<String, Object> metadonnees) {
        return execute(() -> {
            RecetteInteraction interaction = service.enregistrerInteractionComplete(
            		idUser, idRecette, typeInteraction, duree, deviceType, sessionId, sourceInteraction, metadonnees);
            return new ResponseEntity<>(interaction, HttpStatus.CREATED);
        });
    }
    
    @PutMapping("/{id}/comptabiliser")
    public ResponseEntity<RecetteInteraction> marquerComptabilisee(@PathVariable String id) {
        return execute(() -> {
            RecetteInteraction interaction = service.marquerComptabilisee(id);
            return new ResponseEntity<>(interaction, HttpStatus.OK);
        });
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInteraction(@PathVariable String id) {
        return execute(() -> {
            service.supprimerInteraction(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        });
    }
    
    @GetMapping("/statistiques/recette/{idRecette}/consultations")
    public ResponseEntity<Long> getConsultationsRecette(@PathVariable Long idRecette) {
        return execute(() -> {
            Long count = service.compterConsultationsRecette(idRecette);
            return new ResponseEntity<>(count, HttpStatus.OK);
        });
    }
    
    @GetMapping("/statistiques/user/{idUser}/count")
    public ResponseEntity<Long> getInteractionsCountUtilisateur(
            @PathVariable Long idUser,
            @RequestParam(required = false) String depuis) {
        return execute(() -> {
            LocalDateTime depuisDate = depuis != null ? LocalDateTime.parse(depuis) : LocalDateTime.now().minusDays(30);
            Long count = service.compterInteractionsUser(idUser, depuisDate);
            return new ResponseEntity<>(count, HttpStatus.OK);
        });
    }
    
    @GetMapping("/statistiques/recette/{idRecette}/types")
    public ResponseEntity<Map<String, Long>> getStatistiquesParType(@PathVariable Long idRecette) {
        return execute(() -> {
            Map<String, Long> stats = service.getStatistiquesParTypeInteraction(idRecette);
            return new ResponseEntity<>(stats, HttpStatus.OK);
        });
    }
    
    @GetMapping("/statistiques/devices")
    public ResponseEntity<Map<String, Long>> getStatistiquesParDevice(
            @RequestParam(required = false) String depuis) {
        return execute(() -> {
            LocalDateTime depuisDate = depuis != null ? LocalDateTime.parse(depuis) : LocalDateTime.now().minusDays(30);
            Map<String, Long> stats = service.getStatistiquesParDevice(depuisDate);
            return new ResponseEntity<>(stats, HttpStatus.OK);
        });
    }
    
    @GetMapping("/statistiques/recette/{idRecette}/duree-moyenne")
    public ResponseEntity<Double> getDureeMoyenneInteraction(
            @PathVariable Long idRecette,
            @RequestParam String typeInteraction) {
        return execute(() -> {
            Double duree = service.calculerDureeMoyenneInteraction(idRecette, typeInteraction);
            return new ResponseEntity<>(duree, HttpStatus.OK);
        });
    }
    
    @GetMapping("/statistiques/top-recettes")
    public ResponseEntity<List<RecetteInteraction>> getTopRecettes(
            @RequestParam(required = false) String depuis,
            @RequestParam(defaultValue = "10") int limit) {
        return execute(() -> {
            LocalDateTime depuisDate = depuis != null ? LocalDateTime.parse(depuis) : LocalDateTime.now().minusDays(30);
            List<RecetteInteraction> topRecettes = service.getTopRecettesParInteractions(depuisDate, limit);
            return new ResponseEntity<>(topRecettes, HttpStatus.OK);
        });
    }
    
    @GetMapping("/statistiques/recette/{idRecette}/details")
    public ResponseEntity<List<RecetteInteraction>> getStatistiquesRecette(@PathVariable Long idRecette) {
        return execute(() -> {
            List<RecetteInteraction> stats = service.getStatistiquesInteractionsRecette(idRecette);
            return new ResponseEntity<>(stats, HttpStatus.OK);
        });
    }
}