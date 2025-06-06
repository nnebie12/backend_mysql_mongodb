package com.example.demo.servicesImplMongoDB;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.entiesMongodb.PropositionRecommandation;
import com.example.demo.repositoryMongoDB.PropositionRecommandationRepository;
import com.example.demo.servicesMongoDB.PropositionRecommandationService;

@Service
public class PropositionRecommandationServiceImpl implements PropositionRecommandationService {
    
    @Autowired
    private PropositionRecommandationRepository repository;
    
    @Override
    public PropositionRecommandation save(PropositionRecommandation proposition) {
        return repository.save(proposition);
    }
    
    @Override
    public Optional<PropositionRecommandation> findById(String id) {
        return repository.findById(id);
    }
    
    @Override
    public List<PropositionRecommandation> findAll() {
        return repository.findAll();
    }
    
    @Override
    public List<PropositionRecommandation> findByIdUser(Long idUser) {
        return repository.findByIdUser(idUser);
    }
    
    @Override
    public List<PropositionRecommandation> findByStatut(String statut) {
        return repository.findByStatut(statut);
    }
    
    @Override
    public List<PropositionRecommandation> findByIdUserAndStatut(Long idUser, String statut) {
        return repository.findByIdUserAndStatut(idUser, statut);
    }
    
    @Override
    public Optional<PropositionRecommandation> findByIdUserAndIdRecommandation(Long idUser, String idRecommandation) {
        return repository.findByIdUserAndIdRecommandation(idUser, idRecommandation);
    }
    
    @Override
    public PropositionRecommandation createProposition(Long idUser, String idRecommandation, Integer priorite) {
        PropositionRecommandation proposition = new PropositionRecommandation();
        proposition.setIdUser(idUser);
        proposition.setIdRecommandation(idRecommandation);
        proposition.setStatut("PROPOSEE");
        proposition.setDateProposition(LocalDateTime.now());
        proposition.setPriorite(priorite != null ? priorite : 3);
        proposition.setNotificationEnvoyee(false);
        proposition.setScoreInteret(0.5);

        proposition.setFeedbackUser("");
        proposition.setRaisonRefus("");
        proposition.setDateReponse(null); 

        return repository.save(proposition);
    }

    
    @Override
    public PropositionRecommandation acceptProposition(String id, String feedback) {
        Optional<PropositionRecommandation> optProposition = repository.findById(id);
        if (optProposition.isPresent()) {
            PropositionRecommandation proposition = optProposition.get();
            proposition.setStatut("ACCEPTEE");
            proposition.setDateReponse(LocalDateTime.now());
            proposition.setFeedbackUser(feedback);
            return repository.save(proposition);
        }
        throw new RuntimeException("Proposition non trouvée avec l'ID: " + id);
    }
    
    @Override
    public PropositionRecommandation rejectProposition(String id, String raisonRefus, String feedback) {
        Optional<PropositionRecommandation> optProposition = repository.findById(id);
        if (optProposition.isPresent()) {
            PropositionRecommandation proposition = optProposition.get();
            proposition.setStatut("REFUSEE");
            proposition.setDateReponse(LocalDateTime.now());
            proposition.setRaisonRefus(raisonRefus);
            proposition.setFeedbackUser(feedback);
            return repository.save(proposition);
        }
        throw new RuntimeException("Proposition non trouvée avec l'ID: " + id);
    }
    
    @Override
    public PropositionRecommandation ignoreProposition(String id) {
        Optional<PropositionRecommandation> optProposition = repository.findById(id);
        if (optProposition.isPresent()) {
            PropositionRecommandation proposition = optProposition.get();
            proposition.setStatut("IGNOREE");
            proposition.setDateReponse(LocalDateTime.now());
            return repository.save(proposition);
        }
        throw new RuntimeException("Proposition non trouvée avec l'ID: " + id);
    }
    
    @Override
    public List<PropositionRecommandation> findPendingPropositions(Long idUser) {
        return repository.findByIdUserAndStatut(idUser, "PROPOSEE");
    }
    
    @Override
    public List<PropositionRecommandation> findPropositionsWithPendingNotification(Long idUser) {
        return repository.findByIdUserAndNotificationEnvoyeeFalse(idUser);
    }
    
    @Override
    public PropositionRecommandation markNotificationSent(String id) {
        Optional<PropositionRecommandation> optProposition = repository.findById(id);
        if (optProposition.isPresent()) {
            PropositionRecommandation proposition = optProposition.get();
            proposition.setNotificationEnvoyee(true);
            return repository.save(proposition);
        }
        throw new RuntimeException("Proposition non trouvée avec l'ID: " + id);
    }
    
    @Override
    public List<PropositionRecommandation> findPropositionsByPeriod(LocalDateTime debut, LocalDateTime fin) {
        return repository.findByDatePropositionBetween(debut, fin);
    }
    
    @Override
    public List<PropositionRecommandation> findHighPriorityPropositions(Integer prioriteMin) {
        return repository.findByStatutAndPrioriteGreaterThanEqual("PROPOSEE", prioriteMin);
    }
    
    @Override
    public void deleteProposition(String id) {
        repository.deleteById(id);
    }
    
    @Override
    public Long countByStatus(String statut) {
        return repository.countByStatut(statut);
    }
    
    @Override
    public Long countRecentPropositionsByUser(Long idUser, LocalDateTime depuis) {
        return repository.countByIdUserAndDatePropositionAfter(idUser, depuis);
    }
    
    @Override
    public Double calculateAcceptanceRate(Long idUser) {
        List<PropositionRecommandation> repondues = repository.findByIdUserAndStatutIn(idUser, Arrays.asList("ACCEPTEE", "REFUSEE"));
        if (repondues.isEmpty()) return 0.0;
        
        long acceptees = repository.countByIdUserAndStatut(idUser, "ACCEPTEE");
        
        return (double) acceptees / repondues.size();
    }
    
    @Override
    public List<PropositionRecommandation> findAnsweredPropositions(Long idUser) {
        return repository.findByIdUserAndStatutIn(idUser, Arrays.asList("ACCEPTEE", "REFUSEE"));
    }
}