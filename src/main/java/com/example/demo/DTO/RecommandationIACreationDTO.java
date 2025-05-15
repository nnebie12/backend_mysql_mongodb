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
}
