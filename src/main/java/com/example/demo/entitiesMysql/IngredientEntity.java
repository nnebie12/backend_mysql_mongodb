package com.example.demo.entitiesMysql;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "ingredients")
public class IngredientEntity {
		    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;
	    
	    @Column(nullable = false, unique = true)
	    private String nom;
	    
	    @Column(name = "categorie", length = 100)
	    private String categorie;
	    
	    @Column(name = "unite_mesure", length = 50)
	    private String uniteMesure;
	    
	    @OneToMany(mappedBy = "ingredientEntity")
	    private List<RecetteIngredientEntity> recetteIngredients;
	
}
