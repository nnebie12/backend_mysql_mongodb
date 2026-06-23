package com.example.demo.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class CommentaireRequestDTO {
    private String contenu;
    private String userId;
    private String userName; 
    private Long recetteId;
}
