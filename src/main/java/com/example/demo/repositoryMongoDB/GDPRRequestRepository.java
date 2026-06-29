package com.example.demo.repositoryMongoDB;

import java.time.LocalDateTime;
import java.util.List;
import com.example.demo.entiesMongodb.GDPRRequest;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
/**
 * Repository pour GDPRRequest (Demandes RGPD formelles)
 */
public interface GDPRRequestRepository extends MongoRepository<GDPRRequest, String> {
  
  List<GDPRRequest> findByUserId(Long userId);
  
  List<GDPRRequest> findByRequestStatus(String status);
  
  @Query("{ 'requestDate': { $lt: ?0 } }")
  List<GDPRRequest> findExpiredRequests(LocalDateTime date);
}
