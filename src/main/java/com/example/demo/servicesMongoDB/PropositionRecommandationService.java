package com.example.demo.servicesMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.example.demo.entiesMongodb.PropositionRecommandation;

public interface PropositionRecommandationService {
    
    PropositionRecommandation save(PropositionRecommandation proposition);
    
    Optional<PropositionRecommandation> findById(String id);
    
    List<PropositionRecommandation> findAll();
    
    void deleteProposition(String id);
    
    List<PropositionRecommandation> findByIdUser(Long idUser);
    
    List<PropositionRecommandation> findByStatut(String statut);
    
    List<PropositionRecommandation> findByIdUserAndStatut(Long idUser, String statut);
    
    Optional<PropositionRecommandation> findByIdUserAndIdRecommandation(Long idUser, String idRecommandation);
    
    List<PropositionRecommandation> findByIdUserAndStatutIn(Long idUser, List<String> statuts);
    
    List<PropositionRecommandation> findByIdRecommandation(String idRecommandation);
    
    List<PropositionRecommandation> findByDatePropositionBetween(LocalDateTime debut, LocalDateTime fin);
    
    List<PropositionRecommandation> findByStatutAndDatePropositionBefore(String statut, LocalDateTime avant);
    
    List<PropositionRecommandation> findByStatutAndPrioriteGreaterThanEqual(String statut, Integer prioriteMin);
    
    List<PropositionRecommandation> findByIdUserAndStatutAndPrioriteGreaterThanEqual(
        Long idUser, String statut, Integer priorite);
    
    List<PropositionRecommandation> findByScoreInteretGreaterThanEqual(Double scoreMin);
    
    List<PropositionRecommandation> findByIdUserOrderByPrioriteDescDatePropositionDesc(Long idUser);
    
    List<PropositionRecommandation> findByStatutOrderByDatePropositionDesc(String statut);
    
    List<PropositionRecommandation> findByIdUserAndNotificationEnvoyeeFalse(Long idUser);
    
    List<PropositionRecommandation> findPropositionsWithPendingNotification(Long idUser);
    
    PropositionRecommandation markNotificationSent(String id);
    
    List<PropositionRecommandation> findByFeedbackUserIsNotNull();
    
    List<PropositionRecommandation> findAnsweredPropositions(Long idUser);
    
    Long countByStatut(String statut);
    
    Long countByIdUserAndStatut(Long idUser, String statut);
    
    Long countByIdUserAndDatePropositionAfter(Long idUser, LocalDateTime depuis);
    
    Long countRecentPropositionsByUser(Long idUser, LocalDateTime depuis);
    
    PropositionRecommandation createProposition(Long idUser, String idRecommandation, Integer priorite);
    
    PropositionRecommandation acceptProposition(String id, String feedback);
    
    PropositionRecommandation rejectProposition(String id, String raisonRefus, String feedback);
    
    PropositionRecommandation ignoreProposition(String id);
    
    List<PropositionRecommandation> findPendingPropositions(Long idUser);
    
    List<PropositionRecommandation> findHighPriorityPropositions(Integer prioriteMin);
    
    Double calculateAcceptanceRate(Long idUser);
    
    List<PropositionRecommandation> findPropositionsByPeriod(LocalDateTime debut, LocalDateTime fin);
    
    Long countByStatus(String statut);
}