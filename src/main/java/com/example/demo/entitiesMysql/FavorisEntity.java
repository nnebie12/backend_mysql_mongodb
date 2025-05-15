package com.example.demo.entitiesMysql;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "favoris")
public class FavorisEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;
    
    @ManyToOne
    @JoinColumn(name = "recette_id", nullable = false)
    private RecetteEntity recetteEntity;
    
    @Column(name = "date_ajout")
    private LocalDateTime dateAjout;
}
