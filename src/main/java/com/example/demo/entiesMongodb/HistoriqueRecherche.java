package com.example.demo.entiesMongodb;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "historique_recherche")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HistoriqueRecherche {
    
    @Id
    private String id;
    
    @Indexed
    private Long userId;
    
    @Indexed
    private String terme;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateRecherche;
    
    private List<Filtre> filtres = new ArrayList<>();
    
    // Nouvelles propriétés pour enrichir l'analyse comportementale
    private Integer nombreResultats;
    private Boolean rechercheFructueuse;
    private String contexteRecherche; // "navigation", "recommendation", "direct"
    private Long dureeRecherche; // en millisecondes
    private String sourceRecherche; // "web", "mobile", "api"
    
    // Métadonnées pour l'analyse
    private String categorieRecherche; // "ingredient", "recette", "technique", etc.
    private Integer positionCliquee; // position du résultat cliqué (si applicable)
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Filtre {
        private String nom;
        private String valeur;
        private String type; // "text", "range", "boolean", "select"
        
        // Constructeurs pour compatibilité avec l'ancien code
        public Filtre(String nom, String valeur) {
            this.nom = nom;
            this.valeur = valeur;
            this.type = "text";
        }
    }
    
    // Méthodes utilitaires
    public boolean estRechercheFructueuse() {
        return Boolean.TRUE.equals(rechercheFructueuse) || 
               (nombreResultats != null && nombreResultats > 0);
    }
    
    public boolean estRechercheRecente() {
        return dateRecherche != null && 
               dateRecherche.isAfter(LocalDateTime.now().minusHours(24));
    }
}