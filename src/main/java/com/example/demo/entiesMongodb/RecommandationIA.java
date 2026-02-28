package com.example.demo.entiesMongodb;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.example.demo.entitiesMysql.RecetteEntity;

import org.springframework.data.mongodb.core.index.Indexed;
import lombok.Data;

@Document(collection = "recommandations_ia")
@Data
public class RecommandationIA {
    
    @Id
    private String id;
    
    @Indexed
    private Long userId;
    
    @Indexed
    private String type;
    
    private List<RecommandationDetail> recommandation;
    
    @Indexed
    private LocalDateTime dateRecommandation;
    
    private Double score;
    
    @Indexed
    private Boolean estUtilise;
    
    @Indexed
    private Long recetteId;
    
    // Champs pour l'intégration avec ComportementUtilisateur
    private String comportementUtilisateurId;
    private String profilUtilisateurCible;
    private Double scoreEngagementReference;
    private String creneauCible;
    private List<String> categoriesRecommandees;
    
    @Data
    public static class RecommandationDetail {
        private String titre;
        private String description;
        private String lien;
        private String categorie;
        private Double scoreRelevance;
        private List<String> tags;
       
    }
}