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
    
    PropositionRecommandation creerProposition(Long idUser, String idRecommandation, Integer priorite);
    
    PropositionRecommandation accepterProposition(String id, String feedback);
    
    PropositionRecommandation refuserProposition(String id, String raisonRefus, String feedback);
    
    PropositionRecommandation ignorerProposition(String id);
    
    List<PropositionRecommandation> findPropositionsPendantes(Long idUser);
    
    List<PropositionRecommandation> findPropositionsAvecNotificationPendante(Long idUser);
    
    PropositionRecommandation marquerNotificationEnvoyee(String id);
    
    List<PropositionRecommandation> findPropositionsParPeriode(LocalDateTime debut, LocalDateTime fin);
    
    List<PropositionRecommandation> findPropositionsHautePriorite(Integer prioriteMin);
    
    void supprimerProposition(String id);
    
    Long compterParStatut(String statut);
    
    Long compterPropositionsRecentesUser(Long idUser, LocalDateTime depuis);
    
    Double calculerTauxAcceptation(Long idUser);
    
    List<PropositionRecommandation> findPropositionsRepondues(Long idUser);
}
