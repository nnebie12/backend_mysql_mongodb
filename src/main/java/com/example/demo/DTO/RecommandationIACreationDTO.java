package com.example.demo.DTO;

import java.util.List;

import com.example.demo.entiesMongodb.RecommandationIA.RecommandationDetail;

import lombok.Data;

@Data
public class RecommandationIACreationDTO {
    private Long userId;
    private String type;
    private List<RecommandationDetail> recommandations;
    private Double score;
    private String profilUtilisateurCible;
    private Double scoreEngagementReference;
    private String creneauCible;
    private List<String> categoriesRecommandees;
}
