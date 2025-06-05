package com.example.demo.entitiesMysql;

import java.io.Serializable;
import java.util.Objects;

import lombok.Data;

@Data
public class RecetteIngredientId implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long recetteId;
    private Long ingredientId;
    
    // Constructeur 
    public RecetteIngredientId() {}
    
    // Constructeur avec paramètres
    public RecetteIngredientId(Long recetteId, Long ingredientId) {
        this.recetteId = recetteId;
        this.ingredientId = ingredientId;
    }
    
    // equals et hashCode 
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecetteIngredientId that = (RecetteIngredientId) o;
        return Objects.equals(recetteId, that.recetteId) && 
               Objects.equals(ingredientId, that.ingredientId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(recetteId, ingredientId);
    }
    
    @Override
    public String toString() {
        return "RecetteIngredientId{" +
                "recetteId=" + recetteId +
                ", ingredientId=" + ingredientId +
                '}';
    }
}