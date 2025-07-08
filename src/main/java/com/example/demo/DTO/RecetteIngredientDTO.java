package com.example.demo.DTO;

import lombok.Data;

@Data
public class RecetteIngredientDTO {
    private Long recetteEntityId;
    private Long ingredientEntityId;
    private String quantite;
    private String instruction;
    private String ingredientName;
}