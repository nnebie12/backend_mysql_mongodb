package com.example.demo.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentaireResponseDTO {
    private String id;
    private String contenu;
    private LocalDateTime dateCommentaire;
    private String userId;
    private String userName;
}
