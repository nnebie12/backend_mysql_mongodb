package com.example.demo.servicesImplMongoDB;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.example.demo.entiesMongodb.InteractionUtilisateur;
import com.example.demo.repositoryMongoDB.InteractionUtilisateurRepository;
import com.example.demo.servicesMongoDB.InteractionUtilisateurService;

@Service
public class InteractionUtilisateurServiceImpl implements InteractionUtilisateurService {

    private static final Logger logger = LoggerFactory.getLogger(InteractionUtilisateurServiceImpl.class);

    private final InteractionUtilisateurRepository interactionRepository;

    public InteractionUtilisateurServiceImpl(InteractionUtilisateurRepository interactionRepository) {
        this.interactionRepository = interactionRepository;
    }

    @Override
    public InteractionUtilisateur addInteractionUtilisateur(Long userId, String typeInteraction,
                                                            Long recetteId, Integer dureeConsultation) {
        InteractionUtilisateur interaction = new InteractionUtilisateur();
        interaction.setUserId(userId);
        interaction.setTypeInteraction(typeInteraction);
        interaction.setRecetteId(recetteId);
        interaction.setDateInteraction(LocalDateTime.now());
        interaction.setDureeConsultation(dureeConsultation);
        return interactionRepository.save(interaction);
    }

    @Override
    public List<InteractionUtilisateur> getInteractionsByUserId(Long userId) {
        return interactionRepository.findByUserId(userId);
    }

    @Override
    public List<InteractionUtilisateur> getAllInteractions() {
        return interactionRepository.findAll();
    }

    @Override
    public List<InteractionUtilisateur> getInteractionsByUserIdAndType(Long userId, String typeInteraction) {
        return interactionRepository.findByUserIdAndTypeInteraction(userId, typeInteraction);
    }

    @Override
    public List<InteractionUtilisateur> getInteractionsByRecetteIdAndType(Long recetteId, String typeInteraction) {
        return interactionRepository.findByRecetteIdAndTypeInteraction(recetteId, typeInteraction);
    }

    
    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR')")
    public void deleteInteractionsUtilisateur(Long userId) {
        logger.info("Suppression des interactions de l'utilisateur {} par un administrateur", userId);
        interactionRepository.deleteByUserId(userId);
    }
}