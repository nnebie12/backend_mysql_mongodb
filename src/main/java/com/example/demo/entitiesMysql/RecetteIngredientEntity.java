package com.example.demo.entitiesMysql;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "recette_ingredients")
@IdClass(RecetteIngredientId.class)  
public class RecetteIngredientEntity {
    
    @Id  
    @Column(name = "recette_id")
    private Long recetteId;
    
    @Id  
    @Column(name = "ingredient_id") 
    private Long ingredientId;
    
    @ManyToOne
    @JoinColumn(name = "recette_id", insertable = false, updatable = false)
    @JsonBackReference
    private RecetteEntity recetteEntity;
    
    @ManyToOne
    @JoinColumn(name = "ingredient_id", insertable = false, updatable = false)
    private IngredientEntity ingredientEntity;
    
    @Column(name = "quantite", nullable = false)
    private String quantite;
    
    @Column(name = "instruction")
    private String instruction;
}