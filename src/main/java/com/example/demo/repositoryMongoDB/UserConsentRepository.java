package com.example.demo.repositoryMongoDB;

import com.example.demo.entiesMongodb.UserConsent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour UserConsent (Consentements RGPD)
 */
@Repository
public interface UserConsentRepository extends MongoRepository<UserConsent, String> {
  
  Optional<UserConsent> findByUserId(Long userId);
  
  void deleteByUserId(Long userId);
}

