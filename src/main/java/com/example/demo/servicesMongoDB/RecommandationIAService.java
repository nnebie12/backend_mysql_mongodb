package com.example.demo.servicesMongoDB;

import java.util.List;

import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.entiesMongodb.RecommandationIA.RecommandationDetail;

public interface RecommandationIAService {
    RecommandationIA addRecommandation(Long userId, String type, List<RecommandationDetail> recommandation, Double score);

    List<RecommandationIA> getRecommandationsByUserId(Long userId);

    List<RecommandationIA> getRecommandationsByUserIdAndType(Long userId, String type);

    RecommandationIA markAsUsed(String recommandationId);

    void deleteRecommandationsUser(Long userId);
}