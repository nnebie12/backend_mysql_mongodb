package com.example.demo.repositoryMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entiesMongodb.RecetteInteraction;

@Repository
public interface RecetteInteractionRepository extends MongoRepository<RecetteInteraction, String> {
    
    List<RecetteInteraction> findByIdUser(Long idUser);
    
    List<RecetteInteraction> findByIdRecette(Long idRecette);
    
    List<RecetteInteraction> findByTypeInteraction(String typeInteraction);
    
    List<RecetteInteraction> findByIdUserAndIdRecette(Long idUser, Long idRecette);
    
    List<RecetteInteraction> findByIdRecetteAndTypeInteraction(Long idRecette, String typeInteraction);
    
    List<RecetteInteraction> findByDateInteractionBetween(LocalDateTime debut, LocalDateTime fin);
    
    List<RecetteInteraction> findBySessionId(String sessionId);
    
    List<RecetteInteraction> findByIdUserAndDateInteractionAfter(Long idUser, LocalDateTime depuis);
    
    Long countByIdRecetteAndTypeInteraction(Long idRecette, String typeInteraction);
    
    List<RecetteInteraction> findByIdUserAndComptabilisee(Long idUser, Boolean comptabilisee);
    
    List<RecetteInteraction> findByDeviceTypeAndDateInteractionAfter(String deviceType, LocalDateTime date);
    
    List<RecetteInteraction> findByIdRecetteOrderByDateInteractionDesc(Long idRecette);
    
    List<RecetteInteraction> findByDateInteractionAfterOrderByDateInteractionDesc(LocalDateTime depuis);
}