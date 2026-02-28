package com.example.demo.repositoryMongoDB;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.entiesMongodb.CommentaireDocument;

public interface CommentaireMongoRepository extends MongoRepository<CommentaireDocument, String> {
    
    List<CommentaireDocument> findByUserId(String userId);

	List<CommentaireDocument> findByRecetteId(Long recetteId);
}