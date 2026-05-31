package com.example.demo.DTO;

import java.util.List;

import com.example.demo.entitiesMysql.UserEntity;

import lombok.Data;


@Data
public class UserAdminResponseDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private Boolean actif;
    private List<String> preferenceAlimentaire;
    private List<String> contraintesAlimentaires;
    private String niveauCuisine;
    private Boolean newsletter;
    private Integer recettesCount;

    
    public UserAdminResponseDTO(UserEntity user) {
        this.id = user.getId();
        this.nom = user.getNom();
        this.prenom = user.getPrenom();
        this.email = user.getEmail();
        this.role = user.getRole() != null ? user.getRole().name() : "USER";
        this.actif = user.getActif();
        this.preferenceAlimentaire = user.getPreferenceAlimentaire();
        this.contraintesAlimentaires = user.getContraintesAlimentaires();
        this.niveauCuisine = user.getNiveauCuisine();
        this.newsletter = user.getNewsletter();
        // Calcul sécurisé : évite NullPointerException si la liste n'est pas chargée
        this.recettesCount = (user.getRecettes() != null) ? user.getRecettes().size() : 0;
    }
}