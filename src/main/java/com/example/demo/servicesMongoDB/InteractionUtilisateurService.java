package com.example.demo.servicesMongoDB;

import java.util.List;

import com.example.demo.entiesMongodb.InteractionUtilisateur;

public interface InteractionUtilisateurService {
    InteractionUtilisateur addInteractionUtilisateur(Long userId, String typeInteraction, Long recetteId, Integer dureeConsultation);
    
    List<InteractionUtilisateur> getInteractionsByUserId(Long userId);
    
    List<InteractionUtilisateur> getInteractionsByUserIdAndType(Long userId, String typeInteraction);
    
    List<InteractionUtilisateur> getInteractionsByRecetteIdAndType(Long recetteId, String typeInteraction);
    
    List<InteractionUtilisateur> getAllInteractions();
    
    void deleteInteractionsUtilisateur(Long userId);
}
