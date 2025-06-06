package com.example.demo.servicesImplMongoDB;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import com.example.demo.DTO.AnalysePatternsDTO;
import com.example.demo.entiesMongodb.*;
import com.example.demo.repositoryMongoDB.ComportementUtilisateurRepository;
import com.example.demo.servicesMongoDB.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ComportementUtilisateurServiceImpl implements ComportementUtilisateurService {

    private static final Logger logger = LoggerFactory.getLogger(ComportementUtilisateurServiceImpl.class);

    private final ComportementUtilisateurRepository comportementRepository;
    private final InteractionUtilisateurService interactionService;
    private final HistoriqueRechercheService historiqueRechercheService;
    // Ajoutez ces services selon votre architecture
    // private final FavorisService favorisService;
    // private final NoteService noteService;
    // private final CommentaireService commentaireService;

    // Constantes pour les seuils
    private static final int MAX_HISTORIQUE_RECHERCHES = 500;
    private static final int MAX_HISTORIQUE_INTERACTIONS = 1000;
    private static final double SCORE_ENGAGEMENT_FIDELE = 70.0;
    private static final double SCORE_ENGAGEMENT_ACTIF = 40.0;
    private static final double SCORE_ENGAGEMENT_OCCASIONNEL = 15.0;

    public ComportementUtilisateurServiceImpl(ComportementUtilisateurRepository comportementRepository,
                                            InteractionUtilisateurService interactionService,
                                            HistoriqueRechercheService historiqueRechercheService) {
        this.comportementRepository = comportementRepository;
        this.interactionService = interactionService;
        this.historiqueRechercheService = historiqueRechercheService;
    }

    @Override
    public ComportementUtilisateur createBehavior(Long userId) {
        ComportementUtilisateur comportement = new ComportementUtilisateur();
        comportement.setUserId(userId);
        comportement.setDateCreation(LocalDateTime.now());
        comportement.setDateMiseAJour(LocalDateTime.now());
        comportement.setHistoriqueInteractionsIds(new ArrayList<>());
        comportement.setHistoriqueRecherchesIds(new ArrayList<>());

        // Initialiser les métriques
        ComportementUtilisateur.MetriquesComportementales metriques =
            new ComportementUtilisateur.MetriquesComportementales();
        metriques.setNombreFavorisTotal(0);
        metriques.setNombreCommentairesLaisses(0);
        metriques.setScoreEngagement(0.0);
        metriques.setProfilUtilisateur("nouveau");
        metriques.setFrequenceActions(new HashMap<>());
        comportement.setMetriques(metriques);

        return comportementRepository.save(comportement);
    }

    @Override
    public Optional<ComportementUtilisateur> getBehaviorByUserId(Long userId) {
        return comportementRepository.findByUserId(userId);
    }

    @Override
    public ComportementUtilisateur getOrCreateBehavior(Long userId) {
        return getBehaviorByUserId(userId)
            .orElseGet(() -> createBehavior(userId));
    }

    @Override
    public ComportementUtilisateur updateBehavior(ComportementUtilisateur comportement) {
        comportement.setDateMiseAJour(LocalDateTime.now());
        return comportementRepository.save(comportement);
    }

    @Override
    public void updateMetrics(Long userId) {
        ComportementUtilisateur comportement = getOrCreateBehavior(userId);
        ComportementUtilisateur.MetriquesComportementales metriques =
            comportement.getMetriques() != null ?
            comportement.getMetriques() :
            new ComportementUtilisateur.MetriquesComportementales();

        // Récupérer les données des services existants
        List<InteractionUtilisateur> interactions = interactionService.getInteractionsByUserId(userId);
        List<HistoriqueRecherche> recherches = historiqueRechercheService.getHistoryByUserId(userId);

        // Vous pouvez décommenter ces lignes selon vos services disponibles
        // List<Favoris> favoris = favorisService.getFavorisByUserId(userId);
        // List<Note> notes = noteService.getNotesByUserId(userId);
        // List<Commentaire> commentaires = commentaireService.getCommentairesByUserId(userId);

        // Calculer les métriques depuis vos entités
        // metriques.setNombreFavorisTotal(favoris.size());
        // metriques.setNoteMoyenneDonnee(notes.stream()
        //     .mapToDouble(Note::getValeur)
        //     .average()
        //     .orElse(0.0));
        // metriques.setNombreCommentairesLaisses(commentaires.size());

        // Métriques de recherche
        metriques.setNombreRecherchesTotales(recherches.size());
        metriques.setTermesRechercheFrequents(
            recherches.stream()
                .collect(Collectors.groupingBy(
                    HistoriqueRecherche::getTerme,
                    Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList())
        );

        // Calculer le taux de recherches fructueuses si disponible
        long recherchesFructueuses = recherches.stream()
            .filter(r -> Boolean.TRUE.equals(r.getRechercheFructueuse()))
            .count();
        double tauxFructueuses = recherches.isEmpty() ? 0.0 :
            (double) recherchesFructueuses / recherches.size() * 100;
        metriques.setTauxRecherchesFructueuses(tauxFructueuses);

        // Analyser les patterns
        metriques.setScoreEngagement(calculerScoreEngagement(interactions, recherches));
        metriques.setProfilUtilisateur(determinerProfilUtilisateur(metriques.getScoreEngagement()));

        // Mettre à jour les fréquences d'actions
        Map<String, Integer> frequences = new HashMap<>();
        interactions.forEach(interaction -> {
            String type = interaction.getTypeInteraction();
            frequences.put(type, frequences.getOrDefault(type, 0) + 1);
        });
        metriques.setFrequenceActions(frequences);

        // Stocker les références aux entités séparées
        comportement.setHistoriqueInteractionsIds(
            interactions.stream().map(InteractionUtilisateur::getId).collect(Collectors.toList())
        );
        comportement.setHistoriqueRecherchesIds(
            recherches.stream().map(HistoriqueRecherche::getId).collect(Collectors.toList())
        );

        comportement.setMetriques(metriques);
        updateBehavior(comportement);
    }

    @Override
    public ComportementUtilisateur recordSearch(Long userId, String terme,
                                                       List<HistoriqueRecherche.Filtre> filtres,
                                                       Integer nombreResultats,
                                                       Boolean rechercheFructueuse) {
        // Utiliser votre service existant pour créer l'historique
        HistoriqueRecherche nouvelleRecherche = historiqueRechercheService.recordSearch(userId, terme, filtres);

        // Si vous avez enrichi votre service avec la méthode complète
        // HistoriqueRecherche nouvelleRecherche = historiqueRechercheService.enregistrerRechercheComplete(
        //     userId, terme, filtres, nombreResultats, rechercheFructueuse);

        // Mettre à jour le comportement utilisateur
        ComportementUtilisateur comportement = getOrCreateBehavior(userId);

        if (comportement.getHistoriqueRecherchesIds() == null) {
            comportement.setHistoriqueRecherchesIds(new ArrayList<>());
        }

        comportement.getHistoriqueRecherchesIds().add(nouvelleRecherche.getId());

        // Limiter les références aux 500 dernières
        if (comportement.getHistoriqueRecherchesIds().size() > MAX_HISTORIQUE_RECHERCHES) {
            comportement.getHistoriqueRecherchesIds().remove(0);
        }

        return updateBehavior(comportement);
    }

    @Override
    public ComportementUtilisateur enregistrerInteraction(Long userId, String typeInteraction,
                                                         String entiteInteraction, String detailsInteraction) {
        // Utiliser votre service existant avec des valeurs par défaut
        // Vous pouvez adapter cette logique selon vos besoins métier
        InteractionUtilisateur nouvelleInteraction = interactionService.addInteractionUtilisateur(
            userId, typeInteraction, null, null);

        // Mettre à jour le comportement utilisateur
        ComportementUtilisateur comportement = getOrCreateBehavior(userId);

        if (comportement.getHistoriqueInteractionsIds() == null) {
            comportement.setHistoriqueInteractionsIds(new ArrayList<>());
        }

        comportement.getHistoriqueInteractionsIds().add(nouvelleInteraction.getId());

        // Limiter les références aux 1000 dernières interactions
        if (comportement.getHistoriqueInteractionsIds().size() > MAX_HISTORIQUE_INTERACTIONS) {
            comportement.getHistoriqueInteractionsIds().remove(0);
        }

        // Mettre à jour les métriques en temps réel
        ComportementUtilisateur.MetriquesComportementales metriques =
            comportement.getMetriques() != null ?
            comportement.getMetriques() :
            new ComportementUtilisateur.MetriquesComportementales();

        // Mettre à jour la fréquence des actions
        Map<String, Integer> frequences = metriques.getFrequenceActions();
        if (frequences == null) {
            frequences = new HashMap<>();
        }
        frequences.put(typeInteraction, frequences.getOrDefault(typeInteraction, 0) + 1);
        metriques.setFrequenceActions(frequences);

        // Recalculer le score d'engagement
        List<InteractionUtilisateur> interactions = interactionService.getInteractionsByUserId(userId);
        List<HistoriqueRecherche> recherches = historiqueRechercheService.getHistoryByUserId(userId);
        metriques.setScoreEngagement(calculerScoreEngagement(interactions, recherches));
        metriques.setProfilUtilisateur(determinerProfilUtilisateur(metriques.getScoreEngagement()));

        comportement.setMetriques(metriques);

        return updateBehavior(comportement);
    }

    @Override
    public List<String> getFrequentSearchTerms(Long userId) {
        List<HistoriqueRecherche> recherches = historiqueRechercheService.getHistoryByUserId(userId);

        return recherches.stream()
            .collect(Collectors.groupingBy(
                HistoriqueRecherche::getTerme,
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    @Override
    public List<ComportementUtilisateur> getUsersByProfile(String profil) {
        return comportementRepository.findByMetriques_ProfilUtilisateur(profil);
    }

    @Override
    public List<ComportementUtilisateur> getEngagedUsers(Double scoreMinimum) {
        return comportementRepository.findByMetriques_ScoreEngagementGreaterThan(scoreMinimum);
    }

    @Override
    public void deleteUserBehavior(Long userId) {
        comportementRepository.deleteByUserId(userId);
    }

    private Double calculerScoreEngagement(List<InteractionUtilisateur> interactions,
                                         List<HistoriqueRecherche> recherches) {
        double score = 0.0;

        // Points pour les interactions
        score += interactions.size() * 0.1;

        // Points pour les recherches
        score += recherches.size() * 0.05;

        // Bonus pour les recherches fructueuses
        long recherchesFructueuses = recherches.stream()
            .filter(r -> Boolean.TRUE.equals(r.getRechercheFructueuse()))
            .count();
        score += recherchesFructueuses * 0.1;

        // Vous pouvez ajouter d'autres critères selon vos besoins
        // score += favoris.size() * 2.0;
        // score += notes.size() * 1.5;
        // score += commentaires.size() * 3.0;

        // Normaliser le score (0-100)
        return Math.min(100.0, score);
    }

    private String determinerProfilUtilisateur(Double scoreEngagement) {
        if (scoreEngagement == null) return "nouveau";

        if (scoreEngagement > SCORE_ENGAGEMENT_FIDELE) return "fidèle";
        else if (scoreEngagement > SCORE_ENGAGEMENT_ACTIF) return "actif";
        else if (scoreEngagement > SCORE_ENGAGEMENT_OCCASIONNEL) return "occasionnel";
        else return "débutant";
    }

    @Override
    public Map<String, Object> obtenirStatistiquesComportement(Long userId) {
        ComportementUtilisateur comportement = getOrCreateBehavior(userId);
        Map<String, Object> statistiques = new HashMap<>();

        // Informations générales
        statistiques.put("userId", userId);
        statistiques.put("dateCreation", comportement.getDateCreation());
        statistiques.put("dateMiseAJour", comportement.getDateMiseAJour());

        // Métriques comportementales
        ComportementUtilisateur.MetriquesComportementales metriques = comportement.getMetriques();
        if (metriques != null) {
            statistiques.put("scoreEngagement", metriques.getScoreEngagement());
            statistiques.put("profilUtilisateur", metriques.getProfilUtilisateur());
            statistiques.put("nombreFavorisTotal", metriques.getNombreFavorisTotal());
            statistiques.put("nombreCommentairesLaisses", metriques.getNombreCommentairesLaisses());
            statistiques.put("noteMoyenneDonnee", metriques.getNoteMoyenneDonnee());
            statistiques.put("nombreRecherchesTotales", metriques.getNombreRecherchesTotales());
            statistiques.put("tauxRecherchesFructueuses", metriques.getTauxRecherchesFructueuses());
            statistiques.put("termesRechercheFrequents", metriques.getTermesRechercheFrequents());
            statistiques.put("frequenceActions", metriques.getFrequenceActions());
        }

        // Statistiques détaillées depuis les services
        try {
            List<InteractionUtilisateur> interactions = interactionService.getInteractionsByUserId(userId);
            List<HistoriqueRecherche> recherches = historiqueRechercheService.getHistoryByUserId(userId);

            // Statistiques d'interaction
            statistiques.put("nombreInteractionsTotal", interactions.size());

            // Grouper les interactions par type
            Map<String, Long> interactionsParType = interactions.stream()
                .collect(Collectors.groupingBy(
                    InteractionUtilisateur::getTypeInteraction,
                    Collectors.counting()
                ));
            statistiques.put("interactionsParType", interactionsParType);

            // Statistiques temporelles des interactions (derniers 30 jours)
            LocalDateTime il30Jours = LocalDateTime.now().minusDays(30);
            long interactionsRecentes = interactions.stream()
                .filter(i -> i.getDateInteraction() != null && i.getDateInteraction().isAfter(il30Jours))
                .count();
            statistiques.put("interactionsDerniers30Jours", interactionsRecentes);

            // Durée moyenne de consultation (si disponible)
            OptionalDouble dureeMoyenne = interactions.stream()
                .filter(i -> i.getDureeConsultation() != null)
                .mapToInt(InteractionUtilisateur::getDureeConsultation)
                .average();
            statistiques.put("dureeMoyenneConsultation", dureeMoyenne.isPresent() ? dureeMoyenne.getAsDouble() : null);

            // Statistiques de recherche
            statistiques.put("nombreRecherches", recherches.size());

            // Recherches récentes (derniers 30 jours)
            long recherchesRecentes = recherches.stream()
                .filter(r -> r.getDateRecherche() != null && r.getDateRecherche().isAfter(il30Jours))
                .count();
            statistiques.put("recherchesDerniers30Jours", recherchesRecentes);

            // Analyse des patterns de recherche
            Map<String, Long> termesFrequents = recherches.stream()
                .collect(Collectors.groupingBy(
                    HistoriqueRecherche::getTerme,
                    Collectors.counting()
                ));
            statistiques.put("termesRecherchePopulaires",
                termesFrequents.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                    ))
            );

            // Analyse de l'activité par heure de la journée
            Map<Integer, Long> activiteParHeure = interactions.stream()
                .filter(i -> i.getDateInteraction() != null)
                .collect(Collectors.groupingBy(
                    i -> i.getDateInteraction().getHour(),
                    Collectors.counting()
                ));
            statistiques.put("activiteParHeure", activiteParHeure);

            // Tendance d'engagement (évolution du score)
            statistiques.put("tendanceEngagement", calculerTendanceEngagement(interactions, recherches));

        } catch (Exception e) {
            logger.error("Erreur lors du calcul des statistiques détaillées pour l'utilisateur {}: {}", userId, e.getMessage(), e);
            statistiques.put("erreur", "Erreur lors du calcul des statistiques détaillées: " + e.getMessage());
        }

        return statistiques;
    }

    private String calculerTendanceEngagement(List<InteractionUtilisateur> interactions,
                                            List<HistoriqueRecherche> recherches) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime il7Jours = maintenant.minusDays(7);
        LocalDateTime il14Jours = maintenant.minusDays(14);

        // Compter les activités des 7 derniers jours vs 7 jours précédents
        long activiteRecente = interactions.stream()
            .filter(i -> i.getDateInteraction() != null && i.getDateInteraction().isAfter(il7Jours))
            .count();
        activiteRecente += recherches.stream()
            .filter(r -> r.getDateRecherche() != null && r.getDateRecherche().isAfter(il7Jours))
            .count();

        long activitePrecedente = interactions.stream()
            .filter(i -> i.getDateInteraction() != null &&
                    i.getDateInteraction().isAfter(il14Jours) &&
                    i.getDateInteraction().isBefore(il7Jours))
            .count();
        activitePrecedente += recherches.stream()
            .filter(r -> r.getDateRecherche() != null &&
                    r.getDateRecherche().isAfter(il14Jours) &&
                    r.getDateRecherche().isBefore(il7Jours))
            .count();

        if (activitePrecedente == 0) {
            return activiteRecente > 0 ? "croissante" : "stable";
        }

        double ratio = (double) activiteRecente / activitePrecedente;
        if (ratio > 1.2) return "croissante";
        else if (ratio < 0.8) return "décroissante";
        else return "stable";
    }
    
    @Override
    public ComportementUtilisateur analyserPatterns(Long userId) {
        try {
            // Récupérer ou créer le comportement utilisateur
            ComportementUtilisateur comportement = getOrCreateBehavior(userId);
            
            // Récupérer les données nécessaires pour l'analyse
            List<InteractionUtilisateur> interactions = interactionService.getInteractionsByUserId(userId);
            List<HistoriqueRecherche> recherches = historiqueRechercheService.getHistoryByUserId(userId);
            
            // Analyser les patterns et mettre à jour les métriques
            ComportementUtilisateur.MetriquesComportementales metriques = 
                comportement.getMetriques() != null ? 
                comportement.getMetriques() : 
                new ComportementUtilisateur.MetriquesComportementales();
            
            // 1. Patterns temporels
            Map<String, Object> patternsTemporels = analyserPatternsTemporels(interactions, recherches);
            
            // 2. Patterns de navigation  
            Map<String, Object> patternsNavigation = analyserPatternsNavigation(interactions);
            
            // 3. Patterns de recherche
            Map<String, Object> patternsRecherche = analyserPatternsRecherche(recherches);
            
            // 4. Mise à jour du score d'engagement basé sur les patterns
            double scoreEngagement = calculerScoreEngagement(interactions, recherches);
            metriques.setScoreEngagement(scoreEngagement);
            
            // 5. Mise à jour du profil utilisateur
            String profilUtilisateur = determinerProfilUtilisateur(scoreEngagement);
            metriques.setProfilUtilisateur(profilUtilisateur);
            
            // 6. Mise à jour des termes de recherche fréquents
            List<String> termesFrequents = recherches.stream()
                .collect(Collectors.groupingBy(
                    HistoriqueRecherche::getTerme,
                    Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            metriques.setTermesRechercheFrequents(termesFrequents);
            
            // 7. Mise à jour des fréquences d'actions
            Map<String, Integer> frequences = new HashMap<>();
            interactions.forEach(interaction -> {
                String type = interaction.getTypeInteraction();
                frequences.put(type, frequences.getOrDefault(type, 0) + 1);
            });
            metriques.setFrequenceActions(frequences);
            
            // 8. Calcul du taux de recherches fructueuses
            if (!recherches.isEmpty()) {
                long recherchesFructueuses = recherches.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getRechercheFructueuse()))
                    .count();
                double tauxFructueuses = (double) recherchesFructueuses / recherches.size() * 100;
                metriques.setTauxRecherchesFructueuses(tauxFructueuses);
            }
            
            // 9. Mise à jour du nombre total de recherches
            metriques.setNombreRecherchesTotales(recherches.size());
            
            // Sauvegarder les métriques mises à jour
            comportement.setMetriques(metriques);
            
            // Mettre à jour la date de dernière analyse
            comportement.setDateMiseAJour(LocalDateTime.now());
            
            // Sauvegarder et retourner le comportement mis à jour
            return updateBehavior(comportement);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'analyse des patterns pour l'utilisateur {}: {}", userId, e.getMessage(), e);
            
            // En cas d'erreur, retourner le comportement existant ou créer un nouveau
            ComportementUtilisateur comportement = getOrCreateBehavior(userId);
            comportement.setDateMiseAJour(LocalDateTime.now());
            return updateBehavior(comportement);
        }
    }

    @Override
    public AnalysePatternsDTO analyserPatternsDTO(Long userId) {
        Map<String, Object> patterns = new HashMap<>();

        try {
            // Récupérer les données de l'utilisateur
            List<InteractionUtilisateur> interactions = interactionService.getInteractionsByUserId(userId);
            List<HistoriqueRecherche> recherches = historiqueRechercheService.getHistoryByUserId(userId);
            ComportementUtilisateur comportement = getOrCreateBehavior(userId);

            // 1. Patterns temporels
            patterns.put("patternsTemporels", analyserPatternsTemporels(interactions, recherches));

            // 2. Patterns de navigation
            patterns.put("patternsNavigation", analyserPatternsNavigation(interactions));

            // 3. Patterns de recherche
            patterns.put("patternsRecherche", analyserPatternsRecherche(recherches));

            // 4. Patterns de session
            patterns.put("patternsSessions", analyserPatternsSessions(interactions, recherches));

            // 5. Préférences utilisateur
            patterns.put("preferences", analyserPreferences(interactions, recherches));

            // 6. Séquences d'actions fréquentes
            patterns.put("sequencesFrequentes", analyserSequencesActions(interactions));

            // 7. Anomalies comportementales
            patterns.put("anomalies", detecterAnomalies(interactions, recherches, comportement));

            // 8. Score de prédictibilité
            patterns.put("scorePredictibilite", calculerScorePredictibiliteSimplifiee(interactions, recherches));

        } catch (Exception e) {
            logger.error("Erreur lors de l'analyse des patterns pour l'utilisateur {}: {}", userId, e.getMessage(), e);
            patterns.put("erreur", "Erreur lors de l'analyse des patterns: " + e.getMessage());
        }

        // Retourner le DTO au lieu de l'objet AnalysePatterns
        return new AnalysePatternsDTO(patterns);
    }

    /**
     * Analyse les patterns temporels d'activité de l'utilisateur.
     * Inclut l'activité par heure de la journée, par jour de la semaine et les pics d'activité.
     * @param interactions Liste des interactions de l'utilisateur.
     * @param recherches Liste des recherches de l'utilisateur.
     * @return Map contenant les patterns temporels.
     */
    private Map<String, Object> analyserPatternsTemporels(List<InteractionUtilisateur> interactions,
                                                          List<HistoriqueRecherche> recherches) {
        Map<String, Object> patternsTemporels = new HashMap<>();

        // Activité par heure de la journée
        Map<Integer, Long> activiteParHeure = new HashMap<>();
        interactions.stream()
            .filter(i -> i.getDateInteraction() != null)
            .forEach(i -> activiteParHeure.merge(i.getDateInteraction().getHour(), 1L, Long::sum));
        recherches.stream()
            .filter(r -> r.getDateRecherche() != null)
            .forEach(r -> activiteParHeure.merge(r.getDateRecherche().getHour(), 1L, Long::sum));
        patternsTemporels.put("activiteParHeure", activiteParHeure);

        // Activité par jour de la semaine
        Map<DayOfWeek, Long> activiteParJourSemaine = new HashMap<>();
        interactions.stream()
            .filter(i -> i.getDateInteraction() != null)
            .forEach(i -> activiteParJourSemaine.merge(i.getDateInteraction().getDayOfWeek(), 1L, Long::sum));
        recherches.stream()
            .filter(r -> r.getDateRecherche() != null)
            .forEach(r -> activiteParJourSemaine.merge(r.getDateRecherche().getDayOfWeek(), 1L, Long::sum));
        patternsTemporels.put("activiteParJourSemaine", activiteParJourSemaine.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue)));

        // Pics d'activité (heure et jour les plus actifs)
        Optional<Map.Entry<Integer, Long>> heurePic = activiteParHeure.entrySet().stream()
            .max(Map.Entry.comparingByValue());
        heurePic.ifPresent(entry -> patternsTemporels.put("heurePicActivite", entry.getKey()));

        Optional<Map.Entry<DayOfWeek, Long>> jourPic = activiteParJourSemaine.entrySet().stream()
            .max(Map.Entry.comparingByValue());
        jourPic.ifPresent(entry -> patternsTemporels.put("jourPicActivite", entry.getKey().toString()));


        return patternsTemporels;
    }

    /**
     * Analyse les patterns de navigation de l'utilisateur.
     * Inclut les types d'interaction fréquents, les entités consultées et les parcours typiques.
     * @param interactions Liste des interactions de l'utilisateur.
     * @return Map contenant les patterns de navigation.
     */
    private Map<String, Object> analyserPatternsNavigation(List<InteractionUtilisateur> interactions) {
        Map<String, Object> patternsNavigation = new HashMap<>();

        // Types d'interaction fréquents
        Map<String, Long> typesInteractionFrequents = interactions.stream()
            .collect(Collectors.groupingBy(
                InteractionUtilisateur::getTypeInteraction,
                Collectors.counting()
            ));
        patternsNavigation.put("typesInteractionFrequents", typesInteractionFrequents.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            )));

        // Entités les plus consultées (si entiteInteraction est rempli)
        Map<String, Long> entitesConsulteesFrequentes = interactions.stream()
        	    .filter(i -> i.getEntiteId() != null) 
        	    .collect(Collectors.groupingBy(
        	        i -> i.getEntiteId().toString(), 
        	        Collectors.counting()
        	    ));
        patternsNavigation.put("entitesConsulteesFrequentes", entitesConsulteesFrequentes.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            )));

        // Durée moyenne de consultation par type d'interaction
        Map<String, Double> dureeMoyenneParType = interactions.stream()
            .filter(i -> i.getDureeConsultation() != null)
            .collect(Collectors.groupingBy(
                InteractionUtilisateur::getTypeInteraction,
                Collectors.averagingDouble(InteractionUtilisateur::getDureeConsultation)
            ));
        patternsNavigation.put("dureeMoyenneConsultationParType", dureeMoyenneParType);

        return patternsNavigation;
    }

    /**
     * Analyse les patterns de recherche de l'utilisateur.
     * Inclut les termes de recherche fréquents, l'utilisation des filtres et le taux de succès.
     * @param recherches Liste des recherches de l'utilisateur.
     * @return Map contenant les patterns de recherche.
     */
    private Map<String, Object> analyserPatternsRecherche(List<HistoriqueRecherche> recherches) {
        Map<String, Object> patternsRecherche = new HashMap<>();

        // Termes de recherche fréquents (déjà calculé dans updateMetrics, mais ici pour l'analyse spécifique)
        patternsRecherche.put("termesRechercheFrequents", recherches.stream()
            .collect(Collectors.groupingBy(
                HistoriqueRecherche::getTerme,
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList()));

        // Utilisation des filtres
        Map<String, Long> utilisationFiltres = recherches.stream()
            .flatMap(r -> r.getFiltres() != null ? r.getFiltres().stream() : null)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                HistoriqueRecherche.Filtre::getNom,
                Collectors.counting()
            ));
        patternsRecherche.put("utilisationFiltres", utilisationFiltres);

        // Taux de recherches fructueuses
        long recherchesFructueuses = recherches.stream()
            .filter(r -> Boolean.TRUE.equals(r.getRechercheFructueuse()))
            .count();
        double tauxFructueuses = recherches.isEmpty() ? 0.0 :
            (double) recherchesFructueuses / recherches.size() * 100;
        patternsRecherche.put("tauxRecherchesFructueuses", tauxFructueuses);

        return patternsRecherche;
    }

    /**
     * Analyse les patterns de session de l'utilisateur.
     * Inclut la durée moyenne des sessions et le nombre d'interactions par session.
     * Note: La notion de "session" doit être définie (ex: interactions espacées de moins de X minutes).
     * Pour cet exemple, nous allons simplifier en considérant des sessions basées sur l'intervalle temporel.
     * @param interactions Liste des interactions de l'utilisateur.
     * @param recherches Liste des recherches de l'utilisateur.
     * @return Map contenant les patterns de session.
     */
    private Map<String, Object> analyserPatternsSessions(List<InteractionUtilisateur> interactions,
                                                        List<HistoriqueRecherche> recherches) {
        Map<String, Object> patternsSessions = new HashMap<>();

        // Combiner et trier toutes les activités par date
        List<LocalDateTime> toutesActivites = new ArrayList<>();
        interactions.stream()
            .filter(i -> i.getDateInteraction() != null)
            .map(InteractionUtilisateur::getDateInteraction)
            .forEach(toutesActivites::add);
        recherches.stream()
            .filter(r -> r.getDateRecherche() != null)
            .map(HistoriqueRecherche::getDateRecherche)
            .forEach(toutesActivites::add);
        toutesActivites.sort(Comparator.naturalOrder());

        if (toutesActivites.isEmpty()) {
            patternsSessions.put("dureeMoyenneSessionMinutes", 0.0);
            patternsSessions.put("nombreActivitesParSession", 0.0);
            return patternsSessions;
        }

        // Définir une session par un intervalle de temps (ex: 30 minutes sans activité)
        long SESSION_INACTIVITY_THRESHOLD_MINUTES = 30;
        List<Double> dureesSessions = new ArrayList<>();
        List<Integer> activitesParSession = new ArrayList<>();

        if (!toutesActivites.isEmpty()) {
            LocalDateTime debutSession = toutesActivites.get(0);
            int countActivites = 1;

            for (int i = 1; i < toutesActivites.size(); i++) {
                LocalDateTime prevTime = toutesActivites.get(i - 1);
                LocalDateTime currentTime = toutesActivites.get(i);

                if (java.time.Duration.between(prevTime, currentTime).toMinutes() > SESSION_INACTIVITY_THRESHOLD_MINUTES) {
                    // Nouvelle session
                    dureesSessions.add((double) java.time.Duration.between(debutSession, prevTime).toMinutes());
                    activitesParSession.add(countActivites);
                    debutSession = currentTime;
                    countActivites = 1;
                } else {
                    countActivites++;
                }
            }
            // Ajouter la dernière session
            dureesSessions.add((double) java.time.Duration.between(debutSession, toutesActivites.get(toutesActivites.size() - 1)).toMinutes());
            activitesParSession.add(countActivites);
        }

        patternsSessions.put("dureeMoyenneSessionMinutes", dureesSessions.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        patternsSessions.put("nombreActivitesParSession", activitesParSession.stream().mapToDouble(Integer::doubleValue).average().orElse(0.0));

        return patternsSessions;
    }

    /**
     * Analyse les préférences générales de l'utilisateur, basées sur les interactions et recherches.
     * @param interactions Liste des interactions de l'utilisateur.
     * @param recherches Liste des recherches de l'utilisateur.
     * @return Map contenant les préférences utilisateur.
     */
    private Map<String, Object> analyserPreferences(List<InteractionUtilisateur> interactions,
                                                    List<HistoriqueRecherche> recherches) {
        Map<String, Object> preferences = new HashMap<>();

        // Préférences d'entités/catégories (basées sur entiteInteraction)
        Map<String, Long> entitesPreferees = interactions.stream()
        	    .filter(i -> i.getEntiteId() != null) 
        	    .collect(Collectors.groupingBy(
        	        i -> i.getEntiteId().toString(), 
        	        Collectors.counting()
        	    ));
        preferences.put("entitesPreferees", entitesPreferees.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            )));

        // Préférences de filtres de recherche
        Map<String, Long> filtresPreferes = recherches.stream()
            .flatMap(r -> r.getFiltres() != null ? r.getFiltres().stream() : null)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                HistoriqueRecherche.Filtre::getNom,
                Collectors.counting()
            ));
        preferences.put("filtresPreferes", filtresPreferes.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            )));

        // Type d'interaction dominant
        Optional<Map.Entry<String, Long>> typeInteractionDominant = interactions.stream()
            .collect(Collectors.groupingBy(
                InteractionUtilisateur::getTypeInteraction,
                Collectors.counting()
            ))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue());
        typeInteractionDominant.ifPresent(entry -> preferences.put("typeInteractionDominant", entry.getKey()));

        return preferences;
    }

    /**
     * Analyse les séquences d'actions fréquentes de l'utilisateur.
     * Ex: Recherche -> Clic sur résultat -> Ajout aux favoris.
     * Cette implémentation est simplifiée et se concentrera sur des paires d'actions.
     * Pour des séquences plus complexes (séquences de Markov, etc.), une librairie dédiée ou un algorithme plus robuste serait nécessaire.
     * @param interactions Liste des interactions de l'utilisateur.
     * @return Liste de chaînes de caractères représentant les séquences fréquentes.
     */
    private List<String> analyserSequencesActions(List<InteractionUtilisateur> interactions) {
        List<String> sequencesFrequentes = new ArrayList<>();

        if (interactions.size() < 2) {
            return sequencesFrequentes;
        }

        // Tri des interactions par date
        List<InteractionUtilisateur> sortedInteractions = interactions.stream()
            .filter(i -> i.getDateInteraction() != null)
            .sorted(Comparator.comparing(InteractionUtilisateur::getDateInteraction))
            .collect(Collectors.toList());

        Map<String, Long> pairesActions = new HashMap<>();

        for (int i = 0; i < sortedInteractions.size() - 1; i++) {
            InteractionUtilisateur current = sortedInteractions.get(i);
            InteractionUtilisateur next = sortedInteractions.get(i + 1);

            // Considérer des actions consécutives dans une courte fenêtre de temps (ex: 5 minutes)
            if (java.time.Duration.between(current.getDateInteraction(), next.getDateInteraction()).toMinutes() <= 5) {
                String paire = current.getTypeInteraction() + " -> " + next.getTypeInteraction();
                pairesActions.merge(paire, 1L, Long::sum);
            }
        }

        // Sélectionner les 5 paires d'actions les plus fréquentes
        sequencesFrequentes = pairesActions.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        return sequencesFrequentes;
    }

    /**
     * Détecte les anomalies comportementales chez l'utilisateur.
     * Ceci est un exemple simplifié. Des modèles de ML seraient nécessaires pour une détection robuste.
     * Exemples d'anomalies : pic soudain d'activité, activité en dehors des heures habituelles, recherches inhabituelles.
     * @param interactions Liste des interactions de l'utilisateur.
     * @param recherches Liste des recherches de l'utilisateur.
     * @param comportement L'objet ComportementUtilisateur.
     * @return Liste de chaînes de caractères décrivant les anomalies détectées.
     */
    private List<String> detecterAnomalies(List<InteractionUtilisateur> interactions,
                                           List<HistoriqueRecherche> recherches,
                                           ComportementUtilisateur comportement) {
        List<String> anomalies = new ArrayList<>();

        // Anomalie 1: Pic d'activité inhabituel (par rapport à la moyenne)
        double moyenneActiviteParJour = (interactions.size() + recherches.size()) / (double) java.time.Duration.between(comportement.getDateCreation(), LocalDateTime.now()).toDays();
        if (moyenneActiviteParJour == 0) moyenneActiviteParJour = 1.0; // Évite la division par zéro

        long activiteDernierJour = interactions.stream()
            .filter(i -> i.getDateInteraction() != null && i.getDateInteraction().toLocalDate().isEqual(LocalDateTime.now().toLocalDate()))
            .count();
        activiteDernierJour += recherches.stream()
            .filter(r -> r.getDateRecherche() != null && r.getDateRecherche().toLocalDate().isEqual(LocalDateTime.now().toLocalDate()))
            .count();

        if (activiteDernierJour > (moyenneActiviteParJour * 3)) { // 3x la moyenne quotidienne
            anomalies.add("Pic d'activité inhabituel détecté aujourd'hui.");
        }

        // Anomalie 2: Activité en dehors des heures habituelles (très tôt le matin, très tard la nuit)
        Map<Integer, Long> activiteParHeure = new HashMap<>();
        interactions.stream()
            .filter(i -> i.getDateInteraction() != null)
            .forEach(i -> activiteParHeure.merge(i.getDateInteraction().getHour(), 1L, Long::sum));
        recherches.stream()
            .filter(r -> r.getDateRecherche() != null)
            .forEach(r -> activiteParHeure.merge(r.getDateRecherche().getHour(), 1L, Long::sum));

        long activiteNuit = activiteParHeure.getOrDefault(0, 0L) + activiteParHeure.getOrDefault(1, 0L) +
                            activiteParHeure.getOrDefault(2, 0L) + activiteParHeure.getOrDefault(3, 0L) +
                            activiteParHeure.getOrDefault(4, 0L) + activiteParHeure.getOrDefault(5, 0L);
        long totalActivite = activiteParHeure.values().stream().mapToLong(Long::longValue).sum();

        if (totalActivite > 0 && (double) activiteNuit / totalActivite > 0.2) { // Plus de 20% de l'activité la nuit
            anomalies.add("Activité significative détectée en dehors des heures normales (nuit).");
        }

        // Anomalie 3: Terme de recherche totalement nouveau et potentiellement sensible (dépend du contexte métier)
        // Ceci est un placeholder, nécessiterait une base de données de termes "normaux" ou une analyse NLP
        // Exemple très simple : si le terme de recherche n'est pas dans les termes fréquents et contient certains mots-clés
        List<String> termesFrequents = comportement.getMetriques() != null ? comportement.getMetriques().getTermesRechercheFrequents() : new ArrayList<>();
        Optional<HistoriqueRecherche> derniereRecherche = recherches.stream()
            .max(Comparator.comparing(HistoriqueRecherche::getDateRecherche));

        if (derniereRecherche.isPresent() && !termesFrequents.contains(derniereRecherche.get().getTerme())) {
            // Ici, vous pourriez ajouter une logique plus complexe pour vérifier la "sensibilité" ou "l'anormalité" du terme
            anomalies.add("Nouveau terme de recherche inhabituel détecté: '" + derniereRecherche.get().getTerme() + "'.");
        }


        return anomalies;
    }

    /**
     * Calcule un score de prédictibilité du comportement de l'utilisateur.
     * Un score élevé indique que le comportement de l'utilisateur est prévisible (suit des patterns).
     * Un score faible indique un comportement plus aléatoire ou nouveau.
     * Ceci est un calcul heuristique.
     * @param interactions Liste des interactions de l'utilisateur.
     * @param recherches Liste des recherches de l'utilisateur.
     * @return Score de prédictibilité (0-100).
     */
 // Méthodes utilitaires à ajouter dans votre classe ComportementUtilisateurServiceImpl

    /**
     * Convertit de manière sécurisée un Object en Map<String, Long>
     * @param obj L'objet à convertir
     * @return Map<String, Long> ou une map vide si la conversion échoue
     */
    @SuppressWarnings("unchecked")
    private Map<String, Long> safeConvertToStringLongMap(Object obj) {
        Map<String, Long> result = new HashMap<>();
        if (obj instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof Long) {
                    result.put((String) entry.getKey(), (Long) entry.getValue());
                }
            }
        }
        return result;
    }

    /**
     * Convertit de manière sécurisée un Object en Map<Integer, Long>
     * @param obj L'objet à convertir
     * @return Map<Integer, Long> ou une map vide si la conversion échoue
     */
    @SuppressWarnings("unchecked")
    private Map<Integer, Long> safeConvertToIntegerLongMap(Object obj) {
        Map<Integer, Long> result = new HashMap<>();
        if (obj instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof Integer && entry.getValue() instanceof Long) {
                    result.put((Integer) entry.getKey(), (Long) entry.getValue());
                }
            }
        }
        return result;
    }

    // Version simplifiée de calculerScorePredictibilite avec les méthodes utilitaires
    private Double calculerScorePredictibiliteSimplifiee(List<InteractionUtilisateur> interactions,
                                                         List<HistoriqueRecherche> recherches) {
        double score = 0.0;
        int maxScore = 100;

        // Pondération de la cohérence des patterns temporels
        Map<String, Object> patternsTemporels = analyserPatternsTemporels(interactions, recherches);
        Map<Integer, Long> activiteParHeure = safeConvertToIntegerLongMap(patternsTemporels.get("activiteParHeure"));

        // Correction : calcul correct de l'écart-type
        if (!activiteParHeure.isEmpty()) {
            double moyenne = activiteParHeure.values().stream()
                .mapToDouble(Long::doubleValue)
                .average()
                .orElse(0.0);
            
            double variance = activiteParHeure.values().stream()
                .mapToDouble(val -> Math.pow(val - moyenne, 2))
                .average()
                .orElse(0.0);
            
            double ecartType = Math.sqrt(variance);
            score += Math.max(0, 10 - ecartType);
        }

        // Récupération de l'userId
        Long userId = null;
        if (!interactions.isEmpty()) {
            userId = interactions.get(0).getUserId();
        } else if (!recherches.isEmpty()) {
            userId = recherches.get(0).getUserId();
        }
        
        if (userId != null) {
            List<String> termesRechercheFrequents = getFrequentSearchTerms(userId);
            if (termesRechercheFrequents.size() >= 5) {
                score += 15;
            } else if (termesRechercheFrequents.size() >= 2) {
                score += 5;
            }
        }

        // Pondération de la cohérence des types d'interaction
        Map<String, Object> patternsNavigation = analyserPatternsNavigation(interactions);
        Map<String, Long> typesInteractionFrequents = safeConvertToStringLongMap(patternsNavigation.get("typesInteractionFrequents"));
        
        if (!typesInteractionFrequents.isEmpty()) {
            Optional<Long> maxCount = typesInteractionFrequents.values().stream().max(Long::compare);
            long totalInteractions = interactions.size();
            
            if (maxCount.isPresent() && totalInteractions > 0) {
                double dominance = (double) maxCount.get() / totalInteractions;
                score += dominance * 20;
            }
        }

        // Pondération des séquences d'actions fréquentes
        List<String> sequencesFrequentes = analyserSequencesActions(interactions);
        score += sequencesFrequentes.size() * 5;

        // Inversement proportionnel aux anomalies
        if (userId != null) {
            ComportementUtilisateur comportement = getOrCreateBehavior(userId);
            List<String> anomalies = detecterAnomalies(interactions, recherches, comportement);
            score -= anomalies.size() * 10;
        }

        return Math.max(0, Math.min(maxScore, score));
    }
}