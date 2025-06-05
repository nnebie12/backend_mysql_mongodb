package com.example.demo.entiesMongodb;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Data;

@Data
@Document
public class InteractionUtilisateur {
    @Id
    private String id;
    
    private Long userId;
    
    private String typeInteraction; 
    
    private Long entiteId;
    
    private LocalDateTime dateInteraction;
    
    private Integer dureeConsultation; 
        
}
