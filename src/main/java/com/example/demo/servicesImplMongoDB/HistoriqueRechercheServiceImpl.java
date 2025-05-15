package com.example.demo.servicesImplMongoDB;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.entiesMongodb.HistoriqueRecherche;
import com.example.demo.repositoryMongoDB.HistoriqueRechercheRepository;
import com.example.demo.servicesMongoDB.HistoriqueRechercheService;

@Service
public class HistoriqueRechercheServiceImpl implements HistoriqueRechercheService {
    private final HistoriqueRechercheRepository historiqueRepository;
    
    
    public HistoriqueRechercheServiceImpl(HistoriqueRechercheRepository historiqueRepository) {
        this.historiqueRepository = historiqueRepository;
    }
    
    @Override
    public HistoriqueRecherche enregistrerRecherche(Long userId, String terme, List<HistoriqueRecherche.Filtre> filtres) {
        HistoriqueRecherche historique = new HistoriqueRecherche();
        historique.setUserId(userId);
        historique.setTerme(terme);
        historique.setFiltres(filtres);
        historique.setDateRecherche(LocalDateTime.now());
        
        return historiqueRepository.save(historique);
    }
    
    @Override
    public List<HistoriqueRecherche> getHistoriqueByUserId(Long userId) {
        return historiqueRepository.findByUserIdOrderByDateRechercheDesc(userId);
    }
    
    @Override
    public List<HistoriqueRecherche> getRecherchesSimilaires(String terme) {
        return historiqueRepository.findByTermeContaining(terme);
    }
    
    @Override
    public void supprimerHistoriqueUtilisateur(Long userId) {
        List<HistoriqueRecherche> historiques = historiqueRepository.findByUserId(userId);
        historiqueRepository.deleteAll(historiques);
    }
}