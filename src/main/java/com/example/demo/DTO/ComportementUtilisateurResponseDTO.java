package com.example.demo.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.entiesMongodb.enums.ProfilUtilisateur;
import com.example.demo.entiesMongodb.enums.Saison;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComportementUtilisateurResponseDTO {
    private String id;
    private Long userId;
    private PreferencesSaisonnieresDTO preferencesSaisonnieres;
    private HabitudesNavigationDTO habitudesNavigation;
    private CyclesActiviteDTO cyclesActivite;
    private MetriquesComportementalesDTO metriques;
    private List<String> historiqueInteractionsIds;
    private List<String> historiqueRecherchesIds;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateCreation;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateMiseAJour;

    // ----- Classes imbriquées DTOs pour la réponse -----

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PreferencesSaisonnieresDTO {
        private List<String> ingredientsPrintemps;
        private List<String> ingredientsEte;
        private List<String> ingredientsAutomne;
        private List<String> ingredientsHiver;
        private Saison saisonPreferee; // Utilisez l'enum
        private java.util.Map<String, Double> scoresPreferenceSaisonniere;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime derniereMiseAJour;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HabitudesNavigationDTO {
        private java.util.Map<String, Integer> pagesVisitees;
        private java.util.Map<String, Long> tempsParPage;
        private List<String> recherchesFavorites;
        private String typeRecettePreferee;
        private Integer nombreConnexionsParJour;
        private List<String> heuresConnexionHabituelles;
        private java.util.Map<String, String> parcoursFavoris;
        private Double tempsMoyenParSession;
        private Integer nombrePagesParSession;
        private List<String> categoriesPreferees;
        private java.util.Map<String, Integer> frequenceParCategorie;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CyclesActiviteDTO {
        private CreneauRepasDTO petitDejeuner;
        private CreneauRepasDTO dejeuner;
        private CreneauRepasDTO diner;
        private java.util.Map<String, Integer> activiteParJour;
        private List<String> joursActifs;
        private java.util.Map<String, List<String>> activitesParCreneau;
        private String creneauLePlusActif;
        private Double consistanceHoraire;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreneauRepasDTO {
        @JsonFormat(pattern = "HH:mm:ss")
        private java.time.LocalTime heureDebut;
        @JsonFormat(pattern = "HH:mm:ss")
        private java.time.LocalTime heureFin;
        private List<String> typeRecettesPreferees;
        private Integer frequenceConsultation;
        private Boolean actif;
        private Double dureMoyenneConsultation;
        private List<String> ingredientsFavoris;
        private java.util.Map<com.example.demo.entiesMongodb.enums.ComplexiteRecette, Integer> complexitePreferee; 
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MetriquesComportementalesDTO {
        private Integer nombreFavorisTotal;
        private Double noteMoyenneDonnee;
        private Integer nombreCommentairesLaisses;
        private Integer nombreRecherchesTotales;
        private List<String> termesRechercheFrequents;
        private Double tauxRecherchesFructueuses;
        private Double scoreEngagement;
        private ProfilUtilisateur profilUtilisateur; 
        private java.util.Map<String, Integer> frequenceActions;
        private Double scoreRecommandation;
        private java.util.Map<String, Double> tendancesTemporelles;
        private Integer streakConnexion;
        private LocalDateTime derniereActivite;
    }
}
