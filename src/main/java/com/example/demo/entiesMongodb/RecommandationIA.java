package com.example.demo.entiesMongodb;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Data;

@Document
@Data
public class RecommandationIA {

    @Id
    private String id;

    private Long userId;

    private String type;

    private List<RecommandationDetail> recommandation;

    private LocalDateTime dateRecommandation;

    private Double score;

    private Boolean estUtilise;

    @Data
    public static class RecommandationDetail {
        private String titre;
        private String description;
        private String lien;
    }
}
