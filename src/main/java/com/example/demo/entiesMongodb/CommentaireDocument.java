package com.example.demo.entiesMongodb;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.example.demo.entitiesMysql.RecetteEntity;

import jakarta.persistence.Id;
import lombok.Data;

@Data
@Document
public class CommentaireDocument {
    @Id
    private String id;
    
    private String contenu;
    private LocalDateTime dateCommentaire;
    private String userId;
    private String userName;
    private RecetteEntity recetteEntity;
}
