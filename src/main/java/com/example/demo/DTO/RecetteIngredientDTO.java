package com.example.demo.DTO;

import lombok.Data;

@Data
public class RecetteIngredientDTO {
    private Long recetteId;
    private Long ingredientEntityId;
    private String quantite;
    private String uniteMesure;
    private String ingredientName;
}