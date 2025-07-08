package com.example.demo.servicesMysql;


import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.demo.DTO.CommentaireRequestDTO;
import com.example.demo.DTO.CommentaireResponseDTO;
import com.example.demo.DTO.NoteRequestDTO;
import com.example.demo.DTO.NoteResponseDTO;
import com.example.demo.DTO.RecetteIngredientDTO;
import com.example.demo.DTO.RecetteRequestDTO;
import com.example.demo.DTO.RecetteResponseDTO;

	public interface RecetteService {
	
	 RecetteResponseDTO saveRecette(RecetteRequestDTO recetteDTO, Long userId);
	 Optional<RecetteResponseDTO> getRecetteById(Long id);
	
	 RecetteResponseDTO updateRecette(Long id, RecetteRequestDTO recetteDetails); 
	
	 List<RecetteResponseDTO> getRecettesByUser(Long userId);
	
	 List<RecetteResponseDTO> getAllRecettes();
	 void deleteRecette(Long id);
	
	 CommentaireResponseDTO addCommentaire(Long recetteId, Long userId, CommentaireRequestDTO commentaireDTO);
	 NoteResponseDTO addNote(Long recetteId, Long userId, NoteRequestDTO noteDTO);
	
	 List<CommentaireResponseDTO> getCommentairesByRecette(Long recetteId);
	 List<NoteResponseDTO> getNotesByRecette(Long recetteId);
	 Double getMoyenneNotesByRecette(Long recetteId);
	
	 Map<String, Object> getRecetteDetails(Long recetteId);
	
	 void addIngredientToRecette(Long recetteId, Long ingredientId, String quantite);
	 void removeIngredientFromRecette(Long recetteId, Long ingredientId);
	
	 List<RecetteIngredientDTO> getRecetteIngredients(Long recetteId);
}