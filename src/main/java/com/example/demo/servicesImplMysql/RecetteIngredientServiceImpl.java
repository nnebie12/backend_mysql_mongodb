package com.example.demo.servicesImplMysql;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.entitiesMysql.IngredientEntity;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.entitiesMysql.RecetteIngredientEntity;
import com.example.demo.repositoryMysql.IngredientRepository;
import com.example.demo.repositoryMysql.RecetteIngredientRepository;
import com.example.demo.repositoryMysql.RecetteRepository;
import com.example.demo.servicesMysql.RecetteIngredientService;

@Service
public class RecetteIngredientServiceImpl implements RecetteIngredientService {
    private final RecetteIngredientRepository recetteIngredientRepository;
    private final RecetteRepository recetteRepository;
    private final IngredientRepository ingredientRepository;
    
    public RecetteIngredientServiceImpl(RecetteIngredientRepository recetteIngredientRepository,
                                       RecetteRepository recetteRepository,
                                       IngredientRepository ingredientRepository) {
        this.recetteIngredientRepository = recetteIngredientRepository;
        this.recetteRepository = recetteRepository;
        this.ingredientRepository = ingredientRepository;
    }
    
    @Override
    public RecetteIngredientEntity addIngredientRecetteEntity(Long recetteEntityId, Long ingredientEntityId,
    		String quantite, String instruction) {
    	Optional<RecetteEntity> recetteOpt = recetteRepository.findById(recetteEntityId);
    	if (recetteOpt.isEmpty()) {
    	    return null;
    	}

    	Optional<IngredientEntity> ingredientOpt = ingredientRepository.findById(ingredientEntityId);
    	if (ingredientOpt.isEmpty()) {
    	    return null;
    	}


        if (existsRecetteIngredient(recetteEntityId, ingredientEntityId)) {
            return null; 
        }
            
        RecetteIngredientEntity recetteIngredientEntity = new RecetteIngredientEntity();
        recetteIngredientEntity.setRecetteEntity(recetteOpt.get());
        recetteIngredientEntity.setIngredientEntity(ingredientOpt.get());
        recetteIngredientEntity.setQuantite(quantite);
        recetteIngredientEntity.setInstruction(instruction);
        
        return recetteIngredientRepository.save(recetteIngredientEntity);
    }
    
    @Override
    public List<RecetteIngredientEntity> getIngredientsByRecetteEntityId(Long recetteEntityId) {
        return recetteIngredientRepository.findByRecetteEntityId(recetteEntityId);
    }
    
    @Override
    public void deleteRecetteIngredient(Long recetteEntityId, Long ingredientEntityId) {
    	recetteIngredientRepository.deleteByRecetteEntityIdAndIngredientEntityId(recetteEntityId, ingredientEntityId);
        
    }
    
    @Override
    public void deleteAllIngredientsRecetteEntity(Long recetteEntityId) {
        recetteIngredientRepository.deleteByRecetteEntityId(recetteEntityId);
    }

    @Override
    public void deleteIngredientRecetteEntity(Long recetteIngredientEntityId) {
    	recetteIngredientRepository.deleteById(recetteIngredientEntityId);
    }


    @Override
    public void deleteRecetteEntity(Long recetteEntityId) {
    	recetteRepository.deleteById(recetteEntityId);
    }


	@Override
	public boolean existsRecetteIngredient(Long ingredientEntityId, Long recetteEntityId) {
		return recetteIngredientRepository.findByRecetteEntityIdAndIngredientEntityId(recetteEntityId, ingredientEntityId).isPresent();
	}

}
