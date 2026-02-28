package com.example.demo.repositoryMongoDB;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entiesMongodb.InteractionUtilisateur;

@Repository
public interface InteractionUtilisateurRepository extends MongoRepository<InteractionUtilisateur, String> {
	
    List<InteractionUtilisateur> findByUserId(Long userId);
    List<InteractionUtilisateur> findByUserIdAndTypeInteraction(Long userId, String typeInteraction);
    List<InteractionUtilisateur> findByRecetteIdAndTypeInteraction(Long recetteId, String typeInteraction);
    void deleteByUserId(Long userId);
}
