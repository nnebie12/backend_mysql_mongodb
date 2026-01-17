package com.example.demo.servicesMongoDB;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.example.demo.entiesMongodb.*;
import com.example.demo.entiesMongodb.RecommandationIA.RecommandationDetail;
import com.example.demo.entiesMongodb.enums.*;
import com.example.demo.repositoryMongoDB.*;
import com.example.demo.servicesMongoDB.*;

/**
 * Moteur de recommandations enrichi avec algorithmes collaboratifs et basés sur le contenu
 */
@Service
public class EnhancedRecommandationService {
    
    private final RecommandationIARepository recommandationRepository;
    private final ComportementUtilisateurRepository comportementRepository;
    private final RecetteInteractionRepository recetteInteractionRepository;
    private final HistoriqueRechercheRepository rechercheRepository;
    private final PropositionRecommandationService propositionService;
    
    public EnhancedRecommandationService(
        RecommandationIARepository recommandationRepository,
        ComportementUtilisateurRepository comportementRepository,
        RecetteInteractionRepository recetteInteractionRepository,
        HistoriqueRechercheRepository rechercheRepository,
        PropositionRecommandationService propositionService
    ) {
        this.recommandationRepository = recommandationRepository;
        this.comportementRepository = comportementRepository;
        this.recetteInteractionRepository = recetteInteractionRepository;
        this.rechercheRepository = rechercheRepository;
        this.propositionService = propositionService;
    }
    
    /**
     * Génère des recommandations hybrides (collaboratif + contenu + contextuel)
     */
    public RecommandationIA genererRecommandationHybride(Long userId) {
        ComportementUtilisateur comportement = comportementRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Comportement non trouvé"));
        
        List<RecommandationDetail> recommendations = new ArrayList<>();
        Map<String, Double> scores = new HashMap<>();
        
        // 1. Recommandations collaboratives (utilisateurs similaires)
        List<RecommandationDetail> collab = genererRecommandationsCollaboratives(userId, comportement);
        collab.forEach(r -> scores.put(r.getTitre(), 0.4));
        recommendations.addAll(collab);
        
        // 2. Recommandations basées sur le contenu
        List<RecommandationDetail> content = genererRecommandationsContenu(userId, comportement);
        content.forEach(r -> scores.merge(r.getTitre(), 0.3, Double::sum));
        recommendations.addAll(content);
        
        // 3. Recommandations contextuelles (moment, saison, tendances)
        List<RecommandationDetail> context = genererRecommandationsContextuelles(userId, comportement);
        context.forEach(r -> scores.merge(r.getTitre(), 0.3, Double::sum));
        recommendations.addAll(context);
        
        // Déduplication et scoring
        List<RecommandationDetail> finalRecs = dedupliquerEtScorer(recommendations, scores);
        
        // Calcul du score global de la recommandation
        double scoreGlobal = calculerScoreGlobalRecommandation(comportement, finalRecs);
        
        RecommandationIA recommendation = new RecommandationIA();
        recommendation.setUserId(userId);
        recommendation.setType("HYBRIDE");
        recommendation.setRecommandation(finalRecs);
        recommendation.setScore(scoreGlobal);
        recommendation.setDateRecommandation(LocalDateTime.now());
        recommendation.setEstUtilise(false);
        recommendation.setComportementUtilisateurId(comportement.getId());
        
        RecommandationIA saved = recommandationRepository.save(recommendation);
        propositionService.createProposition(userId, saved.getId(), 5);
        
        return saved;
    }
    
    /**
     * Recommandations collaboratives (filtrage collaboratif simplifié)
     */
    private List<RecommandationDetail> genererRecommandationsCollaboratives(
        Long userId, 
        ComportementUtilisateur comportement
    ) {
        List<RecommandationDetail> recs = new ArrayList<>();
        
        // Trouver des utilisateurs similaires
        List<ComportementUtilisateur> utilisateursSimilaires = trouverUtilisateursSimilaires(
            comportement, 
            5
        );
        
        if (utilisateursSimilaires.isEmpty()) {
            return recs;
        }
        
        // Récupérer les recettes populaires parmi ces utilisateurs
        Map<Long, Long> recettesPopulaires = new HashMap<>();
        
        for (ComportementUtilisateur similaire : utilisateursSimilaires) {
            List<RecetteInteraction> interactions = recetteInteractionRepository
                .findByIdUser(similaire.getUserId());
            
            interactions.stream()
                .filter(ri -> "FAVORI_AJOUTE".equals(ri.getTypeInteraction()) || 
                             "PARTAGE".equals(ri.getTypeInteraction()))
                .forEach(ri -> recettesPopulaires.merge(ri.getIdRecette(), 1L, Long::sum));
        }
        
        // Filtrer celles que l'utilisateur n'a pas encore vues
        List<RecetteInteraction> dejavu = recetteInteractionRepository.findByIdUser(userId);
        Set<Long> recettesVues = dejavu.stream()
            .map(RecetteInteraction::getIdRecette)
            .collect(Collectors.toSet());
        
        recettesPopulaires.entrySet().stream()
            .filter(e -> !recettesVues.contains(e.getKey()))
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(3)
            .forEach(e -> {
                RecommandationDetail detail = new RecommandationDetail();
                detail.setTitre("Recette #" + e.getKey() + " - Populaire dans votre communauté");
                detail.setDescription("Découverte par " + e.getValue() + " utilisateurs similaires à vous");
                detail.setLien("/recettes/" + e.getKey());
                detail.setCategorie("COLLABORATIF");
                detail.setScoreRelevance(e.getValue() * 10.0);
                detail.setTags(List.of("communauté", "populaire", "recommandé"));
                recs.add(detail);
            });
        
        return recs;
    }
    
    /**
     * Recommandations basées sur le contenu (préférences utilisateur)
     */
    private List<RecommandationDetail> genererRecommandationsContenu(
        Long userId,
        ComportementUtilisateur comportement
    ) {
        List<RecommandationDetail> recs = new ArrayList<>();
        
        // Analyser les préférences de contenu
        List<HistoriqueRecherche> recherches = rechercheRepository.findByUserId(userId);
        
        // Extraire les termes fréquents
        Map<String, Long> termesFréquents = recherches.stream()
            .collect(Collectors.groupingBy(
                HistoriqueRecherche::getTerme,
                Collectors.counting()
            ));
        
        // Recommander du contenu similaire
        termesFréquents.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(3)
            .forEach(e -> {
                RecommandationDetail detail = new RecommandationDetail();
                detail.setTitre("Plus de recettes avec : " + e.getKey());
                detail.setDescription("Basé sur vos " + e.getValue() + " recherches récentes");
                detail.setLien("/recettes?search=" + e.getKey());
                detail.setCategorie("CONTENU");
                detail.setScoreRelevance(e.getValue() * 8.0);
                detail.setTags(List.of("préférences", e.getKey()));
                recs.add(detail);
            });
        
        // Recommandations basées sur les préférences saisonnières
        if (comportement.getPreferencesSaisonnieres() != null) {
            Saison saisonPreferee = comportement.getPreferencesSaisonnieres().getSaisonPreferee();
            if (saisonPreferee != null) {
                List<String> ingredients = obtenirIngredientsSaison(
                    comportement.getPreferencesSaisonnieres(), 
                    saisonPreferee
                );
                
                ingredients.stream()
                    .limit(2)
                    .forEach(ing -> {
                        RecommandationDetail detail = new RecommandationDetail();
                        detail.setTitre("Recettes " + saisonPreferee + " avec " + ing);
                        detail.setDescription("Parfait pour la saison actuelle");
                        detail.setLien("/recettes?ingredient=" + ing + "&saison=" + saisonPreferee);
                        detail.setCategorie("SAISON");
                        detail.setScoreRelevance(75.0);
                        detail.setTags(List.of(saisonPreferee.toString(), ing, "saison"));
                        recs.add(detail);
                    });
            }
        }
        
        return recs;
    }
    
    /**
     * Recommandations contextuelles (moment, tendances, nouveautés)
     */
    private List<RecommandationDetail> genererRecommandationsContextuelles(
        Long userId,
        ComportementUtilisateur comportement
    ) {
        List<RecommandationDetail> recs = new ArrayList<>();
        
        // 1. Recommandations par créneau horaire
        String creneauActuel = comportement.getCreneauActuel();
        if (creneauActuel != null && !"hors-repas".equals(creneauActuel)) {
            RecommandationDetail detail = new RecommandationDetail();
            detail.setTitre("Idées pour votre " + creneauActuel);
            detail.setDescription("Recettes adaptées à ce moment de la journée");
            detail.setLien("/recettes?creneau=" + creneauActuel);
            detail.setCategorie("CONTEXTUEL");
            detail.setScoreRelevance(85.0);
            detail.setTags(List.of(creneauActuel, "moment", "timing"));
            recs.add(detail);
        }
        
        // 2. Tendances du moment
        List<RecetteInteraction> tendances = recetteInteractionRepository
            .findByDateInteractionAfterOrderByDateInteractionDesc(
                LocalDateTime.now().minusDays(7)
            );
        
        Map<Long, Long> recettesTrending = tendances.stream()
            .collect(Collectors.groupingBy(
                RecetteInteraction::getIdRecette,
                Collectors.counting()
            ));
        
        recettesTrending.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(2)
            .forEach(e -> {
                RecommandationDetail detail = new RecommandationDetail();
                detail.setTitre("Recette #" + e.getKey() + " - Tendance cette semaine");
                detail.setDescription("Consultée " + e.getValue() + " fois cette semaine");
                detail.setLien("/recettes/" + e.getKey());
                detail.setCategorie("TENDANCE");
                detail.setScoreRelevance(90.0);
                detail.setTags(List.of("trending", "populaire", "nouvelle"));
                recs.add(detail);
            });
        
        // 3. Recommandations anti-routine
        if (comportement.getMetriques() != null && 
            comportement.getMetriques().getScorePredictibilite() != null &&
            comportement.getMetriques().getScorePredictibilite() > 70) {
            
            RecommandationDetail detail = new RecommandationDetail();
            detail.setTitre("Sortez de votre zone de confort !");
            detail.setDescription("Découvrez des recettes en dehors de vos habitudes");
            detail.setLien("/recettes?surprise=true");
            detail.setCategorie("DECOUVERTE");
            detail.setScoreRelevance(60.0);
            detail.setTags(List.of("découverte", "nouveau", "surprise"));
            recs.add(detail);
        }
        
        return recs;
    }
    
    /**
     * Trouve des utilisateurs similaires basé sur le comportement
     */
    private List<ComportementUtilisateur> trouverUtilisateursSimilaires(
        ComportementUtilisateur reference,
        int limite
    ) {
        // Récupérer tous les comportements
        List<ComportementUtilisateur> tous = comportementRepository.findAll();
        
        // Calculer la similarité avec chaque utilisateur
        Map<ComportementUtilisateur, Double> similarites = new HashMap<>();
        
        for (ComportementUtilisateur autre : tous) {
            if (autre.getUserId().equals(reference.getUserId())) continue;
            
            double similarite = calculerSimilarite(reference, autre);
            if (similarite > 0.3) { // Seuil de similarité minimum
                similarites.put(autre, similarite);
            }
        }
        
        return similarites.entrySet().stream()
            .sorted(Map.Entry.<ComportementUtilisateur, Double>comparingByValue().reversed())
            .limit(limite)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Calcule la similarité entre deux utilisateurs (distance cosinus simplifiée)
     */
    private double calculerSimilarite(ComportementUtilisateur u1, ComportementUtilisateur u2) {
        double score = 0.0;
        int facteurs = 0;
        
        // 1. Profil similaire
        if (u1.getMetriques() != null && u2.getMetriques() != null) {
            if (u1.getMetriques().getProfilUtilisateur() == u2.getMetriques().getProfilUtilisateur()) {
                score += 0.3;
            }
            facteurs++;
        }
        
        // 2. Score d'engagement similaire
        if (u1.getMetriques() != null && u2.getMetriques() != null &&
            u1.getMetriques().getScoreEngagement() != null && 
            u2.getMetriques().getScoreEngagement() != null) {
            
            double diff = Math.abs(u1.getMetriques().getScoreEngagement() - 
                                 u2.getMetriques().getScoreEngagement());
            score += Math.max(0, 1.0 - (diff / 100.0)) * 0.3;
            facteurs++;
        }
        
        // 3. Préférences saisonnières
        if (u1.getPreferencesSaisonnieres() != null && 
            u2.getPreferencesSaisonnieres() != null) {
            if (u1.getPreferencesSaisonnieres().getSaisonPreferee() == 
                u2.getPreferencesSaisonnieres().getSaisonPreferee()) {
                score += 0.2;
            }
            facteurs++;
        }
        
        // 4. Habitudes de navigation
        if (u1.getHabitudesNavigation() != null && 
            u2.getHabitudesNavigation() != null &&
            u1.getHabitudesNavigation().getTypeRecettePreferee() != null &&
            u2.getHabitudesNavigation().getTypeRecettePreferee() != null) {
            
            if (u1.getHabitudesNavigation().getTypeRecettePreferee()
                  .equals(u2.getHabitudesNavigation().getTypeRecettePreferee())) {
                score += 0.2;
            }
            facteurs++;
        }
        
        return facteurs > 0 ? score / facteurs : 0.0;
    }
    
    /**
     * Déduplique et score les recommandations
     */
    private List<RecommandationDetail> dedupliquerEtScorer(
        List<RecommandationDetail> recommendations,
        Map<String, Double> scores
    ) {
        Map<String, RecommandationDetail> unique = new LinkedHashMap<>();
        
        for (RecommandationDetail rec : recommendations) {
            String key = rec.getTitre();
            if (unique.containsKey(key)) {
                // Fusionner les scores
                RecommandationDetail existing = unique.get(key);
                double newScore = existing.getScoreRelevance() + rec.getScoreRelevance();
                existing.setScoreRelevance(newScore);
                
                // Fusionner les tags
                Set<String> allTags = new HashSet<>(existing.getTags());
                allTags.addAll(rec.getTags());
                existing.setTags(new ArrayList<>(allTags));
            } else {
                unique.put(key, rec);
            }
        }
        
        return unique.values().stream()
            .sorted(Comparator.comparingDouble(RecommandationDetail::getScoreRelevance).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }
    
    /**
     * Calcule le score global de qualité de la recommandation
     */
    private double calculerScoreGlobalRecommandation(
        ComportementUtilisateur comportement,
        List<RecommandationDetail> recommendations
    ) {
        if (recommendations.isEmpty()) return 0.0;
        
        double scoreBase = recommendations.stream()
            .mapToDouble(RecommandationDetail::getScoreRelevance)
            .average()
            .orElse(0.0);
        
        // Bonus pour la diversité
        Set<String> categories = recommendations.stream()
            .map(RecommandationDetail::getCategorie)
            .collect(Collectors.toSet());
        double bonusDiversite = (categories.size() / 5.0) * 10;
        
        // Bonus selon le profil utilisateur
        double bonusProfil = 0.0;
        if (comportement.getMetriques() != null) {
            ProfilUtilisateur profil = comportement.getMetriques().getProfilUtilisateur();
            bonusProfil = switch (profil) {
                case FIDELE, ACTIF -> 15.0;
                case OCCASIONNEL -> 10.0;
                case DEBUTANT -> 5.0;
                default -> 0.0;
            };
        }
        
        return Math.min(100.0, scoreBase + bonusDiversite + bonusProfil);
    }
    
    private List<String> obtenirIngredientsSaison(
        ComportementUtilisateur.PreferencesSaisonnieres prefs, 
        Saison saison
    ) {
        if (prefs == null || saison == null) return List.of();
        
        return switch (saison) {
            case PRINTEMPS -> prefs.getIngredientsPrintemps() != null ? 
                            prefs.getIngredientsPrintemps() : List.of();
            case ETE -> prefs.getIngredientsEte() != null ? 
                      prefs.getIngredientsEte() : List.of();
            case AUTOMNE -> prefs.getIngredientsAutomne() != null ? 
                          prefs.getIngredientsAutomne() : List.of();
            case HIVER -> prefs.getIngredientsHiver() != null ? 
                        prefs.getIngredientsHiver() : List.of();
        };
    }
}