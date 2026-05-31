package com.example.demo.entitiesMysql;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.entitiesMysql.ennums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    public UserEntity(Long id, String nom, String prenom, String email, String motDePasse,
                      List<String> preferenceAlimentaire, List<String> ingredientsApprecies,
                      List<String> ingredientsEvites, List<String> contraintesAlimentaires,
                      String niveauCuisine, Boolean newsletter, Role role, List<RecetteEntity> recettes) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.preferenceAlimentaire = preferenceAlimentaire;
        this.ingredientsApprecies = ingredientsApprecies;
        this.ingredientsEvites = ingredientsEvites;
        this.contraintesAlimentaires = contraintesAlimentaires;
        this.niveauCuisine = niveauCuisine;
        this.newsletter = newsletter;
        this.actif = true;
        this.role = role;
        this.recettes = recettes;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    // Colonne canonique pour le hash du mot de passe.
    // Les colonnes DB "password" et "motdepasse" sont des artefacts legacy ;
    // exécuter la migration : ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NULL;
    //                         ALTER TABLE users MODIFY COLUMN motdepasse VARCHAR(255) NULL;
    // puis les supprimer une fois le déploiement stabilisé.
    @Column(name = "mot_de_passe", nullable = false, length = 255)
    private String motDePasse;

    @ElementCollection
    @Column(name = "preference_alimentaire", length = 500)
    private List<String> preferenceAlimentaire = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "user_ingredients_apprecies", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "ingredient")
    private List<String> ingredientsApprecies = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "user_ingredients_evites", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "ingredient")
    private List<String> ingredientsEvites = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "user_contraintes_alimentaires", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "contrainte")
    private List<String> contraintesAlimentaires = new ArrayList<>();
    
    @Column(name = "niveau_cuisine", length = 50)
    private String niveauCuisine;

    @Column(name = "newsletter", nullable = false)
    private Boolean newsletter = false;

    @Column(name = "actif", nullable = false)
    private Boolean actif = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @OneToMany(mappedBy = "userEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore 
    private List<RecetteEntity> recettes;

}