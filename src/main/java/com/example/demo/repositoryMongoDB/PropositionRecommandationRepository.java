package com.example.demo.repositoryMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entiesMongodb.PropositionRecommandation;

@Repository
public interface PropositionRecommandationRepository extends MongoRepository<PropositionRecommandation, String> {
    
    List<PropositionRecommandation> findByIdUser(Long idUser);
    
    List<PropositionRecommandation> findByStatut(String statut);
    
    List<PropositionRecommandation> findByIdUserAndStatut(Long idUser, String statut);
    
    Optional<PropositionRecommandation> findByIdUserAndIdRecommandation(Long idUser, String idRecommandation);
    
    List<PropositionRecommandation> findByIdUserAndNotificationEnvoyeeFalse(Long idUser);
    
    List<PropositionRecommandation> findByDatePropositionBetween(LocalDateTime debut, LocalDateTime fin);
    
    List<PropositionRecommandation> findByStatutAndPrioriteGreaterThanEqual(String statut, Integer priorite);
    
    Long countByStatut(String statut);
    
    Long countByIdUserAndDatePropositionAfter(Long idUser, LocalDateTime dateDebut);
    
    Long countByIdUserAndStatut(Long idUser, String statut);
    
    List<PropositionRecommandation> findByIdUserAndStatutIn(Long idUser, List<String> statuts);
}