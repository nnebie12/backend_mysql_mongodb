package com.example.demo.servicesMysql;

import java.util.List;
import java.util.Optional;

import com.example.demo.entitiesMysql.IngredientEntity;

public interface IngredientService {
	
    IngredientEntity saveIngredient(IngredientEntity ingredient);
    List<IngredientEntity> getAllIngredients();
    Optional<IngredientEntity> getIngredientById(Long id);
    Optional<IngredientEntity> getIngredientByNom(String nom);
    void deleteIngredient(Long id);
    IngredientEntity updateIngredient(Long id, IngredientEntity ingredientDetails);
}
