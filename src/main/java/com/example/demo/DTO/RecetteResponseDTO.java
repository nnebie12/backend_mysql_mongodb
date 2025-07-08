package com.example.demo.DTO;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.entiesMongodb.CommentaireDocument;
import com.example.demo.entiesMongodb.NoteDocument;

import lombok.Data;

@Data
public class RecetteResponseDTO {
    private Long id;
    private String titre;
    private String description;
    private Integer tempsPreparation;
    private Integer tempsCuisson;
    private String difficulte;
    private LocalDateTime dateCreation;
    private String recetteMongoId;

    private Long userId; 
    private String userName;
    
    private List<RecetteIngredientDTO> ingredients; 
    
    private List<CommentaireDocument> commentaires;
    private List<NoteDocument> notes;
    private Double moyenneNotes;
    private Integer nombreCommentaires;
    private Integer nombreNotes;
}