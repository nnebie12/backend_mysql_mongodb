package com.example.demo.repositoryMongoDB;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.entiesMongodb.NoteDocument;

public interface NoteMongoRepository extends MongoRepository<NoteDocument, String> {
    
    List<NoteDocument> findByUserId(String userId);
    
    Optional<NoteDocument> findByUserIdAndRecetteId(String userId, String recetteId);
}
