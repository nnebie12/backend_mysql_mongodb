package com.example.demo.servicesMysql;

import java.util.List;

import com.example.demo.entitiesMysql.FavorisEntity;

public interface FavorisService {
	
    FavorisEntity addFavori(Long userEntityId, Long recetteEntityId);
    List<FavorisEntity> getFavorisByUserId(Long userEntityId);
    void deleteFavori(Long userEntityId, Long recetteEntityId);
    boolean existsFavori(Long userEntityId, Long recetteEntityId);
}