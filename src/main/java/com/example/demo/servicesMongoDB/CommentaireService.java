package com.example.demo.servicesMongoDB;

import java.util.List;
import java.util.Optional;

import com.example.demo.DTO.CommentaireRequestDTO;
import com.example.demo.DTO.CommentaireResponseDTO;


public interface CommentaireService {
    List<CommentaireResponseDTO> getAllCommentaires(); 
    CommentaireResponseDTO addCommentaire(CommentaireRequestDTO commentaireDto); 
    Optional<CommentaireResponseDTO> getCommentaireById(String id);
    CommentaireResponseDTO updateCommentaire(String id, CommentaireRequestDTO commentaireDto);
    void deleteCommentaire(String id);
}
