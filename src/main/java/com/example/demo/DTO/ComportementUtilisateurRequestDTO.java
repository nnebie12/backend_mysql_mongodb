package com.example.demo.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import com.example.demo.entiesMongodb.enums.ProfilUtilisateur;
import com.example.demo.entiesMongodb.enums.Saison;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComportementUtilisateurRequestDTO {
	
    private PreferencesSaisonnieresRequestDTO preferencesSaisonnieres;
    private HabitudesNavigationRequestDTO habitudesNavigation;
    private CyclesActiviteRequestDTO cyclesActivite;
    private MetriquesComportementalesRequestDTO metriques;

   
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PreferencesSaisonnieresRequestDTO {
        private java.util.List<String> ingredientsPrintemps;
        private java.util.List<String> ingredientsEte;
        private java.util.List<String> ingredientsAutomne;
        private java.util.List<String> ingredientsHiver;
        private Saison saisonPreferee;
        private java.util.Map<String, Double> scoresPreferenceSaisonniere;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HabitudesNavigationRequestDTO {
        private java.util.Map<String, Integer> pagesVisitees;
        private java.util.Map<String, Long> tempsParPage;
        private java.util.List<String> recherchesFavorites;
        private String typeRecettePreferee;
        private Integer nombreConnexionsParJour;
        private java.util.List<String> heuresConnexionHabituelles;
        private java.util.Map<String, String> parcoursFavoris;
        private Double tempsMoyenParSession;
        private Integer nombrePagesParSession;
        private java.util.List<String> categoriesPreferees;
        private java.util.Map<String, Integer> frequenceParCategorie;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CyclesActiviteRequestDTO {
        private CreneauRepasRequestDTO petitDejeuner;
        private CreneauRepasRequestDTO dejeuner;
        private CreneauRepasRequestDTO diner;
        private java.util.Map<String, Integer> activiteParJour;
        private java.util.List<String> joursActifs;
        private java.util.Map<String, java.util.List<String>> activitesParCreneau;
        private String creneauLePlusActif;
        private Double consistanceHoraire;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreneauRepasRequestDTO {
        private java.time.LocalTime heureDebut;
        private java.time.LocalTime heureFin;
        private java.util.List<String> typeRecettesPreferees;
        private Integer frequenceConsultation;
        private Boolean actif;
        private Double dureMoyenneConsultation;
        private java.util.List<String> ingredientsFavoris;
        private java.util.Map<com.example.demo.entiesMongodb.enums.ComplexiteRecette, Integer> complexitePreferee;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MetriquesComportementalesRequestDTO {
        private Integer nombreFavorisTotal;
        private Double noteMoyenneDonnee;
        private Integer nombreCommentairesLaisses;
        private Integer nombreRecherchesTotales;
        private java.util.List<String> termesRechercheFrequents;
        private Double tauxRecherchesFructueuses;
        private Double scoreEngagement;
        private ProfilUtilisateur profilUtilisateur;
        private java.util.Map<String, Integer> frequenceActions;
        private Double scoreRecommandation;
        private java.util.Map<String, Double> tendancesTemporelles;
        private Integer streakConnexion;
        private java.time.LocalDateTime derniereActivite;
    }
}
