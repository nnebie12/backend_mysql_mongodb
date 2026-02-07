package com.example.demo.entitiesMysql;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;

import com.example.demo.entitiesMysql.ennums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "mot_de_passe", nullable = false, length = 255)
    private String motDePasse;

    @Column(name = "preference_alimentaire", length = 500)
    private String preferenceAlimentaire;
    
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @OneToMany(mappedBy = "userEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore 
    private List<RecetteEntity> recettes;

}