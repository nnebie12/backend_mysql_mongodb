package com.example.demo.web.mapper;

import com.example.demo.DTO.RecetteRequestDTO;
import com.example.demo.DTO.RecetteResponseDTO;
import com.example.demo.DTO.RecetteIngredientDTO;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.entitiesMysql.RecetteIngredientEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class RecetteMapper {

    public RecetteResponseDTO toResponseDto(RecetteEntity entity) {
        if (entity == null) {
            return null;
        }

        RecetteResponseDTO dto = new RecetteResponseDTO();
        dto.setId(entity.getId());
        dto.setTitre(entity.getTitre());
        dto.setDescription(entity.getDescription());
        dto.setTempsPreparation(entity.getTempsPreparation());
        dto.setTempsCuisson(entity.getTempsCuisson());
        dto.setDifficulte(entity.getDifficulte());
        dto.setCuisine(entity.getCuisine());
        dto.setTypeRecette(entity.getTypeRecette());
        dto.setVegetarien(entity.getVegetarien());
        dto.setCategorie(entity.getCategorie());
        dto.setSaison(entity.getSaison());
        dto.setTypeCuisine(entity.getTypeCuisine());
        dto.setImageUrl(entity.getImageUrl());
        dto.setPopularite(entity.getPopularite());
        dto.setDateCreation(entity.getDateCreation());
        dto.setRecetteMongoId(entity.getRecetteMongoId());

        if (entity.getUserEntity() != null) {
            dto.setUserId(entity.getUserEntity().getId());
            dto.setUserName(entity.getUserEntity().getPrenom());
        }

        if (entity.getRecetteIngredients() != null && !entity.getRecetteIngredients().isEmpty()) {
            dto.setIngredients(entity.getRecetteIngredients().stream()
                    .filter(Objects::nonNull)
                    .map(this::toRecetteIngredientDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public List<RecetteResponseDTO> toResponseDtoList(List<RecetteEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .filter(Objects::nonNull)
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public void mapRequestDtoToEntity(RecetteRequestDTO dto, RecetteEntity entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setTitre(dto.getTitre());
        entity.setDescription(dto.getDescription());
        entity.setTempsPreparation(dto.getTempsPreparation());
        entity.setTempsCuisson(dto.getTempsCuisson());
        entity.setDifficulte(dto.getDifficulte());
        entity.setCuisine(dto.getCuisine());
        entity.setTypeRecette(dto.getTypeRecette());
        entity.setVegetarien(dto.getVegetarien());
        entity.setCategorie(dto.getCategorie());
        entity.setSaison(dto.getSaison());
        entity.setTypeCuisine(dto.getTypeCuisine());

        if (dto.getImageUrl() != null) {
            entity.setImageUrl(dto.getImageUrl());
        }
    }

    private RecetteIngredientDTO toRecetteIngredientDto(RecetteIngredientEntity entity) {
        RecetteIngredientDTO dto = new RecetteIngredientDTO();
        dto.setQuantite(entity.getQuantite());
        dto.setUniteMesure(entity.getUniteMesure());
        if (entity.getIngredientEntity() != null) {
            dto.setIngredientName(entity.getIngredientEntity().getNom());
        }
        return dto;
    }
}
