package com.example.demo.servicesImplMongoDB;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.entiesMongodb.RecommandationIA.RecommandationDetail;
import com.example.demo.entiesMongodb.ComportementUtilisateur;
import com.example.demo.entiesMongodb.enums.ProfilUtilisateur;
import com.example.demo.entiesMongodb.enums.Saison;
import com.example.demo.repositoryMongoDB.ComportementUtilisateurRepository;
import com.example.demo.repositoryMongoDB.RecommandationIARepository;
import com.example.demo.servicesMongoDB.RecommandationIAService;
import com.example.demo.servicesMongoDB.ComportementUtilisateurService;
import com.example.demo.servicesMongoDB.PropositionRecommandationService;
import com.example.demo.servicesMysql.SmsService;

@Service
public class RecommandationIAServiceImpl implements RecommandationIAService {

    private final RecommandationIARepository recommandationRepository;
    private final ComportementUtilisateurService comportementService;
    private final SmsService smsService;
    private final PropositionRecommandationService propositionRecommandationService;
    private final ComportementUtilisateurRepository comportementUtilisateurRepository;

    @Value("${sms.recipient}")
    private String smsRecipient;

    public RecommandationIAServiceImpl(RecommandationIARepository recommandationRepository,
                                       ComportementUtilisateurService comportementService,
                                       SmsService smsService,
                                       PropositionRecommandationService propositionRecommandationService,
                                       ComportementUtilisateurRepository comportementUtilisateurRepository) {
        this.recommandationRepository = recommandationRepository;
        this.comportementUtilisateurRepository = comportementUtilisateurRepository;
        this.comportementService = comportementService;
        this.smsService = smsService;
        this.propositionRecommandationService = propositionRecommandationService;
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

        // Appel de la notification SMS APRÈS l'enregistrement de la recommandation
        envoyerNotificationSMS(saved);

        return saved;
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

        comportementService.enregistrerInteraction(
                recommandation.getUserId(),
                "RECOMMANDATION_UTILISEE",
                recommandationId,
                "score:" + recommandation.getScore()
        );

        return recommandationRepository.save(recommandation);
    }

    @Override
    public void deleteRecommandationsUser(Long userId) {
        List<RecommandationIA> recommandations = recommandationRepository.findByUserId(userId);
        if (recommandations != null && !recommandations.isEmpty()) {
            recommandationRepository.deleteAll(recommandations);
        }
    }

    @Override
    public RecommandationIA genererRecommandationPersonnalisee(Long userId) {
        RecommandationIA newRecommandation = initialiserNouvelleRecommandation(userId, "PERSONNALISEE");
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);

        List<RecommandationDetail> details = new ArrayList<>();
        double score;

        if (comportementOpt.isEmpty()) {
            details.addAll(genererRecommandationsParDefautDetails());
            score = 50.0;
        } else {
            ComportementUtilisateur comportement = comportementOpt.get();
            ProfilUtilisateur profil = (comportement.getMetriques() != null && comportement.getMetriques().getProfilUtilisateur() != null) ?
                    comportement.getMetriques().getProfilUtilisateur() : ProfilUtilisateur.NOUVEAU;

            switch (profil) {
                case NOUVEAU:
                    details.addAll(genererRecommandationsNouvelUtilisateur());
                    break;
                case ACTIF:
                    details.addAll(genererRecommandationsUtilisateurActif(comportement));
                    break;
                case FIDELE:
                    details.addAll(genererRecommandationsUtilisateurFidele(comportement));
                    break;
                default:
                    details.addAll(genererRecommandationsGeneriques(comportement));
            }
            score = calculerScoreRecommandation(comportement, details);

            newRecommandation.setComportementUtilisateurId(comportement.getId());
            newRecommandation.setProfilUtilisateurCible(profil.name());
            if (comportement.getMetriques() != null && comportement.getMetriques().getScoreEngagement() != null) {
                newRecommandation.setScoreEngagementReference(comportement.getMetriques().getScoreEngagement());
            }
        }

        newRecommandation.setRecommandation(details);
        newRecommandation.setScore(score);

        RecommandationIA savedRecommandation = recommandationRepository.save(newRecommandation);
        propositionRecommandationService.createProposition(savedRecommandation.getUserId(), savedRecommandation.getId(), 3);
        envoyerNotificationSMS(savedRecommandation); 
        return savedRecommandation;
    }
    

    @Override
    public RecommandationIA genererRecommandationSaisonniere(Long userId) {
        RecommandationIA newRecommandation = initialiserNouvelleRecommandation(userId, "SAISONNIERE");
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);

        List<RecommandationDetail> details = new ArrayList<>();
        double score;

        if (comportementOpt.isEmpty()) {
            details.addAll(genererRecommandationsParDefautDetails());
            score = 50.0;
        } else {
            ComportementUtilisateur comportement = comportementOpt.get();
            if (comportement.getPreferencesSaisonnieres() != null) {
                Saison saisonPreferee = comportement.getPreferencesSaisonnieres().getSaisonPreferee();
                List<String> ingredients = obtenirIngredientsSaison(comportement.getPreferencesSaisonnieres(), saisonPreferee);

                if (saisonPreferee != null) {
                    // Limiter à 3 ingrédients pour la recommandation
                    for (String ingredient : ingredients.stream().limit(3).collect(Collectors.toList())) {
                        RecommandationDetail detail = new RecommandationDetail();
                        detail.setTitre("Recettes avec " + ingredient);
                        detail.setDescription("Découvrez nos meilleures recettes de " + saisonPreferee.name().toLowerCase() + " avec " + ingredient);
                        detail.setLien("/recettes/ingredient/" + ingredient.toLowerCase());
                        details.add(detail);
                    }
                }
               
            }
            score = calculerScoreRecommandation(comportement, details);

            newRecommandation.setComportementUtilisateurId(comportement.getId());
            if (comportement.getMetriques() != null && comportement.getMetriques().getScoreEngagement() != null) {
                newRecommandation.setScoreEngagementReference(comportement.getMetriques().getScoreEngagement());
            }
        }

        newRecommandation.setRecommandation(details);
        newRecommandation.setScore(score);

        RecommandationIA savedRecommandation = recommandationRepository.save(newRecommandation);
        propositionRecommandationService.createProposition(savedRecommandation.getUserId(), savedRecommandation.getId(), 3);
        envoyerNotificationSMS(savedRecommandation); 
        return savedRecommandation;
    }


    @Override
    public RecommandationIA genererRecommandationHabitudes(Long userId) {
        RecommandationIA newRecommandation = initialiserNouvelleRecommandation(userId, "HABITUDES");
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);

        List<RecommandationDetail> details = new ArrayList<>();
        double score;

        if (comportementOpt.isEmpty()) {
            details.addAll(genererRecommandationsParDefautDetails());
            score = 50.0;
        } else {
            ComportementUtilisateur comportement = comportementOpt.get();
            if (comportement.getHabitudesNavigation() != null) {
                String typePrefere = comportement.getHabitudesNavigation().getTypeRecettePreferee();
                List<String> categoriesPreferees = comportement.getHabitudesNavigation().getCategoriesPreferees();

                if (typePrefere != null && !typePrefere.isEmpty()) {
                    RecommandationDetail detail = new RecommandationDetail();
                    detail.setTitre("Nouvelles recettes " + typePrefere);
                    detail.setDescription("Basé sur vos préférences pour les recettes " + typePrefere);
                    detail.setLien("/recettes/type/" + typePrefere.toLowerCase());
                    details.add(detail);
                }

                if (categoriesPreferees != null && !categoriesPreferees.isEmpty()) {
                    for (String categorie : categoriesPreferees.stream().limit(2).collect(Collectors.toList())) {
                        RecommandationDetail detail = new RecommandationDetail();
                        detail.setTitre("Explorez " + categorie);
                        detail.setDescription("Nouvelles découvertes dans la catégorie " + categorie);
                        detail.setLien("/recettes/categorie/" + categorie.toLowerCase());
                        details.add(detail);
                    }
                    newRecommandation.setCategoriesRecommandees(categoriesPreferees);
                }
            }
            score = calculerScoreRecommandation(comportement, details);

            newRecommandation.setComportementUtilisateurId(comportement.getId());
            if (comportement.getMetriques() != null && comportement.getMetriques().getScoreEngagement() != null) {
                newRecommandation.setScoreEngagementReference(comportement.getMetriques().getScoreEngagement());
            }
        }

        newRecommandation.setRecommandation(details);
        newRecommandation.setScore(score);

        RecommandationIA savedRecommandation = recommandationRepository.save(newRecommandation);
        propositionRecommandationService.createProposition(savedRecommandation.getUserId(), savedRecommandation.getId(), 3);
        envoyerNotificationSMS(savedRecommandation); 
        return savedRecommandation;
    }


    @Override
    public RecommandationIA genererRecommandationCreneauActuel(Long userId) {
        RecommandationIA newRecommandation = initialiserNouvelleRecommandation(userId, "CRENEAU_ACTUEL");
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);

        List<RecommandationDetail> details = new ArrayList<>();
        double score;

        if (comportementOpt.isEmpty()) {
            details.addAll(genererRecommandationsParDefautDetails());
            score = 50.0;
        } else {
            ComportementUtilisateur comportement = comportementOpt.get();
            String creneauActuel = comportement.getCreneauActuel(); 

            if (comportement.getCyclesActivite() != null && creneauActuel != null && !creneauActuel.isEmpty()) {
                ComportementUtilisateur.CreneauRepas creneau = obtenirCreneauRepas(comportement.getCyclesActivite(), creneauActuel);

                if (creneau != null && creneau.getTypeRecettesPreferees() != null && !creneau.getTypeRecettesPreferees().isEmpty()) {
                    for (String type : creneau.getTypeRecettesPreferees().stream().limit(2).collect(Collectors.toList())) {
                        RecommandationDetail detail = new RecommandationDetail();
                        detail.setTitre("Parfait pour votre " + creneauActuel);
                        detail.setDescription("Recettes " + type + " adaptées à ce moment de la journée");
                        detail.setLien("/recettes/" + creneauActuel.toLowerCase() + "/" + type.toLowerCase());
                        details.add(detail);
                    }
                    newRecommandation.setCategoriesRecommandees(creneau.getTypeRecettesPreferees());
                }
            }
            score = calculerScoreRecommandation(comportement, details);

            newRecommandation.setComportementUtilisateurId(comportement.getId());
            newRecommandation.setCreneauCible(creneauActuel);
            if (comportement.getMetriques() != null && comportement.getMetriques().getScoreEngagement() != null) {
                newRecommandation.setScoreEngagementReference(comportement.getMetriques().getScoreEngagement());
            }
        }

        newRecommandation.setRecommandation(details);
        newRecommandation.setScore(score);

        RecommandationIA savedRecommandation = recommandationRepository.save(newRecommandation);
        propositionRecommandationService.createProposition(savedRecommandation.getUserId(), savedRecommandation.getId(), 3);
        envoyerNotificationSMS(savedRecommandation); 
        return savedRecommandation;
    }


    @Override
    public RecommandationIA mettreAJourScoreRecommandation(String recommandationId, ComportementUtilisateur comportement) {
        RecommandationIA recommandation = recommandationRepository.findById(recommandationId)
                .orElseThrow(() -> new RuntimeException("Recommandation non trouvée avec l'ID: " + recommandationId)); 

        double nouveauScore = calculerScoreRecommandation(comportement, recommandation.getRecommandation());
        recommandation.setScore(nouveauScore);

        return recommandationRepository.save(recommandation);
    }


    @Override
    public List<RecommandationIA> getRecommandationsParProfil(Long userId) {
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);

        if (comportementOpt.isEmpty() || comportementOpt.get().getMetriques() == null || comportementOpt.get().getMetriques().getProfilUtilisateur() == null) {
            return getRecommandationsByUserId(userId);
        }

        String profil = comportementOpt.get().getMetriques().getProfilUtilisateur().name();
        return getRecommandationsByUserIdAndType(userId, "PROFIL_" + profil.toUpperCase());
    }


    @Override
    public RecommandationIA genererRecommandationEngagement(Long userId) {
        RecommandationIA newRecommandation = initialiserNouvelleRecommandation(userId, "ENGAGEMENT");
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);

        List<RecommandationDetail> details = new ArrayList<>();
        double score;

        if (comportementOpt.isEmpty()) {
            details.addAll(genererRecommandationsParDefautDetails());
            score = 50.0;
        } else {
            ComportementUtilisateur comportement = comportementOpt.get();
            Double scoreEngagement = (comportement.getMetriques() != null && comportement.getMetriques().getScoreEngagement() != null) ?
                    comportement.getMetriques().getScoreEngagement() : 0.0;

            if (scoreEngagement < 30) {
                RecommandationDetail detailPopulaires = new RecommandationDetail();
                detailPopulaires.setTitre("Découvrez nos recettes populaires");
                detailPopulaires.setDescription("Les recettes les plus appréciées par notre communauté");
                detailPopulaires.setLien("/recettes/populaires");
                details.add(detailPopulaires);

                RecommandationDetail detailRapides = new RecommandationDetail();
                detailRapides.setTitre("Recettes rapides pour débutants");
                detailRapides.setDescription("Des recettes simples et délicieuses en moins de 30 minutes");
                detailRapides.setLien("/recettes/rapides");
                details.add(detailRapides);
            } else {
                details.addAll(genererRecommandationsGeneriques(comportement));
            }
            score = calculerScoreRecommandation(comportement, details);

            newRecommandation.setComportementUtilisateurId(comportement.getId());
            newRecommandation.setScoreEngagementReference(scoreEngagement);
        }

        newRecommandation.setRecommandation(details);
        newRecommandation.setScore(score);

        RecommandationIA savedRecommandation = recommandationRepository.save(newRecommandation);
        propositionRecommandationService.createProposition(savedRecommandation.getUserId(), savedRecommandation.getId(), 3);
        envoyerNotificationSMS(savedRecommandation); 
        return savedRecommandation;
    }


     //Méthodes utilitaires privées

    /**
     * Initialise une nouvelle instance de RecommandationIA avec les champs communs.
     * @param userId L'ID de l'utilisateur.
     * @param type Le type de la recommandation.
     * @return Une nouvelle instance de RecommandationIA.
     */
    private RecommandationIA initialiserNouvelleRecommandation(Long userId, String type) {
        RecommandationIA newRecommandation = new RecommandationIA();
        newRecommandation.setUserId(userId);
        newRecommandation.setType(type);
        newRecommandation.setDateRecommandation(LocalDateTime.now());
        newRecommandation.setEstUtilise(false);
        return newRecommandation;
    }

    private List<RecommandationDetail> genererRecommandationsParDefautDetails() {
        List<RecommandationDetail> details = new ArrayList<>();
        RecommandationDetail detail = new RecommandationDetail();
        detail.setTitre("Bienvenue ! Découvrez nos recettes");
        detail.setDescription("Explorez notre collection de recettes délicieuses");
        detail.setLien("/recettes/populaires");
        details.add(detail);
        return details;
    }

    private List<RecommandationDetail> genererRecommandationsNouvelUtilisateur() {
        List<RecommandationDetail> details = new ArrayList<>();
        RecommandationDetail detail = new RecommandationDetail();
        detail.setTitre("Guide pour bien commencer");
        detail.setDescription("Découvrez comment utiliser au mieux notre plateforme");
        detail.setLien("/guide/debutant");
        details.add(detail);
        return details;
    }

    private List<RecommandationDetail> genererRecommandationsUtilisateurActif(ComportementUtilisateur comportement) {
        List<RecommandationDetail> details = new ArrayList<>();
        RecommandationDetail detail = new RecommandationDetail();
        detail.setTitre("Nouvelles recettes pour vous");
        detail.setDescription("Basé sur votre activité récente");
        detail.setLien("/recettes/personnalisees");
        details.add(detail);
        return details;
    }

    private List<RecommandationDetail> genererRecommandationsUtilisateurFidele(ComportementUtilisateur comportement) {
        List<RecommandationDetail> details = new ArrayList<>();
        RecommandationDetail detail = new RecommandationDetail();
        detail.setTitre("Recettes exclusives");
        detail.setDescription("Contenu premium pour nos utilisateurs fidèles");
        detail.setLien("/recettes/premium");
        details.add(detail);
        return details;
    }

    private List<RecommandationDetail> genererRecommandationsGeneriques(ComportementUtilisateur comportement) {
        List<RecommandationDetail> details = new ArrayList<>();
        RecommandationDetail detail = new RecommandationDetail();
        detail.setTitre("Recettes du moment");
        detail.setDescription("Découvrez les tendances actuelles");
        detail.setLien("/recettes/tendances");
        details.add(detail);
        return details;
    }

    private List<String> obtenirIngredientsSaison(ComportementUtilisateur.PreferencesSaisonnieres prefs, Saison saison) {
        if (prefs == null || saison == null) return new ArrayList<>();

        switch (saison) {
            case PRINTEMPS:
                return prefs.getIngredientsPrintemps() != null ? prefs.getIngredientsPrintemps() : new ArrayList<>();
            case ETE:
                return prefs.getIngredientsEte() != null ? prefs.getIngredientsEte() : new ArrayList<>();
            case AUTOMNE:
                return prefs.getIngredientsAutomne() != null ? prefs.getIngredientsAutomne() : new ArrayList<>();
            case HIVER:
                return prefs.getIngredientsHiver() != null ? prefs.getIngredientsHiver() : new ArrayList<>();
            default:
                return new ArrayList<>();
        }
    }

    private ComportementUtilisateur.CreneauRepas obtenirCreneauRepas(ComportementUtilisateur.CyclesActivite cycles, String creneau) {
        if (cycles == null || creneau == null || creneau.isEmpty()) return null;

        // Utilisation de toLowerCase() pour une comparaison insensible à la casse
        switch (creneau.toLowerCase()) {
            case "petit-dejeuner":
                return cycles.getPetitDejeuner();
            case "dejeuner":
                return cycles.getDejeuner();
            case "diner":
                return cycles.getDiner();
            default:
                return null;
        }
    }

    /**
     * Envoie une notification SMS à l'utilisateur.
     * @param recommendation La recommandation qui vient d'être générée.
     */
    private void envoyerNotificationSMS(RecommandationIA recommendation) {
        String message = "Vous avez une nouvelle recommandation ! Consultez-la ici : https://tonsite.com/recommandations/" + recommendation.getId();
        String numeroUtilisateur = recoverNumberUser(recommendation.getUserId()); 

        System.out.println("Tentative d'envoi SMS à : " + numeroUtilisateur);

        if (numeroUtilisateur != null && !numeroUtilisateur.isEmpty()) {
            try {
                smsService.sendSms(numeroUtilisateur, message);
                System.out.println("SMS programmé avec succès");
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi SMS : " + e.getMessage());
            }
        } else {
            System.err.println("Numéro de téléphone non configuré pour l'utilisateur ID: " + recommendation.getUserId());
        }
    }

    /**
     * Calcule le score de la recommandation basé sur le comportement de l'utilisateur et les détails de la recommandation.
     * @param comportement Le comportement de l'utilisateur.
     * @param details Les détails de la recommandation.
     * @return Le score calculé.
     */
    private double calculerScoreRecommandation(ComportementUtilisateur comportement, List<RecommandationDetail> details) {
        double scoreBase = 50.0;

        if (comportement != null && comportement.getMetriques() != null && comportement.getMetriques().getScoreEngagement() != null) {
            scoreBase += comportement.getMetriques().getScoreEngagement() * 0.3;
        }

        scoreBase += details.size() * 5;

        return Math.min(100.0, scoreBase); // Le score ne dépasse pas 100
    }

    /**
     * Cette méthode est un placeholder. En production, elle devrait récupérer le vrai numéro de téléphone
     * de l'utilisateur depuis une base de données MySQL, par exemple.
     * @param userId L'ID de l'utilisateur.
     * @return Le numéro de téléphone de l'utilisateur ou le numéro par défaut/de test.
     */
    private String recoverNumberUser(Long userId) {
    
        return smsRecipient; 
    }
}