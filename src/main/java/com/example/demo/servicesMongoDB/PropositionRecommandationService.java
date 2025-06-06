package com.example.demo.servicesMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.example.demo.entiesMongodb.PropositionRecommandation;

public interface PropositionRecommandationService {
    
    PropositionRecommandation save(PropositionRecommandation proposition);
    
    Optional<PropositionRecommandation> findById(String id);
    
    List<PropositionRecommandation> findAll();
    
    List<PropositionRecommandation> findByIdUser(Long idUser);
    
    List<PropositionRecommandation> findByStatut(String statut);
    
    List<PropositionRecommandation> findByIdUserAndStatut(Long idUser, String statut);
    
    Optional<PropositionRecommandation> findByIdUserAndIdRecommandation(Long idUser, String idRecommandation);
    
    PropositionRecommandation createProposition(Long idUser, String idRecommandation, Integer priorite);
    
    PropositionRecommandation acceptProposition(String id, String feedback);
    
    PropositionRecommandation rejectProposition(String id, String raisonRefus, String feedback);
    
    PropositionRecommandation ignoreProposition(String id);
    
    List<PropositionRecommandation> findPendingPropositions(Long idUser);
    
    List<PropositionRecommandation> findPropositionsWithPendingNotification(Long idUser);
    
    PropositionRecommandation markNotificationSent(String id);
    
    List<PropositionRecommandation> findPropositionsByPeriod(LocalDateTime debut, LocalDateTime fin);
    
    List<PropositionRecommandation> findHighPriorityPropositions(Integer prioriteMin);
    
    void deleteProposition(String id);
    
    Long countByStatus(String statut);
    
    Long countRecentPropositionsByUser(Long idUser, LocalDateTime depuis);
    
    Double calculateAcceptanceRate(Long idUser);
    
    List<PropositionRecommandation> findAnsweredPropositions(Long idUser);
}
