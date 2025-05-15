package com.example.demo.repositoryMysql;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entitiesMysql.FavorisEntity;

@Repository
public interface FavorisRepository extends JpaRepository<FavorisEntity, Long> {
	List<FavorisEntity> findByUserEntityId(Long userEntityId);
	Optional<FavorisEntity> findByUserEntityIdAndRecetteEntityId(Long userEntityId, Long recetteEntityId);
	void deleteByUserEntityIdAndRecetteEntityId(Long userEntityId, Long recetteEntityId);
}
