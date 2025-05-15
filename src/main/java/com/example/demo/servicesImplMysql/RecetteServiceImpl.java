package com.example.demo.servicesImplMysql;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.entiesMongodb.CommentaireDocument;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entiesMongodb.RecetteDetailsDocument;
import com.example.demo.entitiesMysql.IngredientEntity;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.entitiesMysql.RecetteIngredientEntity;
import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.repositoryMongoDB.RecetteDetailsRepository;
import com.example.demo.repositoryMysql.IngredientRepository;
import com.example.demo.repositoryMysql.RecetteRepository;
import com.example.demo.repositoryMysql.UserRepository;
import com.example.demo.servicesMysql.RecetteService;

import jakarta.transaction.Transactional;


@Service
public class RecetteServiceImpl implements RecetteService {
    
    private final RecetteRepository recetteRepository;
    private final UserRepository userRepository;
    private final RecetteDetailsRepository recetteDetailsRepository;
    private final IngredientRepository ingredientRepository;

    
    public RecetteServiceImpl(RecetteRepository recetteRepository, 
            UserRepository userRepository,
            RecetteDetailsRepository recetteDetailsRepository,
            IngredientRepository ingredientRepository) {  
	this.recetteRepository = recetteRepository;
	this.userRepository = userRepository;
	this.recetteDetailsRepository = recetteDetailsRepository;
	this.ingredientRepository = ingredientRepository; 
}
    
    @Override
    @Transactional
    public RecetteEntity saveRecette(RecetteEntity recetteEntity, Long userEntityId) {
        UserEntity userEntity = userRepository.findById(userEntityId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        recetteEntity.setUserEntity(userEntity);
        recetteEntity.setDateCreation(LocalDateTime.now());
        
        RecetteEntity savedRecette = recetteRepository.save(recetteEntity);
        
        try {
            RecetteDetailsDocument recetteDetails = new RecetteDetailsDocument();
            recetteDetails.setRecetteId(savedRecette.getId().toString());
            recetteDetails.setCommentaires(new ArrayList<>());
            recetteDetails.setNotes(new ArrayList<>());
            recetteDetails.setMoyenneNotes(0.0);
            recetteDetails.setNombreCommentaires(0);
            recetteDetails.setNombreNotes(0);
            
            RecetteDetailsDocument savedDetails = recetteDetailsRepository.save(recetteDetails);
            
            savedRecette.setRecetteMongoId(savedDetails.getId());
            return recetteRepository.save(savedRecette);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'enregistrement dans MongoDB: " + e.getMessage());
            return savedRecette;
        }
    }
    
    @Override
    public List<RecetteEntity> getAllRecettes() {
        return recetteRepository.findAll();
    }
    
    @Override
    public Optional<RecetteEntity> getRecetteById(Long id) {
        return recetteRepository.findById(id);
    }
    
    @Override
    @Transactional
    public void deleteRecette(Long id) {
    	RecetteEntity recetteEntity = recetteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recette not found"));
        
        if (recetteEntity.getRecetteMongoId() != null) {
            recetteDetailsRepository.deleteById(recetteEntity.getRecetteMongoId());
        }
        
        recetteRepository.deleteById(id);
    }
    
    @Override
    public RecetteEntity updateRecette(Long id, RecetteEntity recetteDetails) {
        RecetteEntity recetteEntity = recetteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recette not found"));
        
        recetteEntity.setTitre(recetteDetails.getTitre());
        recetteEntity.setDescription(recetteDetails.getDescription());
        recetteEntity.setTempsPreparation(recetteDetails.getTempsPreparation());
        recetteEntity.setTempsCuisson(recetteDetails.getTempsCuisson());
        recetteEntity.setDifficulte(recetteDetails.getDifficulte());        
        return recetteRepository.save(recetteEntity);
    }
    
    @Override
    public List<RecetteEntity> getRecettesByUser(Long userEntityId) {
        UserEntity userEntity = userRepository.findById(userEntityId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return recetteRepository.findByUserEntity(userEntity);
    }
    
    @Override
    @Transactional
    public void addIngredientToRecette(Long recetteEntityId, Long ingredientEntityId, String quantite) {
        RecetteEntity recetteEntity = recetteRepository.findById(recetteEntityId)
                .orElseThrow(() -> new RuntimeException("Recette not found"));
        IngredientEntity ingredientEntity = ingredientRepository.findById(ingredientEntityId)
                .orElseThrow(() -> new RuntimeException("Ingredient not found"));
        
        recetteEntity.addIngredientEntity(ingredientEntity, quantite);
        recetteRepository.save(recetteEntity);
    }

    @Override
    @Transactional
    public void removeIngredientFromRecette(Long recetteEntityId, Long ingredientId) {
        RecetteEntity recette = recetteRepository.findById(recetteEntityId)
                .orElseThrow(() -> new RuntimeException("Recette not found"));
        
        recette.getRecetteIngredients().removeIf(ri -> ri.getIngredientEntity().getId().equals(ingredientId));
        recetteRepository.save(recette);
    }

    @Override
    public List<RecetteIngredientEntity> getRecetteIngredients(Long recetteEntityId) {
        RecetteEntity recette = recetteRepository.findById(recetteEntityId)
                .orElseThrow(() -> new RuntimeException("Recette not found"));
        return recette.getRecetteIngredients();
    }
    
    
    @Override
    @Transactional
    public CommentaireDocument addCommentaire(Long recetteEntityId, Long userId, CommentaireDocument commentaire) {
        RecetteEntity recetteEntity = recetteRepository.findById(recetteEntityId)
                .orElseThrow(() -> new RuntimeException("Recette not found"));
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        RecetteDetailsDocument details = recetteDetailsRepository.findById(recetteEntity.getRecetteMongoId())
                .orElseThrow(() -> new RuntimeException("Recette details not found"));
        
        commentaire.setDateCommentaire(LocalDateTime.now());
        commentaire.setUserId(userEntity.getId().toString());
        commentaire.setUserName(userEntity.getPrenom());
        
        if (details.getCommentaires() == null) {
            details.setCommentaires(new ArrayList<>());
        }
        
        details.getCommentaires().add(commentaire);
        details.setNombreCommentaires(details.getCommentaires().size());
        
        recetteDetailsRepository.save(details);
        return commentaire;
    }
    
    @Override
    @Transactional
    public NoteDocument addNote(Long recetteEntityId, Long userId, NoteDocument note) {
        RecetteEntity recetteEntity = recetteRepository.findById(recetteEntityId)
                .orElseThrow(() -> new RuntimeException("Recette not found"));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        RecetteDetailsDocument details = recetteDetailsRepository.findById(recetteEntity.getRecetteMongoId())
        	    .orElseThrow(() -> new RuntimeException("Recette details not found"));
        
        note.setUserId(user.getId().toString());
        note.setUserName(user.getPrenom());
        
        if (details.getNotes() == null) {
            details.setNotes(new ArrayList<>());
        }
        
        Optional<NoteDocument> existingNote = details.getNotes().stream()
                .filter(n -> n.getUserId().equals(user.getId().toString()))
                .findFirst();
        
        if (existingNote.isPresent()) {
            existingNote.get().setValeur(note.getValeur());
        } else {
            details.getNotes().add(note);
        }
        
        double moyenne = details.getNotes().stream()
                .mapToInt(NoteDocument::getValeur)
                .average()
                .orElse(0.0);
        
        details.setMoyenneNotes(moyenne);
        details.setNombreNotes(details.getNotes().size());
        
        recetteDetailsRepository.save(details);
        return note;
    }
    
    @Override
    public List<CommentaireDocument> getCommentairesByRecette(Long recetteEntityId) {
        RecetteEntity recetteEntity = recetteRepository.findById(recetteEntityId)
                .orElseThrow(() -> new RuntimeException("Recette not found"));
        
        RecetteDetailsDocument details = recetteDetailsRepository.findById(recetteEntity.getRecetteMongoId())
                .orElseThrow(() -> new RuntimeException("Recette details not found"));
        
        return details.getCommentaires() != null ? details.getCommentaires() : new ArrayList<>();
    }
    
    @Override
    public List<NoteDocument> getNotesByRecette(Long recetteEntityId) {
    	RecetteEntity recetteEntity = recetteRepository.findById(recetteEntityId)
                .orElseThrow(() -> new RuntimeException("Recette not found"));
        
        RecetteDetailsDocument details = recetteDetailsRepository.findById(recetteEntity.getRecetteMongoId())
                .orElseThrow(() -> new RuntimeException("Recette details not found"));
        
        return details.getNotes() != null ? details.getNotes() : new ArrayList<>();
    }
    
    @Override
    public Double getMoyenneNotesByRecette(Long recetteEntityId) {
    	RecetteEntity recetteEntity = recetteRepository.findById(recetteEntityId)
                .orElseThrow(() -> new RuntimeException("Recette not found"));
        
        RecetteDetailsDocument details = recetteDetailsRepository.findById(recetteEntity.getRecetteMongoId())
                .orElseThrow(() -> new RuntimeException("Recette details not found"));
        
        return details.getMoyenneNotes();
    }
    
    @Override
    public Map<String, Object> getRecetteDetails(Long recetteEntityId) {
    	RecetteEntity recette = recetteRepository.findById(recetteEntityId)
                .orElseThrow(() -> new RuntimeException("Recette not found"));
        
        RecetteDetailsDocument details = recetteDetailsRepository.findById(recette.getRecetteMongoId())
                .orElseThrow(() -> new RuntimeException("Recette details not found"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("recette", recette);
        response.put("commentaires", details.getCommentaires());
        response.put("notes", details.getNotes());
        response.put("moyenneNotes", details.getMoyenneNotes());
        response.put("nombreCommentaires", details.getNombreCommentaires());
        response.put("nombreNotes", details.getNombreNotes());
        
        return response;
    }
	}
