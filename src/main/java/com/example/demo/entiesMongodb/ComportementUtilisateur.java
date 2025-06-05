package com.example.demo.entiesMongodb;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Entité représentant le comportement d'un utilisateur
 * Stocke les patterns, préférences et métriques comportementales
 */
@Document(collection = "comportement_utilisateur")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComportementUtilisateur {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long userId;

    // Préférences saisonnières
    private PreferencesSaisonnieres preferencesSaisonnieres;

    // Habitudes de navigation
    private HabitudesNavigation habitudesNavigation;

    // Cycles d'activité
    private CyclesActivite cyclesActivite;

    // Métriques comportementales (pour compatibilité avec l'implémentation existante)
    private MetriquesComportementales metriques;

    // Références aux interactions et recherches (pour éviter la duplication)
    private List<String> historiqueInteractionsIds;
    private List<String> historiqueRecherchesIds;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateCreation;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateMiseAJour;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PreferencesSaisonnieres {
        private List<String> ingredientsPrintemps;
        private List<String> ingredientsEte;
        private List<String> ingredientsAutomne;
        private List<String> ingredientsHiver;
        private String saisonPreferee;
        
        // Scores de préférence par saison (0-100)
        private Map<String, Double> scoresPreferenceSaisonniere;
        
        // Dernière mise à jour des préférences
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime derniereMiseAJour;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HabitudesNavigation {
        private Map<String, Integer> pagesVisitees; // page -> nombre de visites
        private Map<String, Long> tempsParPage; // page -> temps en secondes
        private List<String> recherchesFavorites;
        private String typeRecettePreferee;
        private Integer nombreConnexionsParJour;
        private List<String> heuresConnexionHabituelles;
        
        // Patterns de navigation
        private Map<String, String> parcoursFavoris; // séquences de pages visitées
        private Double tempsMoyenParSession; // en minutes
        private Integer nombrePagesParSession;
        
        // Préférences de contenu
        private List<String> categoriesPreferees;
        private Map<String, Integer> frequenceParCategorie;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CyclesActivite {
        private CreneauRepas petitDejeuner;
        private CreneauRepas dejeuner;
        private CreneauRepas diner;
        private Map<String, Integer> activiteParJour; // lundi -> niveau d'activité (0-100)
        private List<String> joursActifs;
        
        // Patterns temporels
        private Map<String, List<String>> activitesParCreneau; // matin/midi/soir -> types d'activités
        private String creneauLePlusActif;
        private Double consistanceHoraire; // régularité des horaires (0-100)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreneauRepas {
        @JsonFormat(pattern = "HH:mm:ss")
        private LocalTime heureDebut;
        
        @JsonFormat(pattern = "HH:mm:ss")
        private LocalTime heureFin;
        
        private List<String> typeRecettesPreferees;
        private Integer frequenceConsultation;
        private Boolean actif;
        
        // Nouvelles propriétés
        private Double dureMoyenneConsultation; // en minutes
        private List<String> ingredientsFavoris;
        private Map<String, Integer> complexitePreferee; // facile/moyen/difficile -> score
    }

    // Classe pour compatibilité avec l'implémentation existante
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MetriquesComportementales {
        private Integer nombreFavorisTotal;
        private Double noteMoyenneDonnee;
        private Integer nombreCommentairesLaisses;
        private Integer nombreRecherchesTotales;
        private List<String> termesRechercheFrequents;
        private Double tauxRecherchesFructueuses;
        private Double scoreEngagement;
        private String profilUtilisateur; // nouveau, débutant, occasionnel, actif, fidèle
        private Map<String, Integer> frequenceActions;
        
        // Métriques étendues
        private Double scoreRecommandation; // pertinence des recommandations
        private Map<String, Double> tendancesTemporelles; // évolution des comportements
        private Integer streakConnexion; // jours consécutifs d'activité
        private LocalDateTime derniereActivite;
    }

    // Méthodes utilitaires
    public boolean estUtilisateurActif() {
        return metriques != null && 
               metriques.getScoreEngagement() != null && 
               metriques.getScoreEngagement() > 15.0;
    }
    
    public boolean estNouvelUtilisateur() {
        return metriques == null || 
               "nouveau".equals(metriques.getProfilUtilisateur()) ||
               (dateCreation != null && dateCreation.isAfter(LocalDateTime.now().minusDays(7)));
    }
    
    public String getSaisonActuelle() {
        if (preferencesSaisonnieres != null) {
            return preferencesSaisonnieres.getSaisonPreferee();
        }
        return null;
    }
    
    public String getCreneauActuel() {
        LocalTime maintenant = LocalTime.now();
        if (cyclesActivite != null) {
            if (estDansCreneau(maintenant, cyclesActivite.getPetitDejeuner())) {
                return "petit-dejeuner";
            } else if (estDansCreneau(maintenant, cyclesActivite.getDejeuner())) {
                return "dejeuner";
            } else if (estDansCreneau(maintenant, cyclesActivite.getDiner())) {
                return "diner";
            }
        }
        return "hors-repas";
    }
    
    private boolean estDansCreneau(LocalTime heure, CreneauRepas creneau) {
        if (creneau == null || !Boolean.TRUE.equals(creneau.getActif())) {
            return false;
        }
        LocalTime debut = creneau.getHeureDebut();
        LocalTime fin = creneau.getHeureFin();
        
        if (debut != null && fin != null) {
            return !heure.isBefore(debut) && !heure.isAfter(fin);
        }
        return false;
    }
}