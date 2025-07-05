package com.example.demo.servicesImplMongoDB;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.entiesMongodb.RecommandationIA.RecommandationDetail;
import com.example.demo.entiesMongodb.ComportementUtilisateur;
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
    
    @Value("${sms.recipient}")
    private String smsRecipient;
    
    public RecommandationIAServiceImpl(RecommandationIARepository recommandationRepository,
                                       ComportementUtilisateurService comportementService,
                                       SmsService smsService,
                                       PropositionRecommandationService propositionRecommandationService) {
        this.recommandationRepository = recommandationRepository;
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
        String message = "Vous avez une nouvelle recommandation ! Consultez-la ici : https://tonsite.com/recommandations/" + saved.getId();
        String numeroUtilisateur = recoverNumberUser(userId);
        
        System.out.println("Tentative d'envoi SMS à : " + numeroUtilisateur);
        
        if (numeroUtilisateur != null && !numeroUtilisateur.isEmpty()) {
            try {
                smsService.sendSms(numeroUtilisateur, message);
                System.out.println("SMS programmé avec succès");
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi SMS : " + e.getMessage());
            }
        } else {
            System.err.println("Numéro de téléphone non configuré");
        }
        
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
        recommandationRepository.deleteAll(recommandations);
    }
    
    
    @Override
    public RecommandationIA genererRecommandationPersonnalisee(Long userId) {
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);
        
        RecommandationIA newRecommandation = new RecommandationIA();
        newRecommandation.setUserId(userId);
        newRecommandation.setType("PERSONNALISEE");
        newRecommandation.setDateRecommandation(LocalDateTime.now());
        newRecommandation.setEstUtilise(false);

        List<RecommandationDetail> details = new ArrayList<>();
        double score = 50.0; 

        if (comportementOpt.isEmpty()) {
            details.addAll(genererRecommandationsParDefautDetails());
            newRecommandation.setRecommandation(details);
            newRecommandation.setScore(score);
        } else {
            ComportementUtilisateur comportement = comportementOpt.get();
            String profil = comportement.getMetriques() != null ? 
                comportement.getMetriques().getProfilUtilisateur() : "nouveau";
            
            switch (profil) {
                case "nouveau":
                    details.addAll(genererRecommandationsNouvelUtilisateur());
                    break;
                case "actif":
                    details.addAll(genererRecommandationsUtilisateurActif(comportement));
                    break;
                case "fidèle":
                    details.addAll(genererRecommandationsUtilisateurFidele(comportement));
                    break;
                default:
                    details.addAll(genererRecommandationsGeneriques(comportement));
            }
            score = calculerScoreRecommandation(comportement, details);

            newRecommandation.setRecommandation(details);
            newRecommandation.setScore(score);
            newRecommandation.setComportementUtilisateurId(comportement.getId());
            newRecommandation.setProfilUtilisateurCible(profil);
            if (comportement.getMetriques() != null) {
                newRecommandation.setScoreEngagementReference(comportement.getMetriques().getScoreEngagement());
            }
        }
        
        RecommandationIA savedRecommandation = recommandationRepository.save(newRecommandation);
        propositionRecommandationService.createProposition(savedRecommandation.getUserId(), savedRecommandation.getId(), 3);
        return savedRecommandation;
    }
    
    @Override
    public RecommandationIA genererRecommandationSaisonniere(Long userId) {
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);
        
        RecommandationIA newRecommandation = new RecommandationIA();
        newRecommandation.setUserId(userId);
        newRecommandation.setType("SAISONNIERE");
        newRecommandation.setDateRecommandation(LocalDateTime.now());
        newRecommandation.setEstUtilise(false);

        List<RecommandationDetail> details = new ArrayList<>();
        double score = 50.0;

        if (comportementOpt.isEmpty()) {
            details.addAll(genererRecommandationsParDefautDetails());
            newRecommandation.setRecommandation(details);
            newRecommandation.setScore(score);
        } else {
            ComportementUtilisateur comportement = comportementOpt.get();
            if (comportement.getPreferencesSaisonnieres() != null) {
                String saisonPreferee = comportement.getPreferencesSaisonnieres().getSaisonPreferee();
                List<String> ingredients = obtenirIngredientsSaison(comportement.getPreferencesSaisonnieres(), saisonPreferee);
                
                for (String ingredient : ingredients.stream().limit(3).collect(Collectors.toList())) {
                    RecommandationDetail detail = new RecommandationDetail();
                    detail.setTitre("Recettes avec " + ingredient);
                    detail.setDescription("Découvrez nos meilleures recettes de " + saisonPreferee + " avec " + ingredient);
                    detail.setLien("/recettes/ingredient/" + ingredient.toLowerCase());
                    details.add(detail);
                }
                newRecommandation.setCategoriesRecommandees(details.stream().map(RecommandationDetail::getCategorie).filter(Objects::nonNull).collect(Collectors.toList()));
            }
            score = calculerScoreRecommandation(comportement, details);

            newRecommandation.setRecommandation(details);
            newRecommandation.setScore(score);
            newRecommandation.setComportementUtilisateurId(comportement.getId());
            if (comportement.getMetriques() != null) {
                newRecommandation.setScoreEngagementReference(comportement.getMetriques().getScoreEngagement());
            }
        }
        
        RecommandationIA savedRecommandation = recommandationRepository.save(newRecommandation);
        propositionRecommandationService.createProposition(savedRecommandation.getUserId(), savedRecommandation.getId(), 3);
        return savedRecommandation;
    }
    
    @Override
    public RecommandationIA genererRecommandationHabitudes(Long userId) {
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);
        
        RecommandationIA newRecommandation = new RecommandationIA();
        newRecommandation.setUserId(userId);
        newRecommandation.setType("HABITUDES");
        newRecommandation.setDateRecommandation(LocalDateTime.now());
        newRecommandation.setEstUtilise(false);

        List<RecommandationDetail> details = new ArrayList<>();
        double score = 50.0;

        if (comportementOpt.isEmpty()) {
            details.addAll(genererRecommandationsParDefautDetails());
            newRecommandation.setRecommandation(details);
            newRecommandation.setScore(score);
        } else {
            ComportementUtilisateur comportement = comportementOpt.get();
            if (comportement.getHabitudesNavigation() != null) {
                String typePrefere = comportement.getHabitudesNavigation().getTypeRecettePreferee();
                List<String> categoriesPreferees = comportement.getHabitudesNavigation().getCategoriesPreferees();
                
                if (typePrefere != null) {
                    RecommandationDetail detail = new RecommandationDetail();
                    detail.setTitre("Nouvelles recettes " + typePrefere);
                    detail.setDescription("Basé sur vos préférences pour les recettes " + typePrefere);
                    detail.setLien("/recettes/type/" + typePrefere.toLowerCase());
                    details.add(detail);
                }
                
                if (categoriesPreferees != null) {
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

            newRecommandation.setRecommandation(details);
            newRecommandation.setScore(score);
            newRecommandation.setComportementUtilisateurId(comportement.getId());
            if (comportement.getMetriques() != null) {
                newRecommandation.setScoreEngagementReference(comportement.getMetriques().getScoreEngagement());
            }
        }
        
        RecommandationIA savedRecommandation = recommandationRepository.save(newRecommandation);
        propositionRecommandationService.createProposition(savedRecommandation.getUserId(), savedRecommandation.getId(), 3);
        return savedRecommandation;
    }
    
    @Override
    public RecommandationIA genererRecommandationCreneauActuel(Long userId) {
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);
        
        RecommandationIA newRecommandation = new RecommandationIA();
        newRecommandation.setUserId(userId);
        newRecommandation.setType("CRENEAU_ACTUEL");
        newRecommandation.setDateRecommandation(LocalDateTime.now());
        newRecommandation.setEstUtilise(false);

        List<RecommandationDetail> details = new ArrayList<>();
        double score = 50.0;

        if (comportementOpt.isEmpty()) {
            details.addAll(genererRecommandationsParDefautDetails());
            newRecommandation.setRecommandation(details);
            newRecommandation.setScore(score);
        } else {
            ComportementUtilisateur comportement = comportementOpt.get();
            String creneauActuel = comportement.getCreneauActuel();
            
            if (comportement.getCyclesActivite() != null) {
                ComportementUtilisateur.CreneauRepas creneau = obtenirCreneauRepas(comportement.getCyclesActivite(), creneauActuel);
                
                if (creneau != null && creneau.getTypeRecettesPreferees() != null) {
                    for (String type : creneau.getTypeRecettesPreferees().stream().limit(2).collect(Collectors.toList())) {
                        RecommandationDetail detail = new RecommandationDetail();
                        detail.setTitre("Parfait pour votre " + creneauActuel);
                        detail.setDescription("Recettes " + type + " adaptées à ce moment de la journée");
                        detail.setLien("/recettes/" + creneauActuel + "/" + type.toLowerCase());
                        details.add(detail);
                    }
                    newRecommandation.setCategoriesRecommandees(creneau.getTypeRecettesPreferees());
                }
            }
            score = calculerScoreRecommandation(comportement, details);

            newRecommandation.setRecommandation(details);
            newRecommandation.setScore(score);
            newRecommandation.setComportementUtilisateurId(comportement.getId());
            newRecommandation.setCreneauCible(creneauActuel);
            if (comportement.getMetriques() != null) {
                newRecommandation.setScoreEngagementReference(comportement.getMetriques().getScoreEngagement());
            }
        }
        
        RecommandationIA savedRecommandation = recommandationRepository.save(newRecommandation);
        propositionRecommandationService.createProposition(savedRecommandation.getUserId(), savedRecommandation.getId(), 3);
        return savedRecommandation;
    }
    
    @Override
    public RecommandationIA mettreAJourScoreRecommandation(String recommandationId, ComportementUtilisateur comportement) {
        RecommandationIA recommandation = recommandationRepository.findById(recommandationId)
                .orElseThrow(() -> new RuntimeException("Recommandation non trouvée"));
        
        double nouveauScore = calculerScoreRecommandation(comportement, recommandation.getRecommandation());
        recommandation.setScore(nouveauScore);
        
        return recommandationRepository.save(recommandation);
    }
    
    @Override
    public List<RecommandationIA> getRecommandationsParProfil(Long userId) {
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);
        
        if (comportementOpt.isEmpty()) {
            return getRecommandationsByUserId(userId);
        }
        
        String profil = comportementOpt.get().getMetriques().getProfilUtilisateur();
        return getRecommandationsByUserIdAndType(userId, "PROFIL_" + profil.toUpperCase());
    }
    
    @Override
    public RecommandationIA genererRecommandationEngagement(Long userId) {
        Optional<ComportementUtilisateur> comportementOpt = comportementService.getBehaviorByUserId(userId);
        
        RecommandationIA newRecommandation = new RecommandationIA();
        newRecommandation.setUserId(userId);
        newRecommandation.setType("ENGAGEMENT");
        newRecommandation.setDateRecommandation(LocalDateTime.now());
        newRecommandation.setEstUtilise(false);

        List<RecommandationDetail> details = new ArrayList<>();
        double score = 50.0;

        if (comportementOpt.isEmpty()) {
            details.addAll(genererRecommandationsParDefautDetails());
            newRecommandation.setRecommandation(details);
            newRecommandation.setScore(score);
        } else {
            ComportementUtilisateur comportement = comportementOpt.get();
            Double scoreEngagement = comportement.getMetriques() != null ? 
                comportement.getMetriques().getScoreEngagement() : 0.0;
            
            if (scoreEngagement < 30) {
                RecommandationDetail detail = new RecommandationDetail();
                detail.setTitre("Découvrez nos recettes populaires");
                detail.setDescription("Les recettes les plus appréciées par notre communauté");
                detail.setLien("/recettes/populaires");
                details.add(detail);
                
                detail = new RecommandationDetail();
                detail.setTitre("Recettes rapides pour débutants");
                detail.setDescription("Des recettes simples et délicieuses en moins de 30 minutes");
                detail.setLien("/recettes/rapides");
                details.add(detail);
            }
            score = calculerScoreRecommandation(comportement, details);

            newRecommandation.setRecommandation(details);
            newRecommandation.setScore(score);
            newRecommandation.setComportementUtilisateurId(comportement.getId());
            newRecommandation.setScoreEngagementReference(scoreEngagement);
        }
        
        RecommandationIA savedRecommandation = recommandationRepository.save(newRecommandation);
        propositionRecommandationService.createProposition(savedRecommandation.getUserId(), savedRecommandation.getId(), 3);
        return savedRecommandation;
    }
    
    // Méthodes utilitaires privées
    
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
    
    private List<String> obtenirIngredientsSaison(ComportementUtilisateur.PreferencesSaisonnieres prefs, String saison) {
        if (prefs == null || saison == null) return new ArrayList<>();
        
        switch (saison.toLowerCase()) {
            case "printemps": return prefs.getIngredientsPrintemps() != null ? prefs.getIngredientsPrintemps() : new ArrayList<>();
            case "été": return prefs.getIngredientsEte() != null ? prefs.getIngredientsEte() : new ArrayList<>();
            case "automne": return prefs.getIngredientsAutomne() != null ? prefs.getIngredientsAutomne() : new ArrayList<>();
            case "hiver": return prefs.getIngredientsHiver() != null ? prefs.getIngredientsHiver() : new ArrayList<>();
            default: return new ArrayList<>();
        }
    }
    
    private ComportementUtilisateur.CreneauRepas obtenirCreneauRepas(ComportementUtilisateur.CyclesActivite cycles, String creneau) {
        if (cycles == null || creneau == null) return null;
        
        switch (creneau.toLowerCase()) {
            case "petit-dejeuner": return cycles.getPetitDejeuner();
            case "dejeuner": return cycles.getDejeuner();
            case "diner": return cycles.getDiner();
            default: return null;
        }
    }
    
    private double calculerScoreRecommandation(ComportementUtilisateur comportement, List<RecommandationDetail> details) {
        double scoreBase = 50.0;
        
        if (comportement.getMetriques() != null) {
            Double scoreEngagement = comportement.getMetriques().getScoreEngagement();
            if (scoreEngagement != null) {
                scoreBase += scoreEngagement * 0.3;
            }
        }
        
        scoreBase += details.size() * 5;
        
        return Math.min(100.0, scoreBase);
    }
    
    private String recoverNumberUser(Long userId) {
        return smsRecipient; 
    }
}
