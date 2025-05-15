package com.example.demo.servicesMysql;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.demo.entiesMongodb.CommentaireDocument;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.entitiesMysql.RecetteIngredientEntity;

public interface RecetteService {
	RecetteEntity saveRecette(RecetteEntity recetteEntity, Long userId);
    Optional<RecetteEntity> getRecetteById(Long id);
    RecetteEntity updateRecette(Long id, RecetteEntity recetteDetails);
    List<RecetteEntity> getRecettesByUser(Long userId);
    List<RecetteEntity> getAllRecettes();
    void deleteRecette(Long id);

    
    CommentaireDocument addCommentaire(Long recetteId, Long userId, CommentaireDocument commentaire);
    NoteDocument addNote(Long recetteId, Long userId, NoteDocument note);
    List<CommentaireDocument> getCommentairesByRecette(Long recetteId);
    List<NoteDocument> getNotesByRecette(Long recetteId);
    Double getMoyenneNotesByRecette(Long recetteId);
    Map<String, Object> getRecetteDetails(Long recetteId);
    
    void addIngredientToRecette(Long recetteId, Long ingredientId, String quantite);
    void removeIngredientFromRecette(Long recetteId, Long ingredientId);
    List<RecetteIngredientEntity> getRecetteIngredients(Long recetteId);
    }
