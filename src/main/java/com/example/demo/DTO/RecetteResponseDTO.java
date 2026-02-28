package com.example.demo.DTO;

import java.time.LocalDateTime;
import java.util.List;

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

    private String typeRecette;
    private String cuisine;
    private String imageUrl;
    private Boolean vegetarien;

    private Double popularite;
    private String categorie;
    private String saison;
    private String typeCuisine;

    private Long userId;
    private String userName;

    private List<RecetteIngredientDTO> ingredients;

    private List<CommentaireResponseDTO> commentaires;
    private List<NoteResponseDTO> notes;

    private Double moyenneNotes;
    private Integer nombreCommentaires;
    private Integer nombreNotes;
}