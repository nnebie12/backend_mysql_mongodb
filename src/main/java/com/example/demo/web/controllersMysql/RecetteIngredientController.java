package com.example.demo.web.controllersMysql;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTO.RecetteIngredientDTO;
import com.example.demo.entitiesMysql.RecetteIngredientEntity;
import com.example.demo.servicesMysql.RecetteIngredientService;

@RestController
@RequestMapping("/api/recetteIngredient")
public class RecetteIngredientController {
    private final RecetteIngredientService recetteIngredientService;
    
    public RecetteIngredientController(RecetteIngredientService recetteIngredientService) {
        this.recetteIngredientService = recetteIngredientService;
    }
    
    @PostMapping
    public ResponseEntity<RecetteIngredientEntity> addIngredientRecetteEntity(@RequestBody RecetteIngredientDTO dto) {
        RecetteIngredientEntity recetteIngredientEntity = recetteIngredientService.addIngredientRecetteEntity(
                dto.getRecetteId(), dto.getIngredientEntityId(), dto.getQuantite(), dto.getUniteMesure());
        return new ResponseEntity<>(recetteIngredientEntity, HttpStatus.CREATED);
    }

    
    @GetMapping("/recette/{recetteId}")
    public ResponseEntity<List<RecetteIngredientEntity>> getIngredientsByRecetteId(@PathVariable Long recetteId) {
        List<RecetteIngredientEntity> ingredients = recetteIngredientService.getIngredientsByRecetteEntityId(recetteId);
        return new ResponseEntity<>(ingredients, HttpStatus.OK);
    }
    
    @DeleteMapping("/recette/{recetteId}")
    public ResponseEntity<Void> deleteAllIngredientsRecetteEntity(@PathVariable Long recetteId) {
        recetteIngredientService.deleteAllIngredientsRecetteEntity(recetteId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @DeleteMapping("/{recetteId}/{ingredientId}")
    public ResponseEntity<Void> deleteIngredientRecetteEntity(
            @PathVariable Long recetteId, 
            @PathVariable Long ingredientId) {
        recetteIngredientService.deleteIngredientRecetteEntity(recetteId, ingredientId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
