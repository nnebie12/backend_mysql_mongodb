package com.example.demo.servicesImplMongoDB;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.entiesMongodb.InteractionUtilisateur;
import com.example.demo.repositoryMongoDB.InteractionUtilisateurRepository;
import com.example.demo.servicesMongoDB.InteractionUtilisateurService;

@Service
public class InteractionUtilisateurServiceImpl implements InteractionUtilisateurService {
    private final InteractionUtilisateurRepository interactionRepository;
    
    public InteractionUtilisateurServiceImpl(InteractionUtilisateurRepository interactionRepository) {
        this.interactionRepository = interactionRepository;
    }
    
    @Override
    public InteractionUtilisateur addInteractionUtilisateur(Long userId, String typeInteraction, Long entiteId, 
                                                        Integer dureeConsultation) {
        InteractionUtilisateur interaction = new InteractionUtilisateur();
        interaction.setUserId(userId);
        interaction.setTypeInteraction(typeInteraction);
        interaction.setEntiteId(entiteId);
        interaction.setDateInteraction(LocalDateTime.now());
        interaction.setDureeConsultation(dureeConsultation);
        
        return interactionRepository.save(interaction);
    }
    
    @Override
    public List<InteractionUtilisateur> getInteractionsByUserId(Long userId) {
        return interactionRepository.findByUserId(userId);
    }
    
    @Override
    public List<InteractionUtilisateur> getInteractionsByUserIdAndType(Long userId, String typeInteraction) {
        return interactionRepository.findByUserIdAndTypeInteraction(userId, typeInteraction);
    }
    
    @Override
    public List<InteractionUtilisateur> getInteractionsByEntiteIdAndType(Long entiteId, String typeInteraction) {
        return interactionRepository.findByEntiteIdAndTypeInteraction(entiteId, typeInteraction);
    }
    
    @Override
    public void deleteInteractionsUtilisateur(Long userId) {
        interactionRepository.deleteByUserId(userId);
    }
}
