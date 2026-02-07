package com.example.demo.servicesImplMongoDB;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.example.demo.DTO.AnalysePatternsDTO;
import com.example.demo.entiesMongodb.*;
import com.example.demo.entiesMongodb.enums.ProfilUtilisateur;
import com.example.demo.entiesMongodb.enums.Saison;
import com.example.demo.repositoryMongoDB.ComportementUtilisateurRepository;
import com.example.demo.servicesMongoDB.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ComportementUtilisateurServiceImpl implements ComportementUtilisateurService {

    private static final Logger logger = LoggerFactory.getLogger(ComportementUtilisateurServiceImpl.class);

    private final ComportementUtilisateurRepository comportementUtilisateurRepository;
    private final InteractionUtilisateurService interactionService;
    private final HistoriqueRechercheService historiqueRechercheService;
    private final EnhancedComportementAnalysisService enhancedAnalysisService;
    
    private static final int MAX_HISTORIQUE_RECHERCHES = 500;
    private static final int MAX_HISTORIQUE_INTERACTIONS = 1000;
    private static final double SCORE_ENGAGEMENT_FIDELE = 70.0;
    private static final double SCORE_ENGAGEMENT_ACTIF = 40.0;
    private static final double SCORE_ENGAGEMENT_OCCASIONNEL = 15.0;
    private static final long SESSION_INACTIVITY_THRESHOLD_MINUTES = 30; // Seuil pour définir une session

    public ComportementUtilisateurServiceImpl(ComportementUtilisateurRepository comportementUtilisateurRepository,
                                            InteractionUtilisateurService interactionService,
                                            HistoriqueRechercheService historiqueRechercheService, EnhancedComportementAnalysisService enhancedAnalysisService) {
        this.comportementUtilisateurRepository = comportementUtilisateurRepository;
        this.interactionService = interactionService;
        this.historiqueRechercheService = historiqueRechercheService;
        this.enhancedAnalysisService = enhancedAnalysisService;
    }

    @Override
    public ComportementUtilisateur createBehavior(Long userId) {
        ComportementUtilisateur comportement = new ComportementUtilisateur();
        comportement.setUserId(userId);
        comportement.setDateCreation(LocalDateTime.now());
        comportement.setDateMiseAJour(LocalDateTime.now());
        comportement.setHistoriqueInteractionsIds(new ArrayList<>());
        comportement.setHistoriqueRecherchesIds(new ArrayList<>());

        // Initialisation des métriques
        ComportementUtilisateur.MetriquesComportementales metriques =
            new ComportementUtilisateur.MetriquesComportementales();
        metriques.setNombreFavorisTotal(0);
        metriques.setNombreCommentairesLaisses(0);
        metriques.setScoreEngagement(0.0);
        metriques.setProfilUtilisateur(ProfilUtilisateur.NOUVEAU); 
        metriques.setFrequenceActions(new HashMap<>());
        metriques.setScoreRecommandation(0.0);
        metriques.setTendancesTemporelles(new HashMap<>());
        metriques.setStreakConnexion(0);
        metriques.setDerniereActivite(LocalDateTime.now());
        comportement.setMetriques(metriques);

        // Initialisation des préférences saisonnières
        ComportementUtilisateur.PreferencesSaisonnieres preferencesSaisonnieres =
                new ComportementUtilisateur.PreferencesSaisonnieres();
            preferencesSaisonnieres.setIngredientsPrintemps(new ArrayList<>());
            preferencesSaisonnieres.setIngredientsEte(new ArrayList<>());
            preferencesSaisonnieres.setIngredientsAutomne(new ArrayList<>());
            preferencesSaisonnieres.setIngredientsHiver(new ArrayList<>());
            preferencesSaisonnieres.setSaisonPreferee(null);
            preferencesSaisonnieres.setScoresPreferenceSaisonniere(new HashMap<>());
            preferencesSaisonnieres.setDerniereMiseAJour(LocalDateTime.now());
            comportement.setPreferencesSaisonnieres(preferencesSaisonnieres);

            ComportementUtilisateur.HabitudesNavigation habitudesNavigation =
                new ComportementUtilisateur.HabitudesNavigation();
            habitudesNavigation.setPagesVisitees(new HashMap<>());
            habitudesNavigation.setTempsParPage(new HashMap<>());
            habitudesNavigation.setRecherchesFavorites(new ArrayList<>());
            habitudesNavigation.setTypeRecettePreferee(null);
            habitudesNavigation.setNombreConnexionsParJour(0);
            habitudesNavigation.setHeuresConnexionHabituelles(new ArrayList<>());
            habitudesNavigation.setParcoursFavoris(new HashMap<>());
            habitudesNavigation.setTempsMoyenParSession(0.0);
            habitudesNavigation.setNombrePagesParSession(0);
            habitudesNavigation.setCategoriesPreferees(new ArrayList<>());
            habitudesNavigation.setFrequenceParCategorie(new HashMap<>());
            comportement.setHabitudesNavigation(habitudesNavigation);

            ComportementUtilisateur.CyclesActivite cyclesActivite =
                new ComportementUtilisateur.CyclesActivite();
            cyclesActivite.setPetitDejeuner(new ComportementUtilisateur.CreneauRepas(null, null, new ArrayList<>(), 0, false, 0.0, new ArrayList<>(), new HashMap<>()));
            cyclesActivite.setDejeuner(new ComportementUtilisateur.CreneauRepas(null, null, new ArrayList<>(), 0, false, 0.0, new ArrayList<>(), new HashMap<>()));
            cyclesActivite.setDiner(new ComportementUtilisateur.CreneauRepas(null, null, new ArrayList<>(), 0, false, 0.0, new ArrayList<>(), new HashMap<>()));
            cyclesActivite.setActiviteParJour(new HashMap<>());
            cyclesActivite.setJoursActifs(new ArrayList<>());
            cyclesActivite.setActivitesParCreneau(new HashMap<>());
            cyclesActivite.setCreneauLePlusActif(null);
            cyclesActivite.setConsistanceHoraire(0.0);
            comportement.setCyclesActivite(cyclesActivite);

        return comportementUtilisateurRepository.save(comportement);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR') or (#userId == authentication.principal.id)")
    public Optional<ComportementUtilisateur> getBehaviorByUserId(Long userId) {
        return comportementUtilisateurRepository.findByUserId(userId);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR') or (#userId == authentication.principal.id)")
    public ComportementUtilisateur getOrCreateBehavior(Long userId) {
        return getBehaviorByUserId(userId)
            .orElseGet(() -> createBehavior(userId));
    }

    @Override
    public ComportementUtilisateur updateBehavior(ComportementUtilisateur comportement) {
        comportement.setDateMiseAJour(LocalDateTime.now());
        return comportementUtilisateurRepository.save(comportement);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR')")
    public void updateMetrics(Long userId) {
        ComportementUtilisateur comportement = getOrCreateBehavior(userId);
        ComportementUtilisateur.MetriquesComportementales metriques =
            comportement.getMetriques() != null ?
            comportement.getMetriques() :
            new ComportementUtilisateur.MetriquesComportementales();

        List<InteractionUtilisateur> interactions = interactionService.getInteractionsByUserId(userId);
        List<HistoriqueRecherche> recherches = historiqueRechercheService.getHistoryByUserId(userId);

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

        long recherchesFructueuses = recherches.stream()
            .filter(r -> Boolean.TRUE.equals(r.getRechercheFructueuse()))
            .count();
        double tauxFructueuses = recherches.isEmpty() ? 0.0 :
            (double) recherchesFructueuses / recherches.size() * 100;
        metriques.setTauxRecherchesFructueuses(tauxFructueuses);

        metriques.setScoreEngagement(calculerScoreEngagement(interactions, recherches));
        metriques.setProfilUtilisateur(determinerProfilUtilisateur(metriques.getScoreEngagement())); 

        Map<String, Integer> frequences = new HashMap<>();
        interactions.forEach(interaction -> {
            String type = interaction.getTypeInteraction();
            frequences.put(type, frequences.getOrDefault(type, 0) + 1);
        });
        metriques.setFrequenceActions(frequences);

        comportement.setHistoriqueInteractionsIds(
            interactions.stream().map(InteractionUtilisateur::getId).collect(Collectors.toList())
        );
        comportement.setHistoriqueRecherchesIds(
            recherches.stream().map(HistoriqueRecherche::getId).collect(Collectors.toList())
        );

        comportement.setMetriques(metriques);
        updateBehavior(comportement);
    }
    
    /**
    * ✅ NOUVELLE MÉTHODE : Analyse comportementale complète avec analytics avancés
    * Utilise le service enrichi pour fournir des insights supplémentaires
    */
   public Map<String, Object> analyserComportementAvance(Long userId) {
       try {
           // 1. Exécuter l'analyse de base (votre code existant)
           ComportementUtilisateur comportement = getOrCreateBehavior(userId);
           
           // 2. Exécuter l'analyse avancée
           Map<String, Object> analyseAvancee = enhancedAnalysisService
               .analyseComportementaleAvancee(userId);
           
           // 3. Fusionner avec les données de base
           Map<String, Object> resultatComplet = new HashMap<>();
           resultatComplet.put("comportementBase", comportement);
           resultatComplet.put("analyticsAvances", analyseAvancee);
           
           return resultatComplet;
           
       } catch (Exception e) {
           logger.error("Erreur analyse avancée pour userId {}: {}", userId, e.getMessage());
           return Map.of("erreur", e.getMessage());
       }
   }
   
   /**
    * ✅ NOUVELLE MÉTHODE : Obtenir le score de risque de churn
    */
   public Map<String, Object> obtenirRisqueChurn(Long userId) {
       Map<String, Object> analyse = enhancedAnalysisService.analyseComportementaleAvancee(userId);
       return (Map<String, Object>) analyse.getOrDefault("risqueChurn", new HashMap<>());
   }
   
   /**
    * ✅ NOUVELLE MÉTHODE : Obtenir la segmentation RFM
    */
   public Map<String, Object> obtenirSegmentRFM(Long userId) {
       Map<String, Object> analyse = enhancedAnalysisService.analyseComportementaleAvancee(userId);
       return (Map<String, Object>) analyse.getOrDefault("scoreRFM", new HashMap<>());
   }
   
   /**
    * ✅ NOUVELLE MÉTHODE : Obtenir les actions d'engagement recommandées
    */
   public List<Map<String, String>> obtenirActionsEngagement(Long userId) {
       Map<String, Object> analyse = enhancedAnalysisService.analyseComportementaleAvancee(userId);
       return (List<Map<String, String>>) analyse.getOrDefault("actionsEngagement", new ArrayList<>());
   }


    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR') or (#userId == authentication.principal.id)")
    public ComportementUtilisateur recordSearch(Long userId, String terme,
                                                List<HistoriqueRecherche.Filtre> filtres,
                                                Integer nombreResultats,
                                                Boolean rechercheFructueuse) {
        HistoriqueRecherche nouvelleRecherche = historiqueRechercheService.recordSearch(userId, terme, filtres);

        ComportementUtilisateur comportement = getOrCreateBehavior(userId);

        if (comportement.getHistoriqueRecherchesIds() == null) {
            comportement.setHistoriqueRecherchesIds(new ArrayList<>());
        }

        comportement.getHistoriqueRecherchesIds().add(nouvelleRecherche.getId());

        if (comportement.getHistoriqueRecherchesIds().size() > MAX_HISTORIQUE_RECHERCHES) {
            comportement.getHistoriqueRecherchesIds().remove(0);
        }

        return updateBehavior(comportement);
    }

    @Override
    public ComportementUtilisateur enregistrerInteraction(Long userId, String typeInteraction,
                                                        String entiteInteraction, String detailsInteraction) {
        InteractionUtilisateur nouvelleInteraction = interactionService.addInteractionUtilisateur(
            userId, typeInteraction, null, null); 

        ComportementUtilisateur comportement = getOrCreateBehavior(userId);

        if (comportement.getHistoriqueInteractionsIds() == null) {
            comportement.setHistoriqueInteractionsIds(new ArrayList<>());
        }

        comportement.getHistoriqueInteractionsIds().add(nouvelleInteraction.getId());

        if (comportement.getHistoriqueInteractionsIds().size() > MAX_HISTORIQUE_INTERACTIONS) {
            comportement.getHistoriqueInteractionsIds().remove(0);
        }

        ComportementUtilisateur.MetriquesComportementales metriques =
            comportement.getMetriques() != null ?
            comportement.getMetriques() :
            new ComportementUtilisateur.MetriquesComportementales();

        Map<String, Integer> frequences = metriques.getFrequenceActions();
        if (frequences == null) {
            frequences = new HashMap<>();
        }
        frequences.put(typeInteraction, frequences.getOrDefault(typeInteraction, 0) + 1);
        metriques.setFrequenceActions(frequences);

        List<InteractionUtilisateur> interactions = interactionService.getInteractionsByUserId(userId);
        List<HistoriqueRecherche> recherches = historiqueRechercheService.getHistoryByUserId(userId);
        metriques.setScoreEngagement(calculerScoreEngagement(interactions, recherches));
        metriques.setProfilUtilisateur(determinerProfilUtilisateur(metriques.getScoreEngagement())); 

        comportement.setMetriques(metriques);

        return updateBehavior(comportement);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR') or (#userId == authentication.principal.id)")
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
    public List<ComportementUtilisateur> getUsersByProfile(ProfilUtilisateur profil) {
        return comportementUtilisateurRepository.findByMetriques_ProfilUtilisateur(profil);
    }


    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR')")
    public List<ComportementUtilisateur> getEngagedUsers(Double scoreMinimum) {
        return comportementUtilisateurRepository.findByMetriques_ScoreEngagementGreaterThan(scoreMinimum);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR')")
    public void deleteUserBehavior(Long userId) {
    	comportementUtilisateurRepository.deleteByUserId(userId);
    }

    private Double calculerScoreEngagement(List<InteractionUtilisateur> interactions,
                                         List<HistoriqueRecherche> recherches) {
        double score = 0.0;

        score += interactions.size() * 0.1;
        score += recherches.size() * 0.05;

        long recherchesFructueuses = recherches.stream()
            .filter(r -> Boolean.TRUE.equals(r.getRechercheFructueuse()))
            .count();
        score += recherchesFructueuses * 0.1;

        return Math.min(100.0, score);
    }

    private ProfilUtilisateur determinerProfilUtilisateur(Double scoreEngagement) { 
        if (scoreEngagement == null) return ProfilUtilisateur.NOUVEAU;

        if (scoreEngagement > SCORE_ENGAGEMENT_FIDELE) return ProfilUtilisateur.FIDELE;
        else if (scoreEngagement > SCORE_ENGAGEMENT_ACTIF) return ProfilUtilisateur.ACTIF;
        else if (scoreEngagement > SCORE_ENGAGEMENT_OCCASIONNEL) return ProfilUtilisateur.OCCASIONNEL;
        else return ProfilUtilisateur.DEBUTANT;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATEUR')")
    public Map<String, Object> obtenirStatistiquesComportement(Long userId) {
        ComportementUtilisateur comportement = getOrCreateBehavior(userId);
        Map<String, Object> statistiques = new HashMap<>();

        statistiques.put("userId", userId);
        statistiques.put("dateCreation", comportement.getDateCreation());
        statistiques.put("dateMiseAJour", comportement.getDateMiseAJour());

        ComportementUtilisateur.MetriquesComportementales metriques = comportement.getMetriques();
        if (metriques != null) {
            statistiques.put("scoreEngagement", metriques.getScoreEngagement());
            statistiques.put("profilUtilisateur", metriques.getProfilUtilisateur() != null ? metriques.getProfilUtilisateur().name() : "N/A"); 
            statistiques.put("nombreFavorisTotal", metriques.getNombreFavorisTotal());
            statistiques.put("nombreCommentairesLaisses", metriques.getNombreCommentairesLaisses());
            statistiques.put("noteMoyenneDonnee", metriques.getNoteMoyenneDonnee());
            statistiques.put("nombreRecherchesTotales", metriques.getNombreRecherchesTotales());
            statistiques.put("tauxRecherchesFructueuses", metriques.getTauxRecherchesFructueuses());
            statistiques.put("termesRechercheFrequents", metriques.getTermesRechercheFrequents());
            statistiques.put("frequenceActions", metriques.getFrequenceActions());
        }

        try {
            List<InteractionUtilisateur> interactions = interactionService.getInteractionsByUserId(userId);
            List<HistoriqueRecherche> recherches = historiqueRechercheService.getHistoryByUserId(userId);

            statistiques.put("nombreInteractionsTotal", interactions.size());

            Map<String, Long> interactionsParType = interactions.stream()
                .collect(Collectors.groupingBy(
                    InteractionUtilisateur::getTypeInteraction,
                    Collectors.counting()
                ));
            statistiques.put("interactionsParType", interactionsParType);

            LocalDateTime il30Jours = LocalDateTime.now().minusDays(30);
            long interactionsRecentes = interactions.stream()
                .filter(i -> i.getDateInteraction() != null && i.getDateInteraction().isAfter(il30Jours))
                .count();
            statistiques.put("interactionsDerniers30Jours", interactionsRecentes);

            OptionalDouble dureeMoyenne = interactions.stream()
                .filter(i -> i.getDureeConsultation() != null)
                .mapToInt(InteractionUtilisateur::getDureeConsultation)
                .average();
            statistiques.put("dureeMoyenneConsultation", dureeMoyenne.isPresent() ? dureeMoyenne.getAsDouble() : null);

            statistiques.put("nombreRecherches", recherches.size());

            long recherchesRecentes = recherches.stream()
                .filter(r -> r.getDateRecherche() != null && r.getDateRecherche().isAfter(il30Jours))
                .count();
            statistiques.put("recherchesDerniers30Jours", recherchesRecentes);

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

            Map<Integer, Long> activiteParHeure = interactions.stream()
                .filter(i -> i.getDateInteraction() != null)
                .collect(Collectors.groupingBy(
                    i -> i.getDateInteraction().getHour(),
                    Collectors.counting()
                ));
            statistiques.put("activiteParHeure", activiteParHeure);

            statistiques.put("tendanceEngagement", calculerTendanceEngagement(interactions, recherches));
            
            Map<String, Object> statsAvancees = enhancedAnalysisService.analyseComportementaleAvancee(userId);
            statistiques.put("analyticsAvances", statsAvancees);

        } catch (Exception e) {
            logger.error("Erreur lors du calcul des statistiques détaillées pour l'utilisateur {}: {}", userId, e.getMessage(), e);
            statistiques.put("erreur", "Erreur lors du calcul des statistiques détaillées: " + e.getMessage());
            logger.warn("Stats avancées non disponibles pour userId {}", userId);
        }

        return statistiques;
    }

    private String calculerTendanceEngagement(List<InteractionUtilisateur> interactions,
                                             List<HistoriqueRecherche> recherches) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime il7Jours = maintenant.minusDays(7);
        LocalDateTime il14Jours = maintenant.minusDays(14);

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
            ComportementUtilisateur comportement = getOrCreateBehavior(userId);

            
            if (comportement.getMetriques() == null) comportement.setMetriques(new ComportementUtilisateur.MetriquesComportementales());
            if (comportement.getPreferencesSaisonnieres() == null) comportement.setPreferencesSaisonnieres(new ComportementUtilisateur.PreferencesSaisonnieres());
            if (comportement.getHabitudesNavigation() == null) comportement.setHabitudesNavigation(new ComportementUtilisateur.HabitudesNavigation());
            if (comportement.getCyclesActivite() == null) comportement.setCyclesActivite(new ComportementUtilisateur.CyclesActivite());

            List<InteractionUtilisateur> interactions = interactionService.getInteractionsByUserId(userId);
            List<HistoriqueRecherche> recherches = historiqueRechercheService.getHistoryByUserId(userId);

            ComportementUtilisateur.MetriquesComportementales metriques = comportement.getMetriques();
            ComportementUtilisateur.PreferencesSaisonnieres prefsSaison = comportement.getPreferencesSaisonnieres(); 
            ComportementUtilisateur.HabitudesNavigation habitudesNav = comportement.getHabitudesNavigation();
            ComportementUtilisateur.CyclesActivite cyclesActivite = comportement.getCyclesActivite();

            // 1. Patterns temporels et mise à jour de CyclesActivite
            Map<String, Object> patternsTemporels = analyserPatternsTemporels(interactions, recherches);
            Object activiteObj = patternsTemporels.get("activiteParJourSemaine");
            Map<DayOfWeek, Long> activiteParJourSemaineBrute = new HashMap<>();

            
            if (activiteObj instanceof Map) {
                
                @SuppressWarnings("unchecked")
                Map<DayOfWeek, Long> tempMap = (Map<DayOfWeek, Long>) activiteObj;
                activiteParJourSemaineBrute.putAll(tempMap);
            } else {
                logger.warn("Le type de 'activiteParJourSemaine' n'est pas une Map<DayOfWeek, Long> pour l'utilisateur {}", userId);
            }

            if (!activiteParJourSemaineBrute.isEmpty()) { 
                Map<String, Integer> activiteParJourStringInteger = activiteParJourSemaineBrute.entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> entry.getKey().toString(), 
                                entry -> entry.getValue().intValue() 
                        ));
                cyclesActivite.setActiviteParJour(activiteParJourStringInteger);
                cyclesActivite.setCreneauLePlusActif((String) patternsTemporels.get("heurePicActivite")); 
                cyclesActivite.setJoursActifs(activiteParJourStringInteger.keySet().stream().collect(Collectors.toList())); 
            } else {
                cyclesActivite.setActiviteParJour(new HashMap<>());
                cyclesActivite.setJoursActifs(new ArrayList<>());
            }

            // 2. Patterns de navigation et mise à jour de HabitudesNavigation
            
            Map<String, Object> patternsNavigationResult = analyserPatternsNavigation(interactions);

            habitudesNav.setPagesVisitees(safeConvertToStringIntegerMap(patternsNavigationResult.get("pagesVisitees"))); 
            habitudesNav.setTempsParPage(safeConvertToStringLongMap(patternsNavigationResult.get("tempsMoyenConsultationParType"))); 

            habitudesNav.setTempsMoyenParSession((Double) analyserPatternsSessions(interactions, recherches).get("dureeMoyenneSessionMinutes"));
            habitudesNav.setNombrePagesParSession(((Double) analyserPatternsSessions(interactions, recherches).get("nombreActivitesParSession")).intValue());
            habitudesNav.setParcoursFavoris(safeConvertToStringStringMap(analyserSequencesActions(interactions)));

            Map<String, Object> preferencesResult = analyserPreferences(interactions, recherches); 
            Object categoriesObj = preferencesResult.get("categoriesPreferees");

            
            if (categoriesObj instanceof List<?>) { 
                @SuppressWarnings("unchecked") 
                List<String> categoriesPreferees = (List<String>) categoriesObj;
                habitudesNav.setCategoriesPreferees(categoriesPreferees);
            } else {
                habitudesNav.setCategoriesPreferees(new ArrayList<>());
                logger.warn("Le type de 'categoriesPreferees' n'est pas une List<?> pour l'utilisateur {}. Valeur: {}", userId, categoriesObj);
            }
            habitudesNav.setTypeRecettePreferee((String) preferencesResult.get("typeRecetteDominant"));

            
            Object saisonPrefereeObj = preferencesResult.get("saisonPreferee");
            if (saisonPrefereeObj instanceof String) { 
                try {
                    prefsSaison.setSaisonPreferee(Saison.valueOf((String) saisonPrefereeObj));
                } catch (IllegalArgumentException e) {
                    logger.warn("Saison invalide '{}' trouvée pour l'utilisateur {}", saisonPrefereeObj, userId);
                }
            } else {
                prefsSaison.setSaisonPreferee(null); 
            }
            prefsSaison.setDerniereMiseAJour(LocalDateTime.now()); 

            // 3. Patterns de recherche (mis à jour dans Metriques)

            // 4. Score d'engagement et profil utilisateur (mis à jour dans Metriques)
            double scoreEngagement = calculerScoreEngagement(interactions, recherches);
            metriques.setScoreEngagement(scoreEngagement);
            metriques.setProfilUtilisateur(determinerProfilUtilisateur(scoreEngagement));

            // 5. Score de prédictibilité (mis à jour dans Metriques ou un nouveau champ)
            Double scorePredictibilite = calculerScorePredictibiliteSimplifiee(interactions, recherches);
            metriques.setScoreRecommandation(scorePredictibilite);
            Double scoreRecommandationCalcule = calculerScoreRecommandation(interactions, recherches, comportement);
            metriques.setScoreRecommandation(scoreRecommandationCalcule); 

            // 6. Anomalies comportementales
            List<String> anomaliesDetectees = detecterAnomalies(interactions, recherches, comportement);
            if (metriques != null) { 
                metriques.setAnomaliesDetectees(anomaliesDetectees); 
            }

            // 7. Mise à jour des fréquences d'actions
            Map<String, Integer> frequences = new HashMap<>();
            interactions.forEach(interaction -> {
                String type = interaction.getTypeInteraction();
                frequences.put(type, frequences.getOrDefault(type, 0) + 1);
            });
            metriques.setFrequenceActions(frequences);

            // 8. Calcul du taux de recherches fructueuses et nombre total de recherches
            metriques.setNombreRecherchesTotales(recherches.size());
            if (!recherches.isEmpty()) {
                long recherchesFructueuses = recherches.stream()
                        .filter(r -> Boolean.TRUE.equals(r.getRechercheFructueuse()))
                        .count();
                double tauxFructueuses = (double) recherchesFructueuses / recherches.size() * 100;
                metriques.setTauxRecherchesFructueuses(tauxFructueuses);
            }

            comportement.setDateMiseAJour(LocalDateTime.now());
            return updateBehavior(comportement);

        } catch (Exception e) {
            logger.error("Erreur lors de l'analyse des patterns pour l'utilisateur {}: {}", userId, e.getMessage(), e);
            ComportementUtilisateur comportement = getOrCreateBehavior(userId);
            comportement.setDateMiseAJour(LocalDateTime.now());
            return updateBehavior(comportement);
        }
    }

    @Override
    public AnalysePatternsDTO analyserPatternsDTO(Long userId) {
        Map<String, Object> patterns = new HashMap<>();

        try {
            List<InteractionUtilisateur> interactions = interactionService.getInteractionsByUserId(userId);
            List<HistoriqueRecherche> recherches = historiqueRechercheService.getHistoryByUserId(userId);
            ComportementUtilisateur comportement = getOrCreateBehavior(userId);

            patterns.put("patternsTemporels", analyserPatternsTemporels(interactions, recherches));
            patterns.put("patternsNavigation", analyserPatternsNavigation(interactions));
            patterns.put("patternsRecherche", analyserPatternsRecherche(recherches));
            patterns.put("patternsSessions", analyserPatternsSessions(interactions, recherches));
            patterns.put("preferences", analyserPreferences(interactions, recherches));
            patterns.put("sequencesFrequentes", analyserSequencesActions(interactions));
            patterns.put("anomalies", detecterAnomalies(interactions, recherches, comportement));
            patterns.put("scorePredictibilite", calculerScorePredictibiliteSimplifiee(interactions, recherches));

        } catch (Exception e) {
            logger.error("Erreur lors de l'analyse des patterns pour l'utilisateur {}: {}", userId, e.getMessage(), e);
            patterns.put("erreur", "Erreur lors de l'analyse des patterns: " + e.getMessage());
        }

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

        Map<Integer, Long> activiteParHeure = new HashMap<>();
        interactions.stream()
            .filter(i -> i.getDateInteraction() != null)
            .forEach(i -> activiteParHeure.merge(i.getDateInteraction().getHour(), 1L, Long::sum));
        recherches.stream()
            .filter(r -> r.getDateRecherche() != null)
            .forEach(r -> activiteParHeure.merge(r.getDateRecherche().getHour(), 1L, Long::sum));
        patternsTemporels.put("activiteParHeure", activiteParHeure);

        Map<DayOfWeek, Long> activiteParJourSemaine = new HashMap<>();
        interactions.stream()
            .filter(i -> i.getDateInteraction() != null)
            .forEach(i -> activiteParJourSemaine.merge(i.getDateInteraction().getDayOfWeek(), 1L, Long::sum));
        recherches.stream()
            .filter(r -> r.getDateRecherche() != null)
            .forEach(r -> activiteParJourSemaine.merge(r.getDateRecherche().getDayOfWeek(), 1L, Long::sum));
        patternsTemporels.put("activiteParJourSemaine", activiteParJourSemaine);

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

        Map<String, Long> utilisationFiltres = recherches.stream()
            .flatMap(r -> r.getFiltres() != null ? r.getFiltres().stream() : null)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                HistoriqueRecherche.Filtre::getNom,
                Collectors.counting()
            ));
        patternsRecherche.put("utilisationFiltres", utilisationFiltres);

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
     * @param interactions Liste des interactions de l'utilisateur.
     * @param recherches Liste des recherches de l'utilisateur.
     * @return Map contenant les patterns de session.
     */
    private Map<String, Object> analyserPatternsSessions(List<InteractionUtilisateur> interactions,
                                                        List<HistoriqueRecherche> recherches) {
        Map<String, Object> patternsSessions = new HashMap<>();

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

        List<Double> dureesSessions = new ArrayList<>();
        List<Integer> activitesParSession = new ArrayList<>();

        if (!toutesActivites.isEmpty()) {
            LocalDateTime debutSession = toutesActivites.get(0);
            int countActivites = 1;

            for (int i = 1; i < toutesActivites.size(); i++) {
                LocalDateTime prevTime = toutesActivites.get(i - 1);
                LocalDateTime currentTime = toutesActivites.get(i);

                if (java.time.Duration.between(prevTime, currentTime).toMinutes() > SESSION_INACTIVITY_THRESHOLD_MINUTES) {
                    dureesSessions.add((double) java.time.Duration.between(debutSession, prevTime).toMinutes());
                    activitesParSession.add(countActivites);
                    debutSession = currentTime;
                    countActivites = 1;
                } else {
                    countActivites++;
                }
            }
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

        Optional<Map.Entry<String, Long>> typeInteractionDominant = interactions.stream()
            .collect(Collectors.groupingBy(
                InteractionUtilisateur::getTypeInteraction,
                Collectors.counting()
            ))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue());
        typeInteractionDominant.ifPresent(entry -> preferences.put("typeInteractionDominant", entry.getKey()));

        // Analyse et ajout des catégories préférées
        
        Map<String, Long> categoriesInteractedWith = interactions.stream()
                .filter(i -> i.getEntiteId() != null) 
                .map(InteractionUtilisateur::getTypeInteraction) 
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        type -> type, 
                        Collectors.counting()
                ));
        List<String> topCategories = categoriesInteractedWith.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        preferences.put("categoriesPreferees", topCategories);

        return preferences;
    }

    /**
     * Analyse les séquences d'actions fréquentes de l'utilisateur.
     * @param interactions Liste des interactions de l'utilisateur.
     * @return Liste de chaînes de caractères représentant les séquences fréquentes.
     */
    private List<String> analyserSequencesActions(List<InteractionUtilisateur> interactions) {
        List<String> sequencesFrequentes = new ArrayList<>();

        if (interactions.size() < 2) {
            return sequencesFrequentes;
        }

        List<InteractionUtilisateur> sortedInteractions = interactions.stream()
            .filter(i -> i.getDateInteraction() != null)
            .sorted(Comparator.comparing(InteractionUtilisateur::getDateInteraction))
            .collect(Collectors.toList());

        Map<String, Long> pairesActions = new HashMap<>();

        for (int i = 0; i < sortedInteractions.size() - 1; i++) {
            InteractionUtilisateur current = sortedInteractions.get(i);
            InteractionUtilisateur next = sortedInteractions.get(i + 1);

            if (java.time.Duration.between(current.getDateInteraction(), next.getDateInteraction()).toMinutes() <= 5) {
                String paire = current.getTypeInteraction() + " -> " + next.getTypeInteraction();
                pairesActions.merge(paire, 1L, Long::sum);
            }
        }

        sequencesFrequentes = pairesActions.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        return sequencesFrequentes;
    }
    
 // Méthodes utilitaires pour les conversions de Map en toute sécurité

    /**
     * Détecte les anomalies comportementales chez l'utilisateur.
     * @param interactions Liste des interactions de l'utilisateur.
     * @param recherches Liste des recherches de l'utilisateur.
     * @param comportement L'objet ComportementUtilisateur.
     * @return Liste de chaînes de caractères décrivant les anomalies détectées.
     */
    private List<String> detecterAnomalies(List<InteractionUtilisateur> interactions,
                                           List<HistoriqueRecherche> recherches,
                                           ComportementUtilisateur comportement) {
        List<String> anomalies = new ArrayList<>();

        double moyenneActiviteParJour = (interactions.size() + recherches.size()) / (double) java.time.Duration.between(comportement.getDateCreation(), LocalDateTime.now()).toDays();
        if (moyenneActiviteParJour == 0) moyenneActiviteParJour = 1.0;

        long activiteDernierJour = interactions.stream()
            .filter(i -> i.getDateInteraction() != null && i.getDateInteraction().toLocalDate().isEqual(LocalDateTime.now().toLocalDate()))
            .count();
        activiteDernierJour += recherches.stream()
            .filter(r -> r.getDateRecherche() != null && r.getDateRecherche().toLocalDate().isEqual(LocalDateTime.now().toLocalDate()))
            .count();

        if (activiteDernierJour > (moyenneActiviteParJour * 3)) {
            anomalies.add("Pic d'activité inhabituel détecté aujourd'hui.");
        }

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

        if (totalActivite > 0 && (double) activiteNuit / totalActivite > 0.2) {
            anomalies.add("Activité significative détectée en dehors des heures normales (nuit).");
        }
        
        List<String> termesFrequents = (comportement.getMetriques() != null && comportement.getMetriques().getTermesRechercheFrequents() != null) ?
                                       comportement.getMetriques().getTermesRechercheFrequents() : new ArrayList<>();
        Optional<HistoriqueRecherche> derniereRecherche = recherches.stream()
            .max(Comparator.comparing(HistoriqueRecherche::getDateRecherche));

        if (derniereRecherche.isPresent() && !termesFrequents.contains(derniereRecherche.get().getTerme())) {
            anomalies.add("Nouveau terme de recherche inhabituel détecté: '" + derniereRecherche.get().getTerme() + "'.");
        }

        return anomalies;
    }

    /**
     * Calcule un score de prédictibilité du comportement de l'utilisateur.
     * @param interactions Liste des interactions de l'utilisateur.
     * @param recherches Liste des recherches de l'utilisateur.
     * @return Score de prédictibilité (0-100).
     */
    private Double calculerScorePredictibiliteSimplifiee(List<InteractionUtilisateur> interactions,
                                                         List<HistoriqueRecherche> recherches) {
        double score = 0.0;
        int maxScore = 100;

        Map<String, Object> patternsTemporels = analyserPatternsTemporels(interactions, recherches);
        Map<Integer, Long> activiteParHeure = safeConvertToIntegerLongMap(patternsTemporels.get("activiteParHeure"));

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

        List<String> sequencesFrequentes = analyserSequencesActions(interactions);
        score += sequencesFrequentes.size() * 5;

        if (userId != null) {
            ComportementUtilisateur comportement = getOrCreateBehavior(userId);
            List<String> anomalies = detecterAnomalies(interactions, recherches, comportement);
            score -= anomalies.size() * 10;
        }

        return Math.max(0, Math.min(maxScore, score));
    }

    /**
     * Convertit un objet en une Map<Integer, Long> de manière sûre,
     * en gérant les casts non vérifiés et les types de valeurs Long/Integer.
     * Cette méthode est utilisée pour les données comme 'activiteParHeure'.
     * @param obj L'objet à convertir.
     * @return Une Map<Integer, Long> convertie, ou une Map vide si la conversion échoue.
     */
    @SuppressWarnings("unchecked")
    private Map<Integer, Long> safeConvertToIntegerLongMap(Object obj) {
        Map<Integer, Long> result = new HashMap<>();
        if (obj instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                // Vérifie si la clé est un Integer
                if (entry.getKey() instanceof Integer) {
                    // Vérifie si la valeur est un Long
                    if (entry.getValue() instanceof Long) {
                        result.put((Integer) entry.getKey(), (Long) entry.getValue());
                    } 
                    // Ou si la valeur est un Integer et peut être convertie en Long
                    else if (entry.getValue() instanceof Integer) {
                        result.put((Integer) entry.getKey(), ((Integer) entry.getValue()).longValue());
                    }
                } else {
                    // Log un avertissement si la clé n'est pas du type attendu
                    logger.warn("Clé de type inattendu dans safeConvertToIntegerLongMap: {} (attendu Integer)", entry.getKey() != null ? entry.getKey().getClass().getName() : "null");
                }
            }
        } else {
            // Log un avertissement si l'objet n'est pas une Map
            logger.warn("Objet de type inattendu dans safeConvertToIntegerLongMap: {} (attendu Map)", obj != null ? obj.getClass().getName() : "null");
        }
        return result;
    }

    /**
     * Convertit un objet en une Map<String, Integer> de manière sûre.
     * @param obj L'objet à convertir.
     * @return Une Map<String, Integer> convertie, ou une Map vide si la conversion échoue.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Integer> safeConvertToStringIntegerMap(Object obj) {
        Map<String, Integer> result = new HashMap<>();
        if (obj instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String) {
                    if (entry.getValue() instanceof Integer) {
                        result.put((String) entry.getKey(), (Integer) entry.getValue());
                    } else if (entry.getValue() instanceof Long) {
                        Long val = (Long) entry.getValue();
                        if (val <= Integer.MAX_VALUE && val >= Integer.MIN_VALUE) {
                            result.put((String) entry.getKey(), val.intValue());
                        } else {
                            logger.warn("La valeur Long {} dépasse la capacité de Integer pour la clé {}", val, entry.getKey());
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Convertit un objet en une Map<String, Long> de manière sûre.
     * @param obj L'objet à convertir.
     * @return Une Map<String, Long> convertie, ou une Map vide si la conversion échoue.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Long> safeConvertToStringLongMap(Object obj) {
        Map<String, Long> result = new HashMap<>();
        if (obj instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String) {
                    if (entry.getValue() instanceof Long) {
                        result.put((String) entry.getKey(), (Long) entry.getValue());
                    } else if (entry.getValue() instanceof Integer) {
                        result.put((String) entry.getKey(), ((Integer) entry.getValue()).longValue());
                    }
                }
            }
        }
        return result;
    }

    /**
     * Convertit une liste de chaînes de caractères en une Map<String, String>
     * où les clés sont générées comme "sequence1", "sequence2", etc.
     * Utile pour stocker des listes ordonnées de parcours favoris.
     * @param sequences La liste de chaînes de caractères à convertir.
     * @return Une Map<String, String> représentant les séquences.
     */
    private Map<String, String> safeConvertToStringStringMap(List<String> sequences) {
        Map<String, String> result = new LinkedHashMap<>(); 
        if (sequences != null) {
            for (int i = 0; i < sequences.size(); i++) {
                result.put("sequence" + (i + 1), sequences.get(i));
            }
        }
        return result;
    }

    /**
     * Calcule le score de recommandation basé sur l'engagement et la diversité des interactions.
     * C'est une méthode d'exemple, à adapter selon vos critères réels.
     */
    private Double calculerScoreRecommandation(List<InteractionUtilisateur> interactions,
                                              List<HistoriqueRecherche> recherches,
                                              ComportementUtilisateur comportement) {
        double scoreRecommandation = 0.0;

        // Plus l'utilisateur est engagé, plus le score est élevé
        scoreRecommandation += comportement.getMetriques().getScoreEngagement() * 0.5;

        // Bonus pour la diversité des interactions
        Set<String> typesInteractionsUniques = interactions.stream()
            .map(InteractionUtilisateur::getTypeInteraction)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        scoreRecommandation += typesInteractionsUniques.size() * 5; 

        // Bonus pour les recherches fructueuses
        long fructueuses = recherches.stream().filter(HistoriqueRecherche::getRechercheFructueuse).count();
        scoreRecommandation += fructueuses * 0.2;

        // Bonus si les préférences saisonnières sont remplies (indique un profil plus complet)
        if (comportement.getPreferencesSaisonnieres() != null &&
            comportement.getPreferencesSaisonnieres().getSaisonPreferee() != null) {
            scoreRecommandation += 10;
        }

        return Math.min(100.0, scoreRecommandation); // Limiter le score à 100
    }
}