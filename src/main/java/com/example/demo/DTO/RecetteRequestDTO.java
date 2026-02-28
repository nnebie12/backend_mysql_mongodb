package com.example.demo.DTO;

import lombok.Data;

@Data
public class RecetteRequestDTO {
    private String titre;
    private String description;
    private Integer tempsPreparation;
    private Integer tempsCuisson;
    private String difficulte;

    private String cuisine;
    private String typeRecette;
    private Boolean vegetarien;
    private String categorie;
    private String saison;
    private String typeCuisine;
    private String imageUrl;
}