package com.example.demo.repositoryMysql;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entitiesMysql.RecetteIngredientEntity;

@Repository
public interface RecetteIngredientRepository extends JpaRepository<RecetteIngredientEntity, Long> {
    List<RecetteIngredientEntity> findByRecetteEntityId(Long recetteEntityId);
    List<RecetteIngredientEntity> findByIngredientEntityId(Long ingredientEntityId);
    Optional<RecetteIngredientEntity> findByRecetteEntityIdAndIngredientEntityId(Long recetteEntityId, Long ingredientEntityId);
    void deleteByRecetteEntityId(Long recetteEntityId);
	void deleteByRecetteEntityIdAndIngredientEntityId(Long recetteEntityId, Long ingredientEntityId);
}
