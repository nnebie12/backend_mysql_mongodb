package com.example.demo.web.controllersMysql;


import java.util.List;
import java.util.Map; 

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

import com.example.demo.DTO.CommentaireRequestDTO;
import com.example.demo.DTO.CommentaireResponseDTO;
import com.example.demo.DTO.NoteRequestDTO;
import com.example.demo.DTO.NoteResponseDTO;
import com.example.demo.DTO.RecetteIngredientDTO;
import com.example.demo.DTO.RecetteRequestDTO;
import com.example.demo.DTO.RecetteResponseDTO;

import com.example.demo.servicesMysql.RecetteService;


@RestController
@RequestMapping("/api/v1/recettes") 
	public class RecetteController {
	
	 private final RecetteService recetteService; 
	
	 public RecetteController(RecetteService recetteService) {
	     this.recetteService = recetteService;
	 }
	
	 @GetMapping("/all")
	 public ResponseEntity<List<RecetteResponseDTO>> getAllRecettes() {
	     List<RecetteResponseDTO> recettes = recetteService.getAllRecettes();
	     return ResponseEntity.ok(recettes); 
	 }
	
	 @PostMapping("/user/{userId}")
	 public ResponseEntity<RecetteResponseDTO> createRecette(@RequestBody RecetteRequestDTO recetteDTO, @PathVariable Long userId) {
	     RecetteResponseDTO savedRecette = recetteService.saveRecette(recetteDTO, userId);
	     return new ResponseEntity<>(savedRecette, HttpStatus.CREATED);
	 }
	
	 @GetMapping("/{id}")
	 public ResponseEntity<RecetteResponseDTO> getRecetteById(@PathVariable Long id) {
	     return recetteService.getRecetteById(id)
	             .map(ResponseEntity::ok) 
	             .orElse(ResponseEntity.notFound().build()); 
	 }
	
	 @PutMapping("/{id}")
	 public ResponseEntity<RecetteResponseDTO> updateRecette(@PathVariable Long id, @RequestBody RecetteRequestDTO recetteDetailsDTO) {
	     try {
	         RecetteResponseDTO updatedRecette = recetteService.updateRecette(id, recetteDetailsDTO);
	         return ResponseEntity.ok(updatedRecette);
	     } catch (RuntimeException e) {
	         
	         return ResponseEntity.notFound().build();
	     }
	 }
	
	 @GetMapping("/{recetteId}/details")
	 public ResponseEntity<Map<String, Object>> getRecetteDetails(@PathVariable Long recetteId) {
	    
	     Map<String, Object> details = recetteService.getRecetteDetails(recetteId);
	     return ResponseEntity.ok(details);
	 }
	
	 @GetMapping("/user/{userId}")
	 public ResponseEntity<List<RecetteResponseDTO>> getRecettesByUser(@PathVariable Long userId) {
	     List<RecetteResponseDTO> recettes = recetteService.getRecettesByUser(userId);
	     return ResponseEntity.ok(recettes);
	 }
	
	 @DeleteMapping("/{id}")
	 public ResponseEntity<Void> deleteRecette(@PathVariable Long id) {
	     recetteService.deleteRecette(id);
	     return ResponseEntity.noContent().build(); 
	 }
	
	 @PostMapping("/{recetteId}/commentaires/user/{userId}")
	 public ResponseEntity<CommentaireResponseDTO> addCommentaire(@PathVariable Long recetteId,
	                                                              @PathVariable Long userId,
	                                                              @RequestBody CommentaireRequestDTO commentaireDTO) {
	     CommentaireResponseDTO savedCommentaire = recetteService.addCommentaire(recetteId, userId, commentaireDTO);
	     return new ResponseEntity<>(savedCommentaire, HttpStatus.CREATED);
	 }
	
	 @PostMapping("/{recetteId}/notes/user/{userId}")
	 public ResponseEntity<NoteResponseDTO> addNote(@PathVariable Long recetteId,
	                                                @PathVariable Long userId,
	                                                @RequestBody NoteRequestDTO noteDTO) {
	     NoteResponseDTO savedNote = recetteService.addNote(recetteId, userId, noteDTO);
	     return new ResponseEntity<>(savedNote, HttpStatus.CREATED);
	 }
	
	 @GetMapping("/{recetteId}/commentaires")
	 public ResponseEntity<List<CommentaireResponseDTO>> getCommentairesByRecette(@PathVariable Long recetteId) {
	     List<CommentaireResponseDTO> commentaires = recetteService.getCommentairesByRecette(recetteId);
	     return ResponseEntity.ok(commentaires);
	 }
	
	 @GetMapping("/{recetteId}/notes")
	 public ResponseEntity<List<NoteResponseDTO>> getNotesByRecette(@PathVariable Long recetteId) {
	     List<NoteResponseDTO> notes = recetteService.getNotesByRecette(recetteId);
	     return ResponseEntity.ok(notes);
	 }
	
	 @PostMapping("/{recetteId}/ingredients/{ingredientId}")
	 public ResponseEntity<Void> addIngredientToRecette(
	         @PathVariable Long recetteId,
	         @PathVariable Long ingredientId,
	         @RequestParam String quantite) {
	     recetteService.addIngredientToRecette(recetteId, ingredientId, quantite);
	     return new ResponseEntity<>(HttpStatus.CREATED);
	 }
	
	 @DeleteMapping("/{recetteId}/ingredients/{ingredientId}")
	 public ResponseEntity<Void> removeIngredientFromRecette(
	         @PathVariable Long recetteId,
	         @PathVariable Long ingredientId) {
	     recetteService.removeIngredientFromRecette(recetteId, ingredientId);
	     return ResponseEntity.noContent().build();
	 }
	
	 @GetMapping("/{recetteId}/ingredients")
	 public ResponseEntity<List<RecetteIngredientDTO>> getRecetteIngredients( 
	         @PathVariable Long recetteId) {
	     List<RecetteIngredientDTO> ingredients = recetteService.getRecetteIngredients(recetteId);
	     return ResponseEntity.ok(ingredients);
	 }
	
	 @GetMapping("/{recetteId}/moyenne-notes")
	 public ResponseEntity<Double> getMoyenneNotesByRecette(@PathVariable Long recetteId) {
	     Double moyenne = recetteService.getMoyenneNotesByRecette(recetteId);
	     return ResponseEntity.ok(moyenne);
	 }
}