package com.example.demo.servicesMysql;

import java.util.List;
import com.example.demo.entitiesMysql.RecetteIngredientEntity;

public interface RecetteIngredientService {
	RecetteIngredientEntity addIngredientRecetteEntity(Long recetteEntityId, Long ingredientEntityId,
            String quantite, String uniteMesure);
	
    List<RecetteIngredientEntity> getIngredientsByRecetteEntityId(Long recetteEntityId);
    
    void deleteIngredientRecetteEntity(Long recetteEntityId, Long ingredientEntityId); 
    
    void deleteAllIngredientsRecetteEntity(Long recetteEntityId);
    
    void deleteRecetteEntity(Long recetteEntityId);
    
    void deleteRecetteIngredient(Long ingredientEntityId, Long recetteEntityId);
    
    boolean existsRecetteIngredient(Long ingredientEntityId, Long recetteEntityId);
    
}