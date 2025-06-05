package com.example.demo.servicesMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.example.demo.entiesMongodb.RecetteInteraction;

public interface RecetteInteractionService {
    
    RecetteInteraction save(RecetteInteraction interaction);
    Optional<RecetteInteraction> findById(String id);
    List<RecetteInteraction> findAll();
    void supprimerInteraction(String id);
    
    List<RecetteInteraction> findByIdUser(Long idUser);
    List<RecetteInteraction> findByIdRecette(Long idRecette);
    List<RecetteInteraction> findByTypeInteraction(String typeInteraction);
    List<RecetteInteraction> findByIdUserAndIdRecette(Long idUser, Long idRecette);
    List<RecetteInteraction> findParSessionId(String sessionId);
    List<RecetteInteraction> findParDeviceType(String deviceType, LocalDateTime depuis);
    
    RecetteInteraction enregistrerInteraction(Long idUser, Long idRecette, String typeInteraction, String sessionId);
    RecetteInteraction enregistrerInteractionComplete(Long idUser, Long idRecette, String typeInteraction,
                                                     Integer duree, String deviceType, String sessionId,
                                                     String sourceInteraction, Map<String, Object> metadonnees);
    
    List<RecetteInteraction> findInteractionsParPeriode(LocalDateTime debut, LocalDateTime fin);
    List<RecetteInteraction> findInteractionsRecentesUser(Long idUser, LocalDateTime depuis);
    List<RecetteInteraction> findInteractionsPopulaires(String typeInteraction, LocalDateTime depuis, int limit);
    
    RecetteInteraction marquerComptabilisee(String id);
    List<RecetteInteraction> findInteractionsNonComptabilisees(Long idUser);
    
    Long compterConsultationsRecette(Long idRecette);
    Long compterInteractionsUser(Long idUser, LocalDateTime depuis);
    List<RecetteInteraction> getStatistiquesInteractionsRecette(Long idRecette);
    List<RecetteInteraction> getTopRecettesParInteractions(LocalDateTime depuis, int limit);
    Map<String, Long> getStatistiquesParTypeInteraction(Long idRecette);
    Map<String, Long> getStatistiquesParDevice(LocalDateTime depuis);
    
    Double calculerDureeMoyenneInteraction(Long idRecette, String typeInteraction);
}