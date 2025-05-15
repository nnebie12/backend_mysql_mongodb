package com.example.demo.repositoryMongoDB;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.entiesMongodb.RecetteDetailsDocument;

public interface RecetteDetailsRepository extends MongoRepository<RecetteDetailsDocument, String> {
	
	Optional<RecetteDetailsDocument> findByRecetteId(String recetteId);
}
