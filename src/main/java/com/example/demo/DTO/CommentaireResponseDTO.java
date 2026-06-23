package com.example.demo.DTO;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentaireResponseDTO {
    private String id;
    private String contenu;
    private LocalDateTime dateCommentaire;
    private String userId;
    private String userName;
    private Long recetteId;
}
