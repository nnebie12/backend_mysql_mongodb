package com.example.demo.servicesImplMysql;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.entitiesMysql.IngredientEntity;
import com.example.demo.repositoryMysql.IngredientRepository;
import com.example.demo.servicesMysql.IngredientService;

@Service
public class IngredientServiceImpl implements IngredientService{

private final IngredientRepository ingredientRepository;
    
    public IngredientServiceImpl(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }
    
    @Override
    public IngredientEntity saveIngredient(IngredientEntity ingredientEntity) {
        return ingredientRepository.save(ingredientEntity);
    }
    
    @Override
    public List<IngredientEntity> getAllIngredients() {
        return ingredientRepository.findAll();
    }
    
    @Override
    public Optional<IngredientEntity> getIngredientById(Long id) {
        return ingredientRepository.findById(id);
    }
    
    @Override
    public Optional<IngredientEntity> getIngredientByNom(String nom) {
        return ingredientRepository.findByNom(nom);
    }
    
    @Override
    public void deleteIngredient(Long id) {
        ingredientRepository.deleteById(id);
    }
    
    @Override
    public IngredientEntity updateIngredient(Long id, IngredientEntity ingredientDetails) {
    	IngredientEntity ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingredient not found"));
        
        ingredient.setNom(ingredientDetails.getNom());
        
        if (ingredientDetails.getCategorie() != null) {
            ingredient.setCategorie(ingredientDetails.getCategorie());
        }
        if (ingredientDetails.getUniteMesure() != null) {
            ingredient.setUniteMesure(ingredientDetails.getUniteMesure());
        }

        return ingredientRepository.save(ingredient);
    }
}
