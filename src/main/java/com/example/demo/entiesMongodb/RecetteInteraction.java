package com.example.demo.entiesMongodb;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.mongodb.core.mapping.Document;

import com.example.demo.entitiesMysql.RecetteEntity;

import jakarta.persistence.Id;
import lombok.Data;

@Document(collection = "recette_interaction")
@Data
public class RecetteInteraction {
    @Id
    private String id;
    
    private Long idUser; 
    
    private Long idRecette; 
    
    private String typeInteraction; // CONSULTATION, RECHERCHE, FAVORI_AJOUTE, FAVORI_RETIRE, PARTAGE, IMPRESSION
    
    private LocalDateTime dateInteraction;
    
    private Integer dureeInteraction; 
    
    private String deviceType; // MOBILE, DESKTOP, TABLET
    
    private String sessionId; 
    
    private Map<String, Object> metadonnees; 
    
    private String sourceInteraction; // HOMEPAGE, SEARCH, RECOMMENDATION, DIRECT
    
    private Boolean comptabilisee = false; 
    
    private String adresseIP; 
    
    private Long recetteId;
}
