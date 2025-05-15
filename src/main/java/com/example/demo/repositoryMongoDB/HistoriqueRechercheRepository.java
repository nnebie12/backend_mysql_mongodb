package com.example.demo.repositoryMongoDB;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entiesMongodb.HistoriqueRecherche;

@Repository
public interface HistoriqueRechercheRepository extends MongoRepository<HistoriqueRecherche, String> {
    List<HistoriqueRecherche> findByUserIdOrderByDateRechercheDesc(Long userId);
    List<HistoriqueRecherche> findByTermeContaining(String terme);
    List<HistoriqueRecherche> findByUserId(Long userId);
}
