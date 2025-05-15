package com.example.demo.entitiesMysql;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
	 @Column(nullable = false)
	    private String nom;
	 
    @Column(nullable = false)
    private String prenom;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String motDePasse;
    
    @Column
    private String preferenceAlimentaire;
    
    @Column(nullable = false)
    private String role;
    
    @OneToMany(mappedBy = "userEntity", cascade = CascadeType.ALL)
    private List<RecetteEntity> recettes;

	
}
