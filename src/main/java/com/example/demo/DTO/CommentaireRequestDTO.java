package com.example.demo.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class CommentaireRequestDTO {
    private String contenu;
    private String userId;
    private String userName; 
}
