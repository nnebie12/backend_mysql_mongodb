package com.example.demo.servicesMongoDB;

import java.util.List;
import java.util.Optional;

import com.example.demo.entiesMongodb.CommentaireDocument;


public interface CommentaireService {

	List<CommentaireDocument> getAllCommentaireEntity();
	CommentaireDocument addCommentaireEntity(CommentaireDocument commentaireEntity);
    Optional<CommentaireDocument> getCommentaireById(Long id);
    CommentaireDocument updateCommentaire(Long id, CommentaireDocument commentaireEntity);
    void deleteCommentaire(Long id);
}
