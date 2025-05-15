package com.example.demo.entiesMongodb;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Data;

@Data
@Document
public class HistoriqueRecherche {
    @Id
    private String id;
    
    private Long userId;
    
    private String terme;
    
    private LocalDateTime dateRecherche;
    
    private List<Filtre> filtres = new ArrayList<>();
    
    
    public static class Filtre {
        private String nom;
        private String valeur;

        public String getNom() {
            return nom;
        }

        public void setNom(String nom) {
            this.nom = nom;
        }

        public String getValeur() {
            return valeur;
        }

        public void setValeur(String valeur) {
            this.valeur = valeur;
        }
    }
}