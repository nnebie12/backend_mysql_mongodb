package com.example.demo.servicesImplMongoDB;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.example.demo.entiesMongodb.RecetteInteraction;
import com.example.demo.repositoryMongoDB.RecetteInteractionRepository;
import com.example.demo.servicesMongoDB.RecetteInteractionService;

@Service
public class RecetteInteractionServiceImpl implements RecetteInteractionService {
    
    @Autowired
    private RecetteInteractionRepository repository;
    
    @Override
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public RecetteInteraction save(RecetteInteraction interaction) {
        return repository.save(interaction);
    }
    
    @Override
    public Optional<RecetteInteraction> findById(String id) {
        return repository.findById(id);
    }
    
    @Override
    public List<RecetteInteraction> findAll() {
        return repository.findAll();
    }
    
    @Override
    public List<RecetteInteraction> findByIdUser(Long idUser) {
        return repository.findByIdUser(idUser);
    }
    
    @Override
    public List<RecetteInteraction> findByIdRecette(Long idRecette) {
        return repository.findByIdRecette(idRecette);
    }
    
    @Override
    public List<RecetteInteraction> findByTypeInteraction(String typeInteraction) {
        return repository.findByTypeInteraction(typeInteraction);
    }
    
    @Override
    public List<RecetteInteraction> findByIdUserAndIdRecette(Long idUser, Long idRecette) {
        return repository.findByIdUserAndIdRecette(idUser, idRecette);
    }
    
    @Override
    public RecetteInteraction enregistrerInteraction(Long idUser, Long idRecette, String typeInteraction, String sessionId) {
        RecetteInteraction interaction = new RecetteInteraction();
        interaction.setIdUser(idUser);
        interaction.setIdRecette(idRecette);
        interaction.setTypeInteraction(typeInteraction);
        interaction.setDateInteraction(LocalDateTime.now());
        interaction.setSessionId(sessionId);
        interaction.setComptabilisee(false);
        interaction.setDeviceType("UNKNOWN");
        interaction.setSourceInteraction("DIRECT");
        
        return repository.save(interaction);
    }
    
    @Override
    public RecetteInteraction enregistrerInteractionComplete(Long idUser, Long idRecette, String typeInteraction,
                                                           Integer duree, String deviceType, String sessionId,
                                                           String sourceInteraction, Map<String, Object> metadonnees) {
        RecetteInteraction interaction = new RecetteInteraction();
        interaction.setIdUser(idUser);
        interaction.setIdRecette(idRecette);
        interaction.setTypeInteraction(typeInteraction);
        interaction.setDateInteraction(LocalDateTime.now());
        interaction.setDureeInteraction(duree);
        interaction.setDeviceType(deviceType != null ? deviceType : "UNKNOWN");
        interaction.setSessionId(sessionId);
        interaction.setSourceInteraction(sourceInteraction != null ? sourceInteraction : "DIRECT");
        interaction.setMetadonnees(metadonnees);
        interaction.setComptabilisee(false);
        
        return repository.save(interaction);
    }
    
    @Override
    public List<RecetteInteraction> findInteractionsPopulaires(String typeInteraction, LocalDateTime depuis, int limit) {
        List<RecetteInteraction> interactions = repository.findByTypeInteraction(typeInteraction);
        return interactions.stream()
                .filter(i -> i.getDateInteraction().isAfter(depuis))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RecetteInteraction> findInteractionsParPeriode(LocalDateTime debut, LocalDateTime fin) {
        return repository.findByDateInteractionBetween(debut, fin);
    }
    
    @Override
    public List<RecetteInteraction> findInteractionsRecentesUser(Long idUser, LocalDateTime depuis) {
        return repository.findByIdUserAndDateInteractionAfter(idUser, depuis);
    }
    
    @Override
    public List<RecetteInteraction> findParSessionId(String sessionId) {
        return repository.findBySessionId(sessionId);
    }
    
    @Override
    public List<RecetteInteraction> findParDeviceType(String deviceType, LocalDateTime depuis) {
        return repository.findByDeviceTypeAndDateInteractionAfter(deviceType, depuis);
    }
    
    @Override
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public RecetteInteraction marquerComptabilisee(String id) {
        Optional<RecetteInteraction> optInteraction = repository.findById(id);
        if (optInteraction.isPresent()) {
            RecetteInteraction interaction = optInteraction.get();
            interaction.setComptabilisee(true);
            return repository.save(interaction);
        }
        throw new RuntimeException("Interaction non trouvée avec l'ID: " + id);
    }
    
    @Override
    public List<RecetteInteraction> findInteractionsNonComptabilisees(Long idUser) {
        return repository.findByIdUserAndComptabilisee(idUser, false);
    }
    
    @Override
    @PreAuthorize("hasRole('ADMININISTRATEUR')")
    public void supprimerInteraction(String id) {
        repository.deleteById(id);
    }
    
    @Override
    public Long compterConsultationsRecette(Long idRecette) {
        return repository.countByIdRecetteAndTypeInteraction(idRecette, "CONSULTATION");
    }
    
    @Override
    public Long compterInteractionsUser(Long idUser, LocalDateTime depuis) {
        return (long) repository.findByIdUserAndDateInteractionAfter(idUser, depuis).size();
    }
    
    @Override
    public List<RecetteInteraction> getStatistiquesInteractionsRecette(Long idRecette) {
        return repository.findByIdRecetteOrderByDateInteractionDesc(idRecette);
    }
    
    @Override
    public List<RecetteInteraction> getTopRecettesParInteractions(LocalDateTime depuis, int limit) {
        return repository.findByDateInteractionAfterOrderByDateInteractionDesc(depuis)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Long> getStatistiquesParTypeInteraction(Long idRecette) {
        List<RecetteInteraction> interactions = repository.findByIdRecette(idRecette);
        Map<String, Long> stats = new HashMap<>();
        
        for (RecetteInteraction interaction : interactions) {
            stats.merge(interaction.getTypeInteraction(), 1L, Long::sum);
        }
        
        return stats;
    }
    
    @Override
    public Map<String, Long> getStatistiquesParDevice(LocalDateTime depuis) {
        List<RecetteInteraction> interactions = repository.findByDateInteractionBetween(depuis, LocalDateTime.now());
        Map<String, Long> stats = new HashMap<>();
        
        for (RecetteInteraction interaction : interactions) {
            stats.merge(interaction.getDeviceType(), 1L, Long::sum);
        }
        
        return stats;
    }
    
    @Override
    public Double calculerDureeMoyenneInteraction(Long idRecette, String typeInteraction) {
        List<RecetteInteraction> interactions = repository.findByIdRecetteAndTypeInteraction(idRecette, typeInteraction);
        
        return interactions.stream()
                .filter(i -> i.getDureeInteraction() != null)
                .mapToInt(RecetteInteraction::getDureeInteraction)
                .average()
                .orElse(0.0);
    }
}