package com.example.demo.servicesMongoDB;

import java.util.List;

import com.example.demo.entiesMongodb.InteractionUtilisateur;

public interface InteractionUtilisateurService {
    InteractionUtilisateur addInteractionUtilisateur(Long userId, String typeInteraction, Long entiteId, Integer dureeConsultation);
    List<InteractionUtilisateur> getInteractionsByUserId(Long userId);
    List<InteractionUtilisateur> getInteractionsByUserIdAndType(Long userId, String typeInteraction);
    List<InteractionUtilisateur> getInteractionsByEntiteIdAndType(Long entiteId, String typeInteraction);
    List<InteractionUtilisateur> getAllInteractions();
    void deleteInteractionsUtilisateur(Long userId);
}
