package com.example.demo.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteResponseDTO {
    private String id; 
    private Integer valeur;
    private String userId;
    private String recetteId;
    private String userName; 
}