package com.example.demo.repositoryMongoDB;

import com.example.demo.entiesMongodb.PropositionRecommandation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PropositionRecommandationRepository extends MongoRepository<PropositionRecommandation, String> {

    List<PropositionRecommandation> findByIdUser(Long idUser);

    List<PropositionRecommandation> findByStatut(String statut);

    List<PropositionRecommandation> findByIdUserAndStatut(Long idUser, String statut);

    Optional<PropositionRecommandation> findByIdUserAndIdRecommandation(Long idUser, String idRecommandation);

    List<PropositionRecommandation> findByIdUserAndNotificationEnvoyeeFalse(Long idUser);

    List<PropositionRecommandation> findByDatePropositionBetween(LocalDateTime debut, LocalDateTime fin);

    List<PropositionRecommandation> findByStatutAndPrioriteGreaterThanEqual(String statut, Integer prioriteMin);

    Long countByStatut(String statut);

    Long countByIdUserAndDatePropositionAfter(Long idUser, LocalDateTime depuis);

    List<PropositionRecommandation> findByIdUserAndStatutIn(Long idUser, List<String> statuts);

    Long countByIdUserAndStatut(Long idUser, String statut);

    List<PropositionRecommandation> findByIdUserAndStatutAndPrioriteGreaterThanEqual(
        Long idUser, String statut, Integer priorite);

    List<PropositionRecommandation> findByStatutAndDatePropositionBefore(String statut, LocalDateTime avant);

    List<PropositionRecommandation> findByIdRecommandation(String idRecommandation);

    List<PropositionRecommandation> findByIdUserOrderByPrioriteDescDatePropositionDesc(Long idUser);

    List<PropositionRecommandation> findByStatutOrderByDatePropositionDesc(String statut);

    List<PropositionRecommandation> findByScoreInteretGreaterThanEqual(Double scoreMin);

    List<PropositionRecommandation> findByFeedbackUserIsNotNull();
}