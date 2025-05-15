package com.example.demo.web.controllersMysql;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entitiesMysql.IngredientEntity;
import com.example.demo.servicesMysql.IngredientService;


@RestController
@RequestMapping("/api/v1/IngredientEntity")
public class IngredientController {
	
	@Autowired
    IngredientService ingredientService;

    @GetMapping("/all")
    public ResponseEntity<List<IngredientEntity>> getAllIngredients() {
        List<IngredientEntity> ingredients = ingredientService.getAllIngredients();
        return new ResponseEntity<>(ingredients, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<IngredientEntity> createIngredient(@RequestBody IngredientEntity ingredientEntity) {
    	IngredientEntity savedIngredient = ingredientService.saveIngredient(ingredientEntity);
        return new ResponseEntity<>(savedIngredient, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredientEntity> getIngredientById(@PathVariable Long id) {
        return ingredientService.getIngredientById(id)
                .map(ingredient -> new ResponseEntity<>(ingredient, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/nom/{nom}")
    public ResponseEntity<IngredientEntity> getIngredientByNom(@PathVariable String nom) {
        return ingredientService.getIngredientByNom(nom)
                .map(ingredient -> new ResponseEntity<>(ingredient, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<IngredientEntity> updateIngredient(@PathVariable Long id, @RequestBody IngredientEntity ingredientDetails) {
        try {
        	IngredientEntity updatedIngredient = ingredientService.updateIngredient(id, ingredientDetails);
            return new ResponseEntity<>(updatedIngredient, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIngredient(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
