package com.example.demo.web.controllersMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.entiesMongodb.RecetteInteraction;
import com.example.demo.servicesMongoDB.RecetteInteractionService;

@RestController
@RequestMapping("/api/v1/recette-interactions")
public class RecetteInteractionController {
    
    @Autowired
    private RecetteInteractionService service;
    
    @GetMapping
    public ResponseEntity<List<RecetteInteraction>> getAllInteractions() {
        try {
            List<RecetteInteraction> interactions = service.findAll();
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RecetteInteraction> getInteractionById(@PathVariable String id) {
        try {
            Optional<RecetteInteraction> interaction = service.findById(id);
            return interaction.map(i -> new ResponseEntity<>(i, HttpStatus.OK))
                             .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/user/{idUser}")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsByUtilisateur(@PathVariable Long idUser) {
        try {
            List<RecetteInteraction> interactions = service.findByIdUser(idUser);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/recette/{idRecette}")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsByRecette(@PathVariable Long idRecette) {
        try {
            List<RecetteInteraction> interactions = service.findByIdRecette(idRecette);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/type/{typeInteraction}")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsByType(@PathVariable String typeInteraction) {
        try {
            List<RecetteInteraction> interactions = service.findByTypeInteraction(typeInteraction);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsBySession(@PathVariable String sessionId) {
        try {
            List<RecetteInteraction> interactions = service.findParSessionId(sessionId);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/user/{idUser}/recette/{idRecette}")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsByUserAndRecette(
            @PathVariable Long idUser, 
            @PathVariable Long idRecette) {
        try {
            List<RecetteInteraction> interactions = service.findByIdUserAndIdRecette(idUser, idRecette);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/device/{deviceType}")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsByDevice(
            @PathVariable String deviceType,
            @RequestParam(required = false) String depuis) {
        try {
            LocalDateTime depuisDate = depuis != null ? LocalDateTime.parse(depuis) : LocalDateTime.now().minusDays(30);
            List<RecetteInteraction> interactions = service.findParDeviceType(deviceType, depuisDate);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/periode")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsByPeriode(
            @RequestParam String debut,
            @RequestParam String fin) {
        try {
            LocalDateTime dateDebut = LocalDateTime.parse(debut);
            LocalDateTime dateFin = LocalDateTime.parse(fin);
            List<RecetteInteraction> interactions = service.findInteractionsParPeriode(dateDebut, dateFin);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/user/{idUser}/recentes")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsRecentesUtilisateur(
            @PathVariable Long idUser,
            @RequestParam(required = false) String depuis) {
        try {
            LocalDateTime depuisDate = depuis != null ? LocalDateTime.parse(depuis) : LocalDateTime.now().minusDays(7);
            List<RecetteInteraction> interactions = service.findInteractionsRecentesUser(idUser, depuisDate);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/populaires")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsPopulaires(
            @RequestParam String typeInteraction,
            @RequestParam(required = false) String depuis,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            LocalDateTime depuisDate = depuis != null ? LocalDateTime.parse(depuis) : LocalDateTime.now().minusDays(30);
            List<RecetteInteraction> interactions = service.findInteractionsPopulaires(typeInteraction, depuisDate, limit);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/user/{idUser}/non-comptabilisees")
    public ResponseEntity<List<RecetteInteraction>> getInteractionsNonComptabilisees(@PathVariable Long idUser) {
        try {
            List<RecetteInteraction> interactions = service.findInteractionsNonComptabilisees(idUser);
            return new ResponseEntity<>(interactions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping
    public ResponseEntity<RecetteInteraction> createInteraction(@RequestBody RecetteInteraction interaction) {
        try {
            RecetteInteraction savedInteraction = service.save(interaction);
            return new ResponseEntity<>(savedInteraction, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/registrer")
    public ResponseEntity<RecetteInteraction> enregistrerInteraction(
            @RequestParam Long idUser,
            @RequestParam Long idRecette,
            @RequestParam String typeInteraction,
            @RequestParam String sessionId) {
        try {
            RecetteInteraction interaction = service.enregistrerInteraction(idUser, idRecette, typeInteraction, sessionId);
            return new ResponseEntity<>(interaction, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
        try {
            RecetteInteraction interaction = service.enregistrerInteractionComplete(
            		idUser, idRecette, typeInteraction, duree, deviceType, sessionId, sourceInteraction, metadonnees);
            return new ResponseEntity<>(interaction, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PutMapping("/{id}/comptabiliser")
    public ResponseEntity<RecetteInteraction> marquerComptabilisee(@PathVariable String id) {
        try {
            RecetteInteraction interaction = service.marquerComptabilisee(id);
            return new ResponseEntity<>(interaction, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInteraction(@PathVariable String id) {
        try {
            service.supprimerInteraction(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/statistiques/recette/{idRecette}/consultations")
    public ResponseEntity<Long> getConsultationsRecette(@PathVariable Long idRecette) {
        try {
            Long count = service.compterConsultationsRecette(idRecette);
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/statistiques/user/{idUser}/count")
    public ResponseEntity<Long> getInteractionsCountUtilisateur(
            @PathVariable Long idUser,
            @RequestParam(required = false) String depuis) {
        try {
            LocalDateTime depuisDate = depuis != null ? LocalDateTime.parse(depuis) : LocalDateTime.now().minusDays(30);
            Long count = service.compterInteractionsUser(idUser, depuisDate);
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/statistiques/recette/{idRecette}/types")
    public ResponseEntity<Map<String, Long>> getStatistiquesParType(@PathVariable Long idRecette) {
        try {
            Map<String, Long> stats = service.getStatistiquesParTypeInteraction(idRecette);
            return new ResponseEntity<>(stats, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/statistiques/devices")
    public ResponseEntity<Map<String, Long>> getStatistiquesParDevice(
            @RequestParam(required = false) String depuis) {
        try {
            LocalDateTime depuisDate = depuis != null ? LocalDateTime.parse(depuis) : LocalDateTime.now().minusDays(30);
            Map<String, Long> stats = service.getStatistiquesParDevice(depuisDate);
            return new ResponseEntity<>(stats, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/statistiques/recette/{idRecette}/duree-moyenne")
    public ResponseEntity<Double> getDureeMoyenneInteraction(
            @PathVariable Long idRecette,
            @RequestParam String typeInteraction) {
        try {
            Double duree = service.calculerDureeMoyenneInteraction(idRecette, typeInteraction);
            return new ResponseEntity<>(duree, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/statistiques/top-recettes")
    public ResponseEntity<List<RecetteInteraction>> getTopRecettes(
            @RequestParam(required = false) String depuis,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            LocalDateTime depuisDate = depuis != null ? LocalDateTime.parse(depuis) : LocalDateTime.now().minusDays(30);
            List<RecetteInteraction> topRecettes = service.getTopRecettesParInteractions(depuisDate, limit);
            return new ResponseEntity<>(topRecettes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/statistiques/recette/{idRecette}/details")
    public ResponseEntity<List<RecetteInteraction>> getStatistiquesRecette(@PathVariable Long idRecette) {
        try {
            List<RecetteInteraction> stats = service.getStatistiquesInteractionsRecette(idRecette);
            return new ResponseEntity<>(stats, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}