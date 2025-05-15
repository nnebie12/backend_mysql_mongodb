package com.example.demo.repositoryMysql;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.entitiesMysql.UserEntity;


public interface RecetteRepository extends JpaRepository<RecetteEntity, Long>{

    List<RecetteEntity> findByUserEntity(UserEntity userEntity);
    List<RecetteEntity> findByTitreContainingIgnoreCase(String titre);
    Optional<RecetteEntity> findByRecetteMongoId(String recetteMongoId);
    
}
