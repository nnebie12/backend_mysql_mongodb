package com.example.demo.web.controllersMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entiesMongodb.PropositionRecommandation;
import com.example.demo.servicesMongoDB.PropositionRecommandationService;

@RestController
@RequestMapping("/api/v1/propositions")
public class PropositionRecommandationController {
    
    @Autowired
    private PropositionRecommandationService service;
    
    @GetMapping
    public ResponseEntity<List<PropositionRecommandation>> getAllPropositions() {
        try {
            List<PropositionRecommandation> propositions = service.findAll();
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PropositionRecommandation> getPropositionById(@PathVariable String id) {
        try {
            Optional<PropositionRecommandation> proposition = service.findById(id);
            return proposition.map(p -> new ResponseEntity<>(p, HttpStatus.OK))
                               .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/user/{idUser}")
    public ResponseEntity<List<PropositionRecommandation>> getPropositionsByUserId(@PathVariable Long idUser) {
        try {
            List<PropositionRecommandation> propositions = service.findByIdUser(idUser);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<PropositionRecommandation>> getPropositionsByStatut(@PathVariable String statut) {
        try {
            List<PropositionRecommandation> propositions = service.findByStatut(statut);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/user/{idUser}/statut/{statut}")
    public ResponseEntity<List<PropositionRecommandation>> getPropositionsByUserAndStatut(
            @PathVariable Long idUser, @PathVariable String statut) {
        try {
            List<PropositionRecommandation> propositions = service.findByIdUserAndStatut(idUser, statut);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/user/{idUser}/pendantes")
    public ResponseEntity<List<PropositionRecommandation>> getPendingPropositions(@PathVariable Long idUser) {
        try {
            List<PropositionRecommandation> propositions = service.findPendingPropositions(idUser);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/user/{idUser}/notifications-pendantes")
    public ResponseEntity<List<PropositionRecommandation>> getPropositionsWithPendingNotification(@PathVariable Long idUser) {
        try {
            List<PropositionRecommandation> propositions = service.findPropositionsWithPendingNotification(idUser);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/periode")
    public ResponseEntity<List<PropositionRecommandation>> getPropositionsByPeriod(
            @RequestParam("debut") String debutStr,
            @RequestParam("fin") String finStr) {
        try {
            LocalDateTime debut = LocalDateTime.parse(debutStr);
            LocalDateTime fin = LocalDateTime.parse(finStr);
            List<PropositionRecommandation> propositions = service.findPropositionsByPeriod(debut, fin);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/priorite-haute")
    public ResponseEntity<List<PropositionRecommandation>> getHighPriorityPropositions(
            @RequestParam("min") Integer prioriteMin) {
        try {
            List<PropositionRecommandation> propositions = service.findHighPriorityPropositions(prioriteMin);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/statistiques/user/{idUser}/recentes")
    public ResponseEntity<Long> countRecentPropositions(@PathVariable Long idUser, @RequestParam("depuis") String depuisStr) {
        try {
            LocalDateTime depuis = LocalDateTime.parse(depuisStr);
            Long count = service.countRecentPropositionsByUser(idUser, depuis);
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/utilisateur/{idUser}/repondues")
    public ResponseEntity<List<PropositionRecommandation>> getAnsweredPropositions(@PathVariable Long idUser) {
        try {
            List<PropositionRecommandation> propositions = service.findAnsweredPropositions(idUser);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    @PutMapping("/{id}/accepter")
    public ResponseEntity<PropositionRecommandation> acceptProposition(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String feedback = body != null ? body.get("feedback") : null;
            PropositionRecommandation proposition = service.acceptProposition(id, feedback);
            return new ResponseEntity<>(proposition, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    
    @PutMapping("/{id}/ignorer")
    public ResponseEntity<PropositionRecommandation> ignoreProposition(@PathVariable String id) {
        try {
            PropositionRecommandation proposition = service.ignoreProposition(id);
            return new ResponseEntity<>(proposition, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PutMapping("/{id}/notification-envoyee")
    public ResponseEntity<PropositionRecommandation> markNotificationSent(@PathVariable String id) {
        try {
            PropositionRecommandation proposition = service.markNotificationSent(id);
            return new ResponseEntity<>(proposition, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProposition(@PathVariable String id) {
        try {
            service.deleteProposition(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/statistiques/statut/{statut}")
    public ResponseEntity<Long> countByStatus(@PathVariable String statut) {
        try {
            Long count = service.countByStatus(statut);
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/statistiques/user/{idUser}/taux-acceptation")
    public ResponseEntity<Double> getAcceptanceRate(@PathVariable Long idUser) {
        try {
            Double taux = service.calculateAcceptanceRate(idUser);
            return new ResponseEntity<>(taux, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
