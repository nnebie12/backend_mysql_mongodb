package com.example.demo.servicesMongoDB;

import java.util.List;

import com.example.demo.entiesMongodb.HistoriqueRecherche;

public interface HistoriqueRechercheService {
    HistoriqueRecherche enregistrerRecherche(Long userId, String terme, List<HistoriqueRecherche.Filtre> filtres);
    List<HistoriqueRecherche> getHistoriqueByUserId(Long userId);
    List<HistoriqueRecherche> getRecherchesSimilaires(String terme);
    void supprimerHistoriqueUtilisateur(Long userId);
}

