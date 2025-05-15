package com.example.demo.entitiesMysql;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "recettes")
public class RecetteEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String titre;
    
    @Column(length = 2000)
    private String description;
    
    @Column
    private Integer tempsPreparation;
    
    @Column
    private Integer tempsCuisson;
    
    @Column
    private String difficulte;
    
    @Column(nullable = false)
    private LocalDateTime dateCreation;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;
    
    @OneToMany(mappedBy = "recetteEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecetteIngredientEntity> recetteIngredients = new ArrayList<>();
    
    @Column(unique = true)
    private String recetteMongoId;

    public void addIngredientEntity(IngredientEntity ingredient, String quantite) {
        RecetteIngredientEntity ri = new RecetteIngredientEntity();
        ri.setRecetteEntity(this);
        ri.setIngredientEntity(ingredient);
        ri.setQuantite(quantite);
        this.recetteIngredients.add(ri);
    }
    
}
