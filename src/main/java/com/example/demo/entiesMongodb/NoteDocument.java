package com.example.demo.entiesMongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

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
