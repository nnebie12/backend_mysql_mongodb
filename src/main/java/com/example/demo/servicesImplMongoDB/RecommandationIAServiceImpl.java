package com.example.demo.servicesImplMongoDB;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.entiesMongodb.RecommandationIA.RecommandationDetail;
import com.example.demo.repositoryMongoDB.RecommandationIARepository;
import com.example.demo.servicesMongoDB.RecommandationIAService;
import com.example.demo.servicesMysql.SmsService;

@Service
public class RecommandationIAServiceImpl implements RecommandationIAService {

    private final RecommandationIARepository recommandationRepository;
    private final SmsService smsService;


    public RecommandationIAServiceImpl(RecommandationIARepository recommandationRepository,
            SmsService smsService) {
	this.recommandationRepository = recommandationRepository;
	this.smsService = smsService;
	}

    @Override
    public RecommandationIA addRecommandation(Long userId, String type, List<RecommandationDetail> recommandation, Double score) {
        RecommandationIA newRecommandation = new RecommandationIA();
        newRecommandation.setUserId(userId);
        newRecommandation.setType(type);
        newRecommandation.setRecommandation(recommandation);
        newRecommandation.setScore(score);
        newRecommandation.setDateRecommandation(LocalDateTime.now());
        newRecommandation.setEstUtilise(false);
        
        RecommandationIA saved = recommandationRepository.save(newRecommandation);

        String message = "Vous avez une nouvelle recommandation ! Consultez-la ici : https://tonsite.com/recommandations/" + saved.getId();
        String numeroUtilisateur = recoverNumberUser(userId); 
        
        if (numeroUtilisateur != null) {
            smsService.sendSms(numeroUtilisateur, message);
        }

        return saved;
    }
    
    private String recoverNumberUser(Long userId) {
        return "${SMS_RECIPIENT}";
    }


    @Override
    public List<RecommandationIA> getRecommandationsByUserId(Long userId) {
        return recommandationRepository.findByUserId(userId);
    }

    @Override
    public List<RecommandationIA> getRecommandationsByUserIdAndType(Long userId, String type) {
        return recommandationRepository.findByUserIdAndType(userId, type);
    }

    @Override
    public RecommandationIA markAsUsed(String recommandationId) {
        RecommandationIA recommandation = recommandationRepository.findById(recommandationId)
                .orElseThrow(() -> new RuntimeException("Recommandation non trouvée avec l'ID: " + recommandationId));
        
        recommandation.setEstUtilise(true);
        return recommandationRepository.save(recommandation);
    }

    @Override
    public void deleteRecommandationsUser(Long userId) {
        List<RecommandationIA> recommandations = recommandationRepository.findByUserId(userId);
        recommandationRepository.deleteAll(recommandations);
    }
}
