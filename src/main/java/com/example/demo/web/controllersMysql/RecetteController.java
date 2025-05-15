package com.example.demo.web.controllersMysql;

import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entiesMongodb.CommentaireDocument;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.entitiesMysql.RecetteIngredientEntity;
import com.example.demo.servicesMysql.RecetteService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/RecetteEntity")
public class RecetteController {

	@Autowired
    RecetteService recetteService;

    @GetMapping("/test")
    @Operation(summary = "Tester l'API", description = "Retourne un message de test")
    public String test() {
        return "Api Functional ok";
    }

    @GetMapping("/all")
    public ResponseEntity<List<RecetteEntity>> getAllRecettes() {
        List<RecetteEntity> recettes = recetteService.getAllRecettes();
        return new ResponseEntity<>(recettes, HttpStatus.OK);
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<RecetteEntity> createRecette(@RequestBody RecetteEntity recetteEntity, @PathVariable Long userId) {
    	RecetteEntity savedRecette = recetteService.saveRecette(recetteEntity, userId);
        return new ResponseEntity<>(savedRecette, HttpStatus.CREATED);
    }
    
    
    @GetMapping("/{id}")
    public ResponseEntity<RecetteEntity> getRecetteById(@PathVariable Long id) {
        return recetteService.getRecetteById(id)
                .map(recette -> new ResponseEntity<>(recette, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecetteEntity>> getRecettesByUser(@PathVariable Long userId) {
        List<RecetteEntity> recettes = recetteService.getRecettesByUser(userId);
        return new ResponseEntity<>(recettes, HttpStatus.OK);
    }
    
    
    @PutMapping("/{id}")
    public ResponseEntity<RecetteEntity> updateRecette(@PathVariable Long id, @RequestBody RecetteEntity recetteDetails) {
        try {
        	RecetteEntity updatedRecette = recetteService.updateRecette(id, recetteDetails);
            return new ResponseEntity<>(updatedRecette, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecette(@PathVariable Long id) {
        recetteService.deleteRecette(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
        
    @PostMapping("/{recetteId}/commentaires/user/{userId}")
    public ResponseEntity<CommentaireDocument> addCommentaire(@PathVariable Long recetteId, 
                                                             @PathVariable Long userId, 
                                                             @RequestBody CommentaireDocument commentaire) {
        CommentaireDocument savedCommentaire = recetteService.addCommentaire(recetteId, userId, commentaire);
        return new ResponseEntity<>(savedCommentaire, HttpStatus.CREATED);
    }
    
    @PostMapping("/{recetteId}/notes/user/{userId}")
    public ResponseEntity<NoteDocument> addNote(@PathVariable Long recetteId, 
                                              @PathVariable Long userId, 
                                              @RequestBody NoteDocument note) {
        NoteDocument savedNote = recetteService.addNote(recetteId, userId, note);
        return new ResponseEntity<>(savedNote, HttpStatus.CREATED);
    }
    
    @PostMapping("/{recetteId}/ingredients/{ingredientId}")
    public ResponseEntity<Void> addIngredientToRecette(
            @PathVariable Long recetteEntityId,
            @PathVariable Long ingredientEntityId,
            @RequestParam String quantite) {  
        recetteService.addIngredientToRecette(recetteEntityId, ingredientEntityId, quantite);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/{recetteId}/ingredients/{ingredientId}")
    public ResponseEntity<Void> removeIngredientFromRecette(
            @PathVariable Long recetteEntityId,
            @PathVariable Long ingredientEntityId) {
        recetteService.removeIngredientFromRecette(recetteEntityId, ingredientEntityId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{recetteId}/ingredients")
    public ResponseEntity<List<RecetteIngredientEntity>> getRecetteIngredients(
            @PathVariable Long recetteEntityId) {
        List<RecetteIngredientEntity> ingredients = recetteService.getRecetteIngredients(recetteEntityId);
        return new ResponseEntity<>(ingredients, HttpStatus.OK);
    }
    
    @GetMapping("/{recetteId}/commentaires")
    public ResponseEntity<List<CommentaireDocument>> getCommentairesByRecette(@PathVariable Long recetteId) {
        List<CommentaireDocument> commentaires = recetteService.getCommentairesByRecette(recetteId);
        return new ResponseEntity<>(commentaires, HttpStatus.OK);
    }
    
    @GetMapping("/{recetteId}/notes")
    public ResponseEntity<List<NoteDocument>> getNotesByRecette(@PathVariable Long recetteId) {
        List<NoteDocument> notes = recetteService.getNotesByRecette(recetteId);
        return new ResponseEntity<>(notes, HttpStatus.OK);
    }
    
    @GetMapping("/{recetteId}/moyenne-notes")
    public ResponseEntity<Double> getMoyenneNotesByRecette(@PathVariable Long recetteId) {
        Double moyenne = recetteService.getMoyenneNotesByRecette(recetteId);
        return new ResponseEntity<>(moyenne, HttpStatus.OK);
    }
    
    @GetMapping("/{recetteId}/details")
    public ResponseEntity<Map<String, Object>> getRecetteDetails(@PathVariable Long recetteId) {
        Map<String, Object> details = recetteService.getRecetteDetails(recetteId);
        return new ResponseEntity<>(details, HttpStatus.OK);
    }
}
