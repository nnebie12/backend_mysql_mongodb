package com.example.demo.repositoryMongoDB;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.example.demo.entiesMongodb.UserAction;

/**
 * Repository pour UserAction (Historique anonymisé)
 * Les documents s'auto-suppriment après 180 jours via TTL
 */
public interface UserActionRepository extends MongoRepository<UserAction, String> {
  
  List<UserAction> findByUserId(Long userId);
  
  @Query("{ 'userId': ?0, 'actionDate': { $gte: ?1 } }")
  List<UserAction> findUserActionsAfter(Long userId, LocalDateTime date);
}
