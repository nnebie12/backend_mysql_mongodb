package com.example.demo.servicesImplMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.DTO.CommentaireRequestDTO;
import com.example.demo.DTO.CommentaireResponseDTO;
import com.example.demo.entiesMongodb.CommentaireDocument;
import com.example.demo.repositoryMongoDB.CommentaireMongoRepository;
import com.example.demo.servicesMongoDB.CommentaireService;

@Service
public class CommentaireServiceImpl implements CommentaireService {

    private final CommentaireMongoRepository commentaireRepository;

    public CommentaireServiceImpl(CommentaireMongoRepository commentaireRepository) {
        this.commentaireRepository = commentaireRepository;
    }

    private CommentaireResponseDTO convertToResponseDTO(CommentaireDocument document) {
        return new CommentaireResponseDTO(
            document.getId(),
            document.getContenu(),
            document.getDateCommentaire(),
            document.getUserId(),
            document.getUserName()
        );
    }

    @Override
    public List<CommentaireResponseDTO> getAllCommentaires() {
        return commentaireRepository.findAll().stream()
                .map(this::convertToResponseDTO) 
                .collect(Collectors.toList());
    }

    @Override
    public CommentaireResponseDTO addCommentaire(CommentaireRequestDTO commentaireDto) {
        CommentaireDocument commentaireDocument = new CommentaireDocument();
        commentaireDocument.setContenu(commentaireDto.getContenu());
        commentaireDocument.setUserId(commentaireDto.getUserId());
        commentaireDocument.setUserName(commentaireDto.getUserName());
        commentaireDocument.setDateCommentaire(LocalDateTime.now()); 
        
        if (commentaireDocument.getUserId() == null) {
            commentaireDocument.setUserId("defaultUserId");
        }
        if (commentaireDocument.getUserName() == null) {
            commentaireDocument.setUserName("Anonymous");
        }

        CommentaireDocument savedDocument = commentaireRepository.save(commentaireDocument);
        return convertToResponseDTO(savedDocument); 
    }

    @Override
    public Optional<CommentaireResponseDTO> getCommentaireById(String id) {
        return commentaireRepository.findById(id).map(this::convertToResponseDTO);
    }

    @Override
    public CommentaireResponseDTO updateCommentaire(String id, CommentaireRequestDTO commentaireDto) {
        Optional<CommentaireDocument> existingCommentaire = commentaireRepository.findById(id);

        if (existingCommentaire.isPresent()) {
            CommentaireDocument updatedCommentaire = existingCommentaire.get();
            updatedCommentaire.setContenu(commentaireDto.getContenu());

            CommentaireDocument savedDocument = commentaireRepository.save(updatedCommentaire);
            return convertToResponseDTO(savedDocument);
        } else {
            throw new RuntimeException("Commentaire not found with id: " + id);
        }
    }

    @Override
    public void deleteCommentaire(String id) {
        commentaireRepository.deleteById(id);
    }
}