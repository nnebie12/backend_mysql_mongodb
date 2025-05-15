package com.example.demo.entitiesMysql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "recette_ingredients")
public class RecetteIngredientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "recette_id", nullable = false)
    private RecetteEntity recetteEntity;
    
    @ManyToOne
    @JoinColumn(name = "ingredient_id", nullable = false)
    private IngredientEntity ingredientEntity;
    
    @Column(name = "quantite", nullable = false)
    private String quantite;
    
    @Column(name = "instruction")
    private String instruction;
    
}
