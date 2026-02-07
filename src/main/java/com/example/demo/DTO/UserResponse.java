package com.example.demo.DTO;

import com.example.demo.entitiesMysql.UserEntity;
import lombok.Data;

import java.util.List;

@Data
public class UserResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String preferenceAlimentaire;
    private List<String> ingredientsApprecies;
    private List<String> ingredientsEvites;
    private List<String> contraintesAlimentaires;
    private String role;
    
    public UserResponse(UserEntity userEntity) {
        this.id = userEntity.getId();
        this.nom = userEntity.getNom();
        this.prenom = userEntity.getPrenom();
        this.email = userEntity.getEmail();
        this.preferenceAlimentaire = userEntity.getPreferenceAlimentaire();
        this.ingredientsApprecies = userEntity.getIngredientsApprecies();
        this.ingredientsEvites = userEntity.getIngredientsEvites();
        this.contraintesAlimentaires = userEntity.getContraintesAlimentaires();
        this.role = userEntity.getRole().name();
    }
}
