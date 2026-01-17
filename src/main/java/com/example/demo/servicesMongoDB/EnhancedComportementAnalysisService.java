package com.example.demo.servicesMongoDB;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.example.demo.entiesMongodb.*;
import com.example.demo.entiesMongodb.enums.*;
import com.example.demo.repositoryMongoDB.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service d'analyse comportementale enrichi avec algorithmes avancés
 */
@Service
public class EnhancedComportementAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedComportementAnalysisService.class);
    
    private final ComportementUtilisateurRepository comportementRepository;
    private final InteractionUtilisateurRepository interactionRepository;
    private final HistoriqueRechercheRepository rechercheRepository;
    private final RecetteInteractionRepository recetteInteractionRepository;
    
    // Seuils dynamiques pour la segmentation
    private static final Map<ProfilUtilisateur, Double> SEUILS_ENGAGEMENT = Map.of(
        ProfilUtilisateur.NOUVEAU, 0.0,
        ProfilUtilisateur.DEBUTANT, 15.0,
        ProfilUtilisateur.OCCASIONNEL, 30.0,
        ProfilUtilisateur.ACTIF, 50.0,
        ProfilUtilisateur.FIDELE, 75.0
    );
    
    public EnhancedComportementAnalysisService(
        ComportementUtilisateurRepository comportementRepository,
        InteractionUtilisateurRepository interactionRepository,
        HistoriqueRechercheRepository rechercheRepository,
        RecetteInteractionRepository recetteInteractionRepository
    ) {
        this.comportementRepository = comportementRepository;
        this.interactionRepository = interactionRepository;
        this.rechercheRepository = rechercheRepository;
        this.recetteInteractionRepository = recetteInteractionRepository;
    }
    
    /**
     * Analyse comportementale complète avec machine learning basique
     */
    public Map<String, Object> analyseComportementaleAvancee(Long userId) {
        Map<String, Object> analyse = new HashMap<>();
        
        try {
            // 1. Récupération des données
            ComportementUtilisateur comportement = comportementRepository.findByUserId(userId)
                .orElseGet(() -> creerComportementInitial(userId));
            
            List<InteractionUtilisateur> interactions = interactionRepository.findByUserId(userId);
            List<HistoriqueRecherche> recherches = rechercheRepository.findByUserId(userId);
            List<RecetteInteraction> recetteInteractions = recetteInteractionRepository.findByIdUser(userId);
            
            // 2. Analyse temporelle avancée
            analyse.put("patternsTemporels", analyserPatternsTemporelsAvances(interactions, recherches));
            
            // 3. Segmentation RFM (Récence, Fréquence, Monétaire adapté)
            analyse.put("scoreRFM", calculerScoreRFM(interactions, recetteInteractions));
            
            // 4. Analyse des préférences par clustering
            analyse.put("clustersPreferences", clusteriserPreferences(recherches, recetteInteractions));
            
            // 5. Prédiction de churn
            analyse.put("risqueChurn", calculerRisqueChurn(comportement, interactions));
            
            // 6. Recommandations d'engagement
            analyse.put("actionsEngagement", genererActionsEngagement(comportement, analyse));
            
            // 7. Score de prédictibilité
            analyse.put("scorePredictibilite", calculerScorePredictibilite(interactions, recherches));
            
            // 8. Analyse de la valeur client (CLV simplifié)
            analyse.put("valeurClient", calculerValeurClient(comportement, interactions));
            
            // Mise à jour du comportement avec les nouvelles insights
            mettreAJourComportement(comportement, analyse);
            
            return analyse;
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'analyse comportementale pour userId {}: {}", userId, e.getMessage(), e);
            return Map.of("erreur", e.getMessage());
        }
    }
    
    /**
     * Patterns temporels avancés avec détection de cycles
     */
    private Map<String, Object> analyserPatternsTemporelsAvances(
        List<InteractionUtilisateur> interactions, 
        List<HistoriqueRecherche> recherches
    ) {
        Map<String, Object> patterns = new HashMap<>();
        
        // Analyse par heure avec détection de pics
        Map<Integer, Long> activiteParHeure = new TreeMap<>();
        interactions.stream()
            .filter(i -> i.getDateInteraction() != null)
            .forEach(i -> activiteParHeure.merge(i.getDateInteraction().getHour(), 1L, Long::sum));
        
        patterns.put("activiteParHeure", activiteParHeure);
        patterns.put("heuresPic", detecterHeuresPic(activiteParHeure));
        
        // Analyse par jour de la semaine
        Map<DayOfWeek, Long> activiteParJour = new TreeMap<>();
        interactions.stream()
            .filter(i -> i.getDateInteraction() != null)
            .forEach(i -> activiteParJour.merge(i.getDateInteraction().getDayOfWeek(), 1L, Long::sum));
        
        patterns.put("joursActifs", activiteParJour);
        patterns.put("regularite", calculerRegularite(activiteParJour));
        
        // Détection de séquences temporelles
        patterns.put("sequences", detecterSequencesTemporelles(interactions));
        
        return patterns;
    }
    
    /**
     * Calcul du score RFM adapté au contexte culinaire
     */
    private Map<String, Object> calculerScoreRFM(
        List<InteractionUtilisateur> interactions,
        List<RecetteInteraction> recetteInteractions
    ) {
        Map<String, Object> rfm = new HashMap<>();
        
        // Récence (R) - Dernière activité
        LocalDateTime derniere = interactions.stream()
            .map(InteractionUtilisateur::getDateInteraction)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now().minusMonths(6));
        
        long joursDepuisDerniere = ChronoUnit.DAYS.between(derniere, LocalDateTime.now());
        int scoreRecence = calculerScoreRecence(joursDepuisDerniere);
        
        // Fréquence (F) - Nombre d'interactions
        int scoreFrequence = calculerScoreFrequence(interactions.size());
        
        // Monétaire adapté (M) - Engagement/Qualité des interactions
        double scoreMonetaire = calculerScoreEngagementQualitatif(recetteInteractions);
        
        rfm.put("recence", scoreRecence);
        rfm.put("frequence", scoreFrequence);
        rfm.put("engagement", scoreMonetaire);
        rfm.put("scoreTotal", (scoreRecence + scoreFrequence + scoreMonetaire) / 3.0);
        rfm.put("segment", determinerSegmentRFM(scoreRecence, scoreFrequence, scoreMonetaire));
        
        return rfm;
    }
    
    /**
     * Clustering des préférences utilisateur
     */
    private Map<String, Object> clusteriserPreferences(
        List<HistoriqueRecherche> recherches,
        List<RecetteInteraction> recetteInteractions
    ) {
        Map<String, Object> clusters = new HashMap<>();
        
        // Cluster par type de contenu recherché
        Map<String, Long> categoriesRecherchees = recherches.stream()
            .map(HistoriqueRecherche::getCategorieRecherche)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
        
        clusters.put("categoriesDominantes", categoriesRecherchees.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList()));
        
        // Cluster par moment de la journée
        Map<String, Long> momentsPreferes = recetteInteractions.stream()
            .filter(ri -> ri.getDateInteraction() != null)
            .collect(Collectors.groupingBy(
                ri -> determinerMomentJournee(ri.getDateInteraction()),
                Collectors.counting()
            ));
        
        clusters.put("momentsPreferes", momentsPreferes);
        
        // Cluster par type d'appareil
        Map<String, Long> appareilsPreferes = recetteInteractions.stream()
            .filter(ri -> ri.getDeviceType() != null)
            .collect(Collectors.groupingBy(
                RecetteInteraction::getDeviceType,
                Collectors.counting()
            ));
        
        clusters.put("appareilsDominants", appareilsPreferes);
        
        return clusters;
    }
    
    /**
     * Calcul du risque de churn (désengagement)
     */
    private Map<String, Object> calculerRisqueChurn(
        ComportementUtilisateur comportement,
        List<InteractionUtilisateur> interactions
    ) {
        Map<String, Object> churn = new HashMap<>();
        
        // Facteurs de risque
        int score = 0;
        List<String> indicateurs = new ArrayList<>();
        
        // 1. Inactivité récente
        LocalDateTime derniere = interactions.stream()
            .map(InteractionUtilisateur::getDateInteraction)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now().minusMonths(6));
        
        long joursInactif = ChronoUnit.DAYS.between(derniere, LocalDateTime.now());
        if (joursInactif > 30) {
            score += 40;
            indicateurs.add("Inactif depuis " + joursInactif + " jours");
        }
        
        // 2. Baisse d'engagement
        if (comportement.getMetriques() != null) {
            Double scoreEngagement = comportement.getMetriques().getScoreEngagement();
            if (scoreEngagement != null && scoreEngagement < 20) {
                score += 30;
                indicateurs.add("Score d'engagement faible: " + scoreEngagement);
            }
        }
        
        // 3. Recherches infructueuses répétées
        long recherchesFailed = rechercheRepository.findByUserIdAndRechercheFructueuse(
            comportement.getUserId(), false
        ).size();
        
        if (recherchesFailed > 5) {
            score += 20;
            indicateurs.add(recherchesFailed + " recherches infructueuses");
        }
        
        // 4. Absence de favoris récents
        if (comportement.getMetriques() != null && 
            (comportement.getMetriques().getNombreFavorisTotal() == null || 
             comportement.getMetriques().getNombreFavorisTotal() == 0)) {
            score += 10;
            indicateurs.add("Aucun favori enregistré");
        }
        
        churn.put("score", Math.min(score, 100));
        churn.put("niveau", determinerNiveauRisque(score));
        churn.put("indicateurs", indicateurs);
        
        return churn;
    }
    
    /**
     * Génération d'actions d'engagement personnalisées
     */
    private List<Map<String, String>> genererActionsEngagement(
        ComportementUtilisateur comportement,
        Map<String, Object> analyse
    ) {
        List<Map<String, String>> actions = new ArrayList<>();
        
        // Analyse du risque de churn
        @SuppressWarnings("unchecked")
        Map<String, Object> churn = (Map<String, Object>) analyse.get("risqueChurn");
        int scoreChurn = (int) churn.getOrDefault("score", 0);
        
        if (scoreChurn > 50) {
            actions.add(Map.of(
                "action", "CAMPAGNE_REENGAGEMENT",
                "priorite", "HAUTE",
                "description", "Envoyer une campagne de réengagement avec offres exclusives"
            ));
        }
        
        // Analyse des préférences
        if (comportement.getPreferencesSaisonnieres() == null || 
            comportement.getPreferencesSaisonnieres().getSaisonPreferee() == null) {
            actions.add(Map.of(
                "action", "QUIZ_PREFERENCES",
                "priorite", "MOYENNE",
                "description", "Proposer un quiz pour définir les préférences"
            ));
        }
        
        // Analyse de l'engagement
        Double scoreEngagement = comportement.getMetriques() != null ? 
            comportement.getMetriques().getScoreEngagement() : 0.0;
        
        if (scoreEngagement != null && scoreEngagement < 30) {
            actions.add(Map.of(
                "action", "TUTORIEL_ONBOARDING",
                "priorite", "HAUTE",
                "description", "Relancer le tutoriel d'utilisation"
            ));
        }
        
        return actions;
    }
    
    /**
     * Calcul du score de prédictibilité amélioré
     */
    private double calculerScorePredictibilite(
        List<InteractionUtilisateur> interactions,
        List<HistoriqueRecherche> recherches
    ) {
        if (interactions.isEmpty() && recherches.isEmpty()) return 0.0;
        
        double score = 0.0;
        
        // 1. Régularité temporelle (40 points)
        double regularite = calculerRegulariteActivite(interactions);
        score += regularite * 40;
        
        // 2. Cohérence des recherches (30 points)
        double coherence = calculerCoherenceRecherches(recherches);
        score += coherence * 30;
        
        // 3. Patterns répétitifs (30 points)
        double repetition = detecterPatternsRepetitifs(interactions);
        score += repetition * 30;
        
        return Math.min(score, 100.0);
    }
    
    /**
     * Calcul de la valeur client (CLV simplifié)
     */
    private Map<String, Object> calculerValeurClient(
        ComportementUtilisateur comportement,
        List<InteractionUtilisateur> interactions
    ) {
        Map<String, Object> clv = new HashMap<>();
        
        // Calcul basé sur l'engagement et la fidélité
        double scoreBase = comportement.getMetriques() != null ? 
            comportement.getMetriques().getScoreEngagement() : 0.0;
        
        // Multiplicateur selon le profil
        double multiplicateur = 1.0;
        if (comportement.getMetriques() != null) {
            ProfilUtilisateur profil = comportement.getMetriques().getProfilUtilisateur();
            multiplicateur = switch (profil) {
                case FIDELE -> 2.0;
                case ACTIF -> 1.5;
                case OCCASIONNEL -> 1.0;
                case DEBUTANT -> 0.7;
                default -> 0.5;
            };
        }
        
        double valeur = scoreBase * multiplicateur;
        
        clv.put("valeurActuelle", valeur);
        clv.put("potentiel", calculerPotentielCroissance(comportement));
        clv.put("segment", categoriserValeur(valeur));
        
        return clv;
    }
    
    // ========== Méthodes utilitaires ==========
    
    private ComportementUtilisateur creerComportementInitial(Long userId) {
        ComportementUtilisateur comportement = new ComportementUtilisateur();
        comportement.setUserId(userId);
        comportement.setDateCreation(LocalDateTime.now());
        comportement.setDateMiseAJour(LocalDateTime.now());
        
        ComportementUtilisateur.MetriquesComportementales metriques = 
            new ComportementUtilisateur.MetriquesComportementales();
        metriques.setScoreEngagement(0.0);
        metriques.setProfilUtilisateur(ProfilUtilisateur.NOUVEAU);
        comportement.setMetriques(metriques);
        
        return comportementRepository.save(comportement);
    }
    
    private List<Integer> detecterHeuresPic(Map<Integer, Long> activiteParHeure) {
        if (activiteParHeure.isEmpty()) return List.of();
        
        double moyenne = activiteParHeure.values().stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        return activiteParHeure.entrySet().stream()
            .filter(e -> e.getValue() > moyenne * 1.5)
            .map(Map.Entry::getKey)
            .sorted()
            .collect(Collectors.toList());
    }
    
    private double calculerRegularite(Map<DayOfWeek, Long> activiteParJour) {
        if (activiteParJour.isEmpty()) return 0.0;
        
        long total = activiteParJour.values().stream().mapToLong(Long::longValue).sum();
        double moyenne = total / 7.0;
        
        double variance = activiteParJour.values().stream()
            .mapToDouble(v -> Math.pow(v - moyenne, 2))
            .average()
            .orElse(0.0);
        
        // Plus la variance est faible, plus la régularité est élevée
        return Math.max(0, 100 - (variance / moyenne * 10));
    }
    
    private List<String> detecterSequencesTemporelles(List<InteractionUtilisateur> interactions) {
        // Détection de séquences d'actions récurrentes
        List<String> sequences = new ArrayList<>();
        
        if (interactions.size() < 3) return sequences;
        
        List<InteractionUtilisateur> sorted = interactions.stream()
            .filter(i -> i.getDateInteraction() != null && i.getTypeInteraction() != null)
            .sorted(Comparator.comparing(InteractionUtilisateur::getDateInteraction))
            .collect(Collectors.toList());
        
        Map<String, Integer> pairesFrequentes = new HashMap<>();
        
        for (int i = 0; i < sorted.size() - 1; i++) {
            String paire = sorted.get(i).getTypeInteraction() + " → " + 
                          sorted.get(i + 1).getTypeInteraction();
            pairesFrequentes.merge(paire, 1, Integer::sum);
        }
        
        return pairesFrequentes.entrySet().stream()
            .filter(e -> e.getValue() >= 3)
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    private int calculerScoreRecence(long jours) {
        if (jours <= 1) return 5;
        if (jours <= 7) return 4;
        if (jours <= 30) return 3;
        if (jours <= 90) return 2;
        return 1;
    }
    
    private int calculerScoreFrequence(int nombreInteractions) {
        if (nombreInteractions >= 50) return 5;
        if (nombreInteractions >= 20) return 4;
        if (nombreInteractions >= 10) return 3;
        if (nombreInteractions >= 5) return 2;
        return 1;
    }
    
    private double calculerScoreEngagementQualitatif(List<RecetteInteraction> interactions) {
        if (interactions.isEmpty()) return 1.0;
        
        double score = 0.0;
        for (RecetteInteraction ri : interactions) {
            score += switch (ri.getTypeInteraction()) {
                case "FAVORI_AJOUTE" -> 5.0;
                case "PARTAGE" -> 4.0;
                case "IMPRESSION" -> 3.0;
                case "RECHERCHE" -> 2.0;
                default -> 1.0;
            };
        }
        
        return Math.min(5.0, score / interactions.size());
    }
    
    private String determinerSegmentRFM(int recence, int frequence, double engagement) {
        int scoreTotal = recence + frequence + (int) engagement;
        
        if (scoreTotal >= 13) return "CHAMPIONS";
        if (scoreTotal >= 10) return "FIDELES";
        if (scoreTotal >= 7) return "POTENTIEL";
        if (scoreTotal >= 5) return "RISQUE";
        return "PERDU";
    }
    
    private String determinerMomentJournee(LocalDateTime date) {
        int heure = date.getHour();
        if (heure >= 6 && heure < 11) return "MATIN";
        if (heure >= 11 && heure < 14) return "MIDI";
        if (heure >= 14 && heure < 18) return "APRES_MIDI";
        if (heure >= 18 && heure < 22) return "SOIR";
        return "NUIT";
    }
    
    private String determinerNiveauRisque(int score) {
        if (score >= 70) return "CRITIQUE";
        if (score >= 50) return "ELEVE";
        if (score >= 30) return "MOYEN";
        return "FAIBLE";
    }
    
    private double calculerRegulariteActivite(List<InteractionUtilisateur> interactions) {
        if (interactions.size() < 2) return 0.0;
        
        List<LocalDateTime> dates = interactions.stream()
            .map(InteractionUtilisateur::getDateInteraction)
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.toList());
        
        if (dates.size() < 2) return 0.0;
        
        List<Long> intervalles = new ArrayList<>();
        for (int i = 1; i < dates.size(); i++) {
            intervalles.add(ChronoUnit.HOURS.between(dates.get(i-1), dates.get(i)));
        }
        
        double moyenne = intervalles.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double variance = intervalles.stream()
            .mapToDouble(v -> Math.pow(v - moyenne, 2))
            .average()
            .orElse(Double.MAX_VALUE);
        
        return Math.max(0.0, 1.0 - (Math.sqrt(variance) / moyenne));
    }
    
    private double calculerCoherenceRecherches(List<HistoriqueRecherche> recherches) {
        if (recherches.size() < 2) return 0.0;
        
        Map<String, Long> categories = recherches.stream()
            .map(HistoriqueRecherche::getCategorieRecherche)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
        
        if (categories.isEmpty()) return 0.0;
        
        long max = categories.values().stream().max(Long::compare).orElse(0L);
        return (double) max / recherches.size();
    }
    
    private double detecterPatternsRepetitifs(List<InteractionUtilisateur> interactions) {
        if (interactions.size() < 5) return 0.0;
        
        Map<String, Long> typesFrequents = interactions.stream()
            .map(InteractionUtilisateur::getTypeInteraction)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        
        if (typesFrequents.isEmpty()) return 0.0;
        
        long max = typesFrequents.values().stream().max(Long::compare).orElse(0L);
        return (double) max / interactions.size();
    }
    
    private double calculerPotentielCroissance(ComportementUtilisateur comportement) {
        if (comportement.getMetriques() == null) return 50.0;
        
        ProfilUtilisateur profil = comportement.getMetriques().getProfilUtilisateur();
        Double scoreActuel = comportement.getMetriques().getScoreEngagement();
        
        if (scoreActuel == null) return 50.0;
        
        return switch (profil) {
            case NOUVEAU -> 90.0;
            case DEBUTANT -> 75.0;
            case OCCASIONNEL -> 60.0;
            case ACTIF -> 40.0;
            case FIDELE -> 20.0;
        };
    }
    
    private String categoriserValeur(double valeur) {
        if (valeur >= 150) return "VIP";
        if (valeur >= 100) return "PREMIUM";
        if (valeur >= 50) return "STANDARD";
        return "BASIQUE";
    }
    
    private void mettreAJourComportement(ComportementUtilisateur comportement, Map<String, Object> analyse) {
        try {
            // Mise à jour des métriques avancées
            if (comportement.getMetriques() == null) {
                comportement.setMetriques(new ComportementUtilisateur.MetriquesComportementales());
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> rfm = (Map<String, Object>) analyse.get("scoreRFM");
            if (rfm != null) {
                comportement.getMetriques().setScoreRecommandation(
                    (Double) rfm.getOrDefault("scoreTotal", 0.0)
                );
            }
            
            comportement.getMetriques().setScorePredictibilite(
                (Double) analyse.getOrDefault("scorePredictibilite", 0.0)
            );
            
            comportement.setDateMiseAJour(LocalDateTime.now());
            comportementRepository.save(comportement);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du comportement: {}", e.getMessage());
        }
    }
}
