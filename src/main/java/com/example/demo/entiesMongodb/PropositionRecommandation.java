package com.example.demo.entiesMongodb;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "proposition_recommandation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropositionRecommandation {
    @Id
    private String id;

    private Long idUser;
    private String idRecommandation;

    private String statut = "PROPOSEE";
    private LocalDateTime dateProposition = LocalDateTime.now();
    private LocalDateTime dateReponse;

    private String feedbackUser = "";
    private Double scoreInteret = 0.5;
    private String raisonRefus = "";

    private boolean notificationEnvoyee = false;
    private Integer priorite = 3;
}
