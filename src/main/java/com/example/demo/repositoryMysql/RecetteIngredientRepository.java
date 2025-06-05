package com.example.demo.repositoryMysql;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entitiesMysql.RecetteIngredientEntity;
import com.example.demo.entitiesMysql.RecetteIngredientId;

@Repository
public interface RecetteIngredientRepository extends JpaRepository<RecetteIngredientEntity, RecetteIngredientId> {
    
    // Recherche par recette_id
    List<RecetteIngredientEntity> findByRecetteId(Long recetteId);
    
    // Recherche par ingredient_id  
    List<RecetteIngredientEntity> findByIngredientId(Long ingredientId);
    
    // Recherche par la combinaison recette_id et ingredient_id
    Optional<RecetteIngredientEntity> findByRecetteIdAndIngredientId(Long recetteId, Long ingredientId);
    
    // Suppression par recette_id
    void deleteByRecetteId(Long recetteId);
    
    // Suppression par la combinaison recette_id et ingredient_id
    void deleteByRecetteIdAndIngredientId(Long recetteId, Long ingredientId);
    
    // Si vous voulez garder les méthodes avec les noms d'entités (optionnel)
    List<RecetteIngredientEntity> findByRecetteEntityId(Long recetteEntityId);
    List<RecetteIngredientEntity> findByIngredientEntityId(Long ingredientEntityId);
    Optional<RecetteIngredientEntity> findByRecetteEntityIdAndIngredientEntityId(Long recetteEntityId, Long ingredientEntityId);
    void deleteByRecetteEntityId(Long recetteEntityId);
    void deleteByRecetteEntityIdAndIngredientEntityId(Long recetteEntityId, Long ingredientEntityId);
}