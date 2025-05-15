package com.example.demo.servicesImplMysql;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.entitiesMysql.FavorisEntity;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.repositoryMysql.FavorisRepository;
import com.example.demo.repositoryMysql.RecetteRepository;
import com.example.demo.repositoryMysql.UserRepository;
import com.example.demo.servicesMysql.FavorisService;

@Service
public class FavorisServiceImpl implements FavorisService {
    private final FavorisRepository favorisRepository;
    private final UserRepository userRepository;
    private final RecetteRepository recetteRepository;
    
    public FavorisServiceImpl(FavorisRepository favorisRepository, UserRepository userRepository, RecetteRepository recetteRepository) {
        this.favorisRepository = favorisRepository;
        this.userRepository = userRepository;
        this.recetteRepository = recetteRepository;
    }
    
    @Override
    public FavorisEntity addFavori(Long userEntityId, Long recetteEntityId) {
        Optional<UserEntity> userOpt = userRepository.findById(userEntityId);
        if (userOpt.isEmpty()) {
            return null; 
        }
        
        Optional<RecetteEntity> recetteOpt = recetteRepository.findById(recetteEntityId);
        if (recetteOpt.isEmpty()) {
            return null; 
        }

        if (existsFavori(userEntityId, recetteEntityId)) {
            return null; 
        }

        FavorisEntity favorisEntity = new FavorisEntity();
        favorisEntity.setUserEntity(userOpt.get());
        favorisEntity.setRecetteEntity(recetteOpt.get());
        favorisEntity.setDateAjout(LocalDateTime.now());

        return favorisRepository.save(favorisEntity);
    }
    
    @Override
    public List<FavorisEntity> getFavorisByUserId(Long userEntityId) {
        return favorisRepository.findByUserEntityId(userEntityId);
    }
    
    @Override
    public void deleteFavori(Long userEntityId, Long recetteEntityId) {
        favorisRepository.deleteByUserEntityIdAndRecetteEntityId(userEntityId, recetteEntityId);
    }
    
    @Override
    public boolean existsFavori(Long userEntityId, Long recetteEntityId) {
        return favorisRepository.findByUserEntityIdAndRecetteEntityId(userEntityId, recetteEntityId).isPresent();
    }
}
