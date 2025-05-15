package com.example.demo.repositoryMongoDB;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entiesMongodb.RecommandationIA;

@Repository
public interface RecommandationIARepository extends MongoRepository<RecommandationIA, String> {
    List<RecommandationIA> findByUserId(Long userId);
    List<RecommandationIA> findByUserIdAndType(Long userId, String type);
    List<RecommandationIA> findByUserIdAndEstUtiliseTrue(Long userId);
}