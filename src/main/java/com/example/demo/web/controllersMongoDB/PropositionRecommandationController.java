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
    
    private final PropositionRecommandationService service;

    public PropositionRecommandationController(PropositionRecommandationService service) {
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
    public ResponseEntity<List<PropositionRecommandation>> getAllPropositions() {
        return execute(() -> {
            List<PropositionRecommandation> propositions = service.findAll();
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PropositionRecommandation> getPropositionById(@PathVariable String id) {
        return execute(() -> {
            Optional<PropositionRecommandation> proposition = service.findById(id);
            return proposition.map(p -> new ResponseEntity<>(p, HttpStatus.OK))
                               .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        });
    }
    
    @GetMapping("/user/{idUser}")
    public ResponseEntity<List<PropositionRecommandation>> getPropositionsByUserId(@PathVariable Long idUser) {
        return execute(() -> {
            List<PropositionRecommandation> propositions = service.findByIdUser(idUser);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<PropositionRecommandation>> getPropositionsByStatut(@PathVariable String statut) {
        return execute(() -> {
            List<PropositionRecommandation> propositions = service.findByStatut(statut);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/user/{idUser}/statut/{statut}")
    public ResponseEntity<List<PropositionRecommandation>> getPropositionsByUserAndStatut(
            @PathVariable Long idUser, @PathVariable String statut) {
        return execute(() -> {
            List<PropositionRecommandation> propositions = service.findByIdUserAndStatut(idUser, statut);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/user/{idUser}/pendantes")
    public ResponseEntity<List<PropositionRecommandation>> getPendingPropositions(@PathVariable Long idUser) {
        return execute(() -> {
            List<PropositionRecommandation> propositions = service.findPendingPropositions(idUser);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        });
    }
    
    @GetMapping("/user/{idUser}/notifications-pendantes")
    public ResponseEntity<List<PropositionRecommandation>> getPropositionsWithPendingNotification(@PathVariable Long idUser) {
        return execute(() -> {
            List<PropositionRecommandation> propositions = service.findPropositionsWithPendingNotification(idUser);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        });
    }

    @GetMapping("/periode")
    public ResponseEntity<List<PropositionRecommandation>> getPropositionsByPeriod(
            @RequestParam("debut") String debutStr,
            @RequestParam("fin") String finStr) {
        return execute(() -> {
            LocalDateTime debut = LocalDateTime.parse(debutStr);
            LocalDateTime fin = LocalDateTime.parse(finStr);
            List<PropositionRecommandation> propositions = service.findPropositionsByPeriod(debut, fin);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        });
    }

    @GetMapping("/priorite-haute")
    public ResponseEntity<List<PropositionRecommandation>> getHighPriorityPropositions(
            @RequestParam("min") Integer prioriteMin) {
        return execute(() -> {
            List<PropositionRecommandation> propositions = service.findHighPriorityPropositions(prioriteMin);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        });
    }

    @GetMapping("/statistiques/user/{idUser}/recentes")
    public ResponseEntity<Long> countRecentPropositions(@PathVariable Long idUser, @RequestParam("depuis") String depuisStr) {
        return execute(() -> {
            LocalDateTime depuis = LocalDateTime.parse(depuisStr);
            Long count = service.countRecentPropositionsByUser(idUser, depuis);
            return new ResponseEntity<>(count, HttpStatus.OK);
        });
    }

    @GetMapping("/utilisateur/{idUser}/repondues")
    public ResponseEntity<List<PropositionRecommandation>> getAnsweredPropositions(@PathVariable Long idUser) {
        return execute(() -> {
            List<PropositionRecommandation> propositions = service.findAnsweredPropositions(idUser);
            return new ResponseEntity<>(propositions, HttpStatus.OK);
        });
    }
    
    
    @PutMapping("/{id}/accepter")
    public ResponseEntity<PropositionRecommandation> acceptProposition(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        return execute(() -> {
            String feedback = body != null ? body.get("feedback") : null;
            PropositionRecommandation proposition = service.acceptProposition(id, feedback);
            return new ResponseEntity<>(proposition, HttpStatus.OK);
        });
    }

    
    @PutMapping("/{id}/ignorer")
    public ResponseEntity<PropositionRecommandation> ignoreProposition(@PathVariable String id) {
        return execute(() -> {
            PropositionRecommandation proposition = service.ignoreProposition(id);
            return new ResponseEntity<>(proposition, HttpStatus.OK);
        });
    }
    
    @PutMapping("/{id}/notification-envoyee")
    public ResponseEntity<PropositionRecommandation> markNotificationSent(@PathVariable String id) {
        return execute(() -> {
            PropositionRecommandation proposition = service.markNotificationSent(id);
            return new ResponseEntity<>(proposition, HttpStatus.OK);
        });
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProposition(@PathVariable String id) {
        return execute(() -> {
            service.deleteProposition(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        });
    }
    
    @GetMapping("/statistiques/statut/{statut}")
    public ResponseEntity<Long> countByStatus(@PathVariable String statut) {
        return execute(() -> {
            Long count = service.countByStatus(statut);
            return new ResponseEntity<>(count, HttpStatus.OK);
        });
    }
    
    @GetMapping("/statistiques/user/{idUser}/taux-acceptation")
    public ResponseEntity<Double> getAcceptanceRate(@PathVariable Long idUser) {
        return execute(() -> {
            Double taux = service.calculateAcceptanceRate(idUser);
            return new ResponseEntity<>(taux, HttpStatus.OK);
        });
    }
}
