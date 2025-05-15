package com.example.demo.entiesMongodb;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
@Document
public class NoteDocument { 
	@Id
    private String id;
    
    private Integer valeur;
    private String userId;
    private String recetteId;
    private String userName;
}
