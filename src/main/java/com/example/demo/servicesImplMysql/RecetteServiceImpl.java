package com.example.demo.servicesImplMysql;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional; 

import com.example.demo.DTO.CommentaireRequestDTO;
import com.example.demo.DTO.CommentaireResponseDTO;
import com.example.demo.DTO.NoteRequestDTO;
import com.example.demo.DTO.NoteResponseDTO;
import com.example.demo.DTO.RecetteIngredientDTO;
import com.example.demo.DTO.RecetteRequestDTO;
import com.example.demo.DTO.RecetteResponseDTO;

import com.example.demo.entiesMongodb.CommentaireDocument;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entiesMongodb.RecetteDetailsDocument;

import com.example.demo.entitiesMysql.IngredientEntity;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.entitiesMysql.RecetteIngredientEntity;
import com.example.demo.entitiesMysql.UserEntity;

import com.example.demo.repositoryMongoDB.RecetteDetailsRepository;
import com.example.demo.repositoryMysql.IngredientRepository;
import com.example.demo.repositoryMysql.RecetteRepository;
import com.example.demo.repositoryMysql.UserRepository;
import com.example.demo.servicesMysql.RecetteService; 

@Service
	public class RecetteServiceImpl implements RecetteService {
	
	 private final RecetteRepository recetteRepository;
	 private final UserRepository userRepository;
	 private final RecetteDetailsRepository recetteDetailsRepository;
	 private final IngredientRepository ingredientRepository;
	
	 public RecetteServiceImpl(RecetteRepository recetteRepository,
	                           UserRepository userRepository,
	                           RecetteDetailsRepository recetteDetailsRepository,
	                           IngredientRepository ingredientRepository) {
	     this.recetteRepository = recetteRepository;
	     this.userRepository = userRepository;
	     this.recetteDetailsRepository = recetteDetailsRepository;
	     this.ingredientRepository = ingredientRepository;
	 }
	
	 @Override
	 @Transactional
	 public RecetteResponseDTO saveRecette(RecetteRequestDTO recetteDTO, Long userEntityId) {
	     UserEntity userEntity = userRepository.findById(userEntityId)
	             .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userEntityId));
	
	     RecetteEntity recetteEntity = new RecetteEntity();
	     recetteEntity.setTitre(recetteDTO.getTitre());
	     recetteEntity.setDescription(recetteDTO.getDescription());
	     recetteEntity.setTempsPreparation(recetteDTO.getTempsPreparation());
	     recetteEntity.setTempsCuisson(recetteDTO.getTempsCuisson());
	     recetteEntity.setDifficulte(recetteDTO.getDifficulte());
	     recetteEntity.setUserEntity(userEntity);
	     recetteEntity.setDateCreation(LocalDateTime.now());
	
	     RecetteEntity savedRecette = recetteRepository.save(recetteEntity);
	
	     try {
	         RecetteDetailsDocument recetteDetails = new RecetteDetailsDocument();
	         recetteDetails.setRecetteId(savedRecette.getId().toString());
	         recetteDetails.setCommentaires(new ArrayList<>());
	         recetteDetails.setNotes(new ArrayList<>());
	         recetteDetails.setMoyenneNotes(0.0);
	         recetteDetails.setNombreCommentaires(0);
	         recetteDetails.setNombreNotes(0);
	
	         RecetteDetailsDocument savedDetails = recetteDetailsRepository.save(recetteDetails);
	
	         savedRecette.setRecetteMongoId(savedDetails.getId());
	         recetteRepository.save(savedRecette);
	     } catch (Exception e) {
	         System.err.println("Erreur lors de l'enregistrement dans MongoDB pour la recette " + savedRecette.getId() + ": " + e.getMessage());
	         
	     }
	
	     return convertToRecetteResponseDTO(savedRecette);
	 }
	
	 @Override
	 public List<RecetteResponseDTO> getAllRecettes() {
	     return recetteRepository.findAll().stream()
	             .map(this::convertToRecetteResponseDTO)
	             .collect(Collectors.toList());
	 }
	
	 @Override
	 public Optional<RecetteResponseDTO> getRecetteById(Long id) {
	     return recetteRepository.findById(id)
	             .map(this::convertToRecetteResponseDTO);
	 }
	
	 @Override
	 @Transactional
	 public RecetteResponseDTO updateRecette(Long id, RecetteRequestDTO recetteDetailsDTO) {
	     RecetteEntity recetteEntity = recetteRepository.findById(id)
	             .orElseThrow(() -> new RuntimeException("Recette non trouvée avec l'ID: " + id));
	
	     recetteEntity.setTitre(recetteDetailsDTO.getTitre());
	     recetteEntity.setDescription(recetteDetailsDTO.getDescription());
	     recetteEntity.setTempsPreparation(recetteDetailsDTO.getTempsPreparation());
	     recetteEntity.setTempsCuisson(recetteDetailsDTO.getTempsCuisson());
	     recetteEntity.setDifficulte(recetteDetailsDTO.getDifficulte());
	
	     RecetteEntity updatedRecette = recetteRepository.save(recetteEntity);
	     return convertToRecetteResponseDTO(updatedRecette);
	 }
	
	 @Override
	 @Transactional
	 public void deleteRecette(Long id) {
	     RecetteEntity recetteEntity = recetteRepository.findById(id)
	             .orElseThrow(() -> new RuntimeException("Recette non trouvée avec l'ID: " + id));
	
	     if (recetteEntity.getRecetteMongoId() != null) {
	         recetteDetailsRepository.deleteById(recetteEntity.getRecetteMongoId());
	     }
	
	     recetteRepository.deleteById(id);
	 }
	
	 @Override
	 public List<RecetteResponseDTO> getRecettesByUser(Long userEntityId) {
	     UserEntity userEntity = userRepository.findById(userEntityId)
	             .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userEntityId));
	     return recetteRepository.findByUserEntity(userEntity).stream()
	             .map(this::convertToRecetteResponseDTO) 
	             .collect(Collectors.toList());
	 }
	
	 @Override
	 @Transactional
	 public void addIngredientToRecette(Long recetteEntityId, Long ingredientEntityId, String quantite) {
	     RecetteEntity recetteEntity = recetteRepository.findById(recetteEntityId)
	             .orElseThrow(() -> new RuntimeException("Recette non trouvée avec l'ID: " + recetteEntityId));
	     IngredientEntity ingredientEntity = ingredientRepository.findById(ingredientEntityId)
	             .orElseThrow(() -> new RuntimeException("Ingrédient non trouvé avec l'ID: " + ingredientEntityId));
	
	     recetteEntity.addIngredientEntity(ingredientEntity, quantite);
	     recetteRepository.save(recetteEntity);
	 }
	
	 @Override
	 @Transactional
	 public void removeIngredientFromRecette(Long recetteEntityId, Long ingredientId) {
	     RecetteEntity recette = recetteRepository.findById(recetteEntityId)
	             .orElseThrow(() -> new RuntimeException("Recette non trouvée avec l'ID: " + recetteEntityId));
	
	     recette.getRecetteIngredients().removeIf(ri -> ri.getIngredientEntity().getId().equals(ingredientId));
	     recetteRepository.save(recette);
	 }
	
	 @Override
	 public List<RecetteIngredientDTO> getRecetteIngredients(Long recetteEntityId) {
	     RecetteEntity recette = recetteRepository.findById(recetteEntityId)
	             .orElseThrow(() -> new RuntimeException("Recette non trouvée avec l'ID: " + recetteEntityId));
	     return recette.getRecetteIngredients().stream()
	             .map(this::convertToRecetteIngredientDTO) 
	             .collect(Collectors.toList());
	 }
	
	 @Override
	 @Transactional
	 public CommentaireResponseDTO addCommentaire(Long recetteEntityId, Long userId, CommentaireRequestDTO commentaireDTO) {
	     RecetteEntity recetteEntity = recetteRepository.findById(recetteEntityId)
	             .orElseThrow(() -> new RuntimeException("Recette non trouvée avec l'ID: " + recetteEntityId));
	     UserEntity userEntity = userRepository.findById(userId)
	             .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
	
	     RecetteDetailsDocument details = recetteDetailsRepository.findById(recetteEntity.getRecetteMongoId())
	             .orElseThrow(() -> new RuntimeException("Détails de recette non trouvés pour la recette Mongo ID: " + recetteEntity.getRecetteMongoId()));
	
	     CommentaireDocument commentaireDocument = new CommentaireDocument();
	     commentaireDocument.setContenu(commentaireDTO.getContenu());
	     commentaireDocument.setDateCommentaire(LocalDateTime.now());
	     commentaireDocument.setUserId(userEntity.getId().toString());
	     commentaireDocument.setUserName(userEntity.getPrenom());
	
	     if (details.getCommentaires() == null) {
	         details.setCommentaires(new ArrayList<>());
	     }
	
	     details.getCommentaires().add(commentaireDocument);
	     details.setNombreCommentaires(details.getCommentaires().size());
	
	     recetteDetailsRepository.save(details);
	
	     return convertToCommentaireResponseDTO(commentaireDocument);
	 }
	
	 @Override
	 @Transactional
	 public NoteResponseDTO addNote(Long recetteEntityId, Long userId, NoteRequestDTO noteDTO) {
	     RecetteEntity recetteEntity = recetteRepository.findById(recetteEntityId)
	             .orElseThrow(() -> new RuntimeException("Recette non trouvée avec l'ID: " + recetteEntityId));
	     UserEntity user = userRepository.findById(userId)
	             .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
	
	     RecetteDetailsDocument details = recetteDetailsRepository.findById(recetteEntity.getRecetteMongoId())
	             .orElseThrow(() -> new RuntimeException("Détails de recette non trouvés pour la recette Mongo ID: " + recetteEntity.getRecetteMongoId()));
	
	     NoteDocument noteDocument = new NoteDocument();
	     noteDocument.setValeur(noteDTO.getValeur());
	     noteDocument.setUserId(user.getId().toString());
	     noteDocument.setUserName(user.getPrenom());
	     noteDocument.setRecetteId(recetteEntity.getRecetteMongoId());
	
	     if (details.getNotes() == null) {
	         details.setNotes(new ArrayList<>());
	     }
	
	     Optional<NoteDocument> existingNote = details.getNotes().stream()
	             .filter(n -> n.getUserId().equals(user.getId().toString()))
	             .findFirst();
	
	     if (existingNote.isPresent()) {
	         existingNote.get().setValeur(noteDocument.getValeur());
	     } else {
	         details.getNotes().add(noteDocument);
	     }
	
	     double moyenne = details.getNotes().stream()
	             .mapToInt(NoteDocument::getValeur)
	             .average()
	             .orElse(0.0);
	
	     details.setMoyenneNotes(moyenne);
	     details.setNombreNotes(details.getNotes().size());
	
	     recetteDetailsRepository.save(details);
	
	     return convertToNoteResponseDTO(noteDocument);
	 }
	
	 @Override
	 public List<CommentaireResponseDTO> getCommentairesByRecette(Long recetteEntityId) {
	     RecetteEntity recetteEntity = recetteRepository.findById(recetteEntityId)
	             .orElseThrow(() -> new RuntimeException("Recette non trouvée avec l'ID: " + recetteEntityId));
	
	     RecetteDetailsDocument details = recetteDetailsRepository.findById(recetteEntity.getRecetteMongoId())
	             .orElseThrow(() -> new RuntimeException("Détails de recette non trouvés pour la recette Mongo ID: " + recetteEntity.getRecetteMongoId()));
	
	     return details.getCommentaires() != null ? details.getCommentaires().stream()
	             .map(this::convertToCommentaireResponseDTO)
	             .collect(Collectors.toList()) : new ArrayList<>();
	 }
	
	 @Override
	 public List<NoteResponseDTO> getNotesByRecette(Long recetteEntityId) {
	     RecetteEntity recetteEntity = recetteRepository.findById(recetteEntityId)
	             .orElseThrow(() -> new RuntimeException("Recette non trouvée avec l'ID: " + recetteEntityId));
	
	     RecetteDetailsDocument details = recetteDetailsRepository.findById(recetteEntity.getRecetteMongoId())
	             .orElseThrow(() -> new RuntimeException("Détails de recette non trouvés pour la recette Mongo ID: " + recetteEntity.getRecetteMongoId()));
	
	     return details.getNotes() != null ? details.getNotes().stream()
	             .map(this::convertToNoteResponseDTO)
	             .collect(Collectors.toList()) : new ArrayList<>();
	 }
	
	 @Override
	 public Double getMoyenneNotesByRecette(Long recetteEntityId) {
	     RecetteEntity recetteEntity = recetteRepository.findById(recetteEntityId)
	             .orElseThrow(() -> new RuntimeException("Recette non trouvée avec l'ID: " + recetteEntityId));
	
	     RecetteDetailsDocument details = recetteDetailsRepository.findById(recetteEntity.getRecetteMongoId())
	             .orElseThrow(() -> new RuntimeException("Détails de recette non trouvés pour la recette Mongo ID: " + recetteEntity.getRecetteMongoId()));
	
	     return details.getMoyenneNotes();
	 }
	
	 @Override
	 public Map<String, Object> getRecetteDetails(Long recetteEntityId) {
	     RecetteEntity recette = recetteRepository.findById(recetteEntityId)
	             .orElseThrow(() -> new RuntimeException("Recette non trouvée avec l'ID: " + recetteEntityId));
	
	     RecetteDetailsDocument details = recetteDetailsRepository.findById(recette.getRecetteMongoId())
	             .orElseThrow(() -> new RuntimeException("Détails de recette non trouvés pour la recette Mongo ID: " + recette.getRecetteMongoId()));
	
	     Map<String, Object> response = new HashMap<>();
	     response.put("recette", convertToRecetteResponseDTO(recette));
	     response.put("commentaires", details.getCommentaires() != null ? details.getCommentaires().stream()
	             .map(this::convertToCommentaireResponseDTO).collect(Collectors.toList()) : new ArrayList<>());
	     response.put("notes", details.getNotes() != null ? details.getNotes().stream()
	             .map(this::convertToNoteResponseDTO).collect(Collectors.toList()) : new ArrayList<>());
	     response.put("moyenneNotes", details.getMoyenneNotes());
	     response.put("nombreCommentaires", details.getNombreCommentaires());
	     response.put("nombreNotes", details.getNombreNotes());
	
	     return response;
	 }
	
	
	 private CommentaireResponseDTO convertToCommentaireResponseDTO(CommentaireDocument document) {
	     CommentaireResponseDTO dto = new CommentaireResponseDTO();
	     dto.setId(document.getId());
	     dto.setContenu(document.getContenu());
	     dto.setDateCommentaire(document.getDateCommentaire());
	     dto.setUserId(document.getUserId());
	     dto.setUserName(document.getUserName());
	     return dto;
	 }
	
	 private NoteResponseDTO convertToNoteResponseDTO(NoteDocument document) {
	     NoteResponseDTO dto = new NoteResponseDTO();
	     dto.setId(document.getId());
	     dto.setValeur(document.getValeur());
	     dto.setUserId(document.getUserId());
	     dto.setRecetteId(document.getRecetteId());
	     dto.setUserName(document.getUserName());
	     return dto;
	 }
	
	 private RecetteResponseDTO convertToRecetteResponseDTO(RecetteEntity recetteEntity) {
	     RecetteResponseDTO dto = new RecetteResponseDTO();
	     dto.setId(recetteEntity.getId());
	     dto.setTitre(recetteEntity.getTitre());
	     dto.setDescription(recetteEntity.getDescription());
	     dto.setTempsPreparation(recetteEntity.getTempsPreparation());
	     dto.setTempsCuisson(recetteEntity.getTempsCuisson());
	     dto.setDifficulte(recetteEntity.getDifficulte());
	     dto.setDateCreation(recetteEntity.getDateCreation());
	     dto.setRecetteMongoId(recetteEntity.getRecetteMongoId());
	
	     if (recetteEntity.getUserEntity() != null) {
	         dto.setUserId(recetteEntity.getUserEntity().getId());
	         dto.setUserName(recetteEntity.getUserEntity().getPrenom());
	     }
	
	     if (recetteEntity.getRecetteIngredients() != null && !recetteEntity.getRecetteIngredients().isEmpty()) {
	         dto.setIngredients(recetteEntity.getRecetteIngredients().stream()
	             .map(this::convertToRecetteIngredientDTO)
	             .collect(Collectors.toList()));
	     }
	
	     if (recetteEntity.getRecetteMongoId() != null) {
	         recetteDetailsRepository.findById(recetteEntity.getRecetteMongoId()).ifPresent(details -> {
	             
	             dto.setMoyenneNotes(details.getMoyenneNotes());
	             dto.setNombreCommentaires(details.getNombreCommentaires());
	             dto.setNombreNotes(details.getNombreNotes());
	         });
	     }
	     return dto;
	 }
	
	 private RecetteIngredientDTO convertToRecetteIngredientDTO(RecetteIngredientEntity recetteIngredientEntity) {
	     RecetteIngredientDTO dto = new RecetteIngredientDTO();
	     dto.setQuantite(recetteIngredientEntity.getQuantite());
	     if (recetteIngredientEntity.getIngredientEntity() != null) {
	        
	         dto.setIngredientName(recetteIngredientEntity.getIngredientEntity().getNom());
	     }
	     return dto;
	 }
}