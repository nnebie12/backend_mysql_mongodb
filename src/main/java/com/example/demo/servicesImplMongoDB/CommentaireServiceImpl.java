package com.example.demo.servicesImplMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.entiesMongodb.CommentaireDocument;
import com.example.demo.repositoryMongoDB.CommentaireMongoRepository;
import com.example.demo.servicesMongoDB.CommentaireService;

@Service
public class CommentaireServiceImpl implements CommentaireService {

    private final CommentaireMongoRepository commentaireRepository;

    public CommentaireServiceImpl(CommentaireMongoRepository commentaireRepository) {
        this.commentaireRepository = commentaireRepository;
    }

    @Override
    public List<CommentaireDocument> getAllCommentaireEntity() {
        return commentaireRepository.findAll();
    }

    @Override
    public CommentaireDocument addCommentaireEntity(CommentaireDocument commentaireEntity) {
        if (commentaireEntity.getDateCommentaire() == null) {
            commentaireEntity.setDateCommentaire(LocalDateTime.now());
        }

        if (commentaireEntity.getUserId() == null) {
            commentaireEntity.setUserId("defaultUserId");
        }

        if (commentaireEntity.getUserName() == null) {
            commentaireEntity.setUserName("Anonymous");
        }

        return commentaireRepository.save(commentaireEntity);
    }


    @Override
    public Optional<CommentaireDocument> getCommentaireById(Long id) {
        return commentaireRepository.findById(id.toString());
    }

    @Override
    public CommentaireDocument updateCommentaire(Long id, CommentaireDocument commentaireEntity) {
        Optional<CommentaireDocument> existingCommentaire = commentaireRepository.findById(id.toString());
        
        if (existingCommentaire.isPresent()) {
            CommentaireDocument updatedCommentaire = existingCommentaire.get();
            updatedCommentaire.setContenu(commentaireEntity.getContenu());
            updatedCommentaire.setDateCommentaire(commentaireEntity.getDateCommentaire());
            
            return commentaireRepository.save(updatedCommentaire);
        } else {
            throw new RuntimeException("Commentaire not found with id: " + id);
        }
    }

    @Override
    public void deleteCommentaire(Long id) {
        commentaireRepository.deleteById(id.toString());
    }
}