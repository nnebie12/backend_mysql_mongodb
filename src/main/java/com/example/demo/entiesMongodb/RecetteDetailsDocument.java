package com.example.demo.entiesMongodb;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "recette_details")
public class RecetteDetailsDocument {
	@Id
    private String id;
    
    private String recetteId; 
    
    private List<CommentaireDocument> commentaires;
    private List<NoteDocument> notes;
    
   
    private Double moyenneNotes;
    private Integer nombreCommentaires;
    private Integer nombreNotes;
}
