package com.example.demo.servicesImplMongoDB;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.demo.DTO.RecetteResponseDTO;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.entiesMongodb.RecommandationIA.RecommandationDetail;
import com.example.demo.entiesMongodb.ComportementUtilisateur;
import com.example.demo.entiesMongodb.RecetteInteraction;
import com.example.demo.entiesMongodb.enums.ProfilUtilisateur;
import com.example.demo.entiesMongodb.enums.Saison;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.repositoryMongoDB.ComportementUtilisateurRepository;
import com.example.demo.repositoryMongoDB.RecommandationIARepository;
import com.example.demo.servicesMongoDB.RecommandationIAService;
import com.example.demo.servicesMongoDB.ComportementUtilisateurService;
import com.example.demo.servicesMongoDB.EnhancedRecommandationService;
import com.example.demo.servicesMongoDB.PropositionRecommandationService;
import com.example.demo.servicesMysql.SmsService;

@Service
public class RecommandationIAServiceImpl implements RecommandationIAService {

    private final RecommandationIARepository recommandationRepository;
    private final ComportementUtilisateurService comportementService;
    private final PropositionRecommandationService propositionRecommandationService;
    private final ComportementUtilisateurRepository comportementUtilisateurRepository;
    private final EnhancedRecommandationService enhancedRecommendationService;
    private final OllamaRecommendationServiceImpl ollamaService;

    @Value("${sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${sms.recipient}")
    private String smsRecipient;

    public RecommandationIAServiceImpl(RecommandationIARepository recommandationRepository,
                                       ComportementUtilisateurService comportementService,
                                       SmsService smsService,
                                       PropositionRecommandationService propositionRecommandationService,
                                       ComportementUtilisateurRepository comportementUtilisateurRepository,
                                       EnhancedRecommandationService enhancedRecommendationService,
                                       OllamaRecommendationServiceImpl ollamaService) {
        this.recommandationRepository         = recommandationRepository;
        this.comportementUtilisateurRepository = comportementUtilisateurRepository;
        this.comportementService              = comportementService;
        this.propositionRecommandationService = propositionRecommandationService;
        this.enhancedRecommendationService    = enhancedRecommendationService;
        this.ollamaService                    = ollamaService;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  MÉTHODES IA DIRECTES (délégation vers Ollama)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public List<RecetteResponseDTO> getPersonalizedRecommendations(
            Long userId,
            List<RecetteInteraction> userHistory,
            List<RecetteEntity> allRecipes,
            List<NoteDocument> userRatings) {
        return ollamaService.getPersonalizedRecommendations(userId, userHistory, allRecipes, userRatings);
    }

    @Override
    public List<RecetteResponseDTO> findSimilarRecipes(RecetteEntity targetRecipe,
                                                        List<RecetteEntity> allRecipes) {
        return ollamaService.findSimilarRecipes(targetRecipe, allRecipes);
    }

    @Override
    public Map<String, Object> detectTrends(List<RecetteInteraction> allInteractions) {
        return ollamaService.detectTrends(allInteractions);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CRUD / ACCÈS
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public RecommandationIA addRecommandation(Long userId, String type,
                                               List<RecommandationDetail> recommandation,
                                               Double score) {
        RecommandationIA newRec = new RecommandationIA();
        newRec.setUserId(userId);
        newRec.setType(type);
        newRec.setRecommandation(recommandation);
        newRec.setScore(score);
        newRec.setDateRecommandation(LocalDateTime.now());
        newRec.setEstUtilise(false);
        RecommandationIA saved = recommandationRepository.save(newRec);
        envoyerNotificationSMS(saved);
        return saved;
    }

    @Override
    public List<RecommandationIA> getAllRecommandations() {
        return recommandationRepository.findAll();
    }

    @Override
    public List<RecommandationIA> getRecommandationsAvecScore(Long userId) {
        List<RecommandationIA> list = recommandationRepository.findByUserId(userId);
        if (list != null) {
            list.sort((a, b) -> Double.compare(
                b.getScore() != null ? b.getScore() : 0.0,
                a.getScore() != null ? a.getScore() : 0.0));
        }
        return list;
    }

    @Override
    public String suggererMeilleurTypeRecommandation(Long userId) {
        Optional<ComportementUtilisateur> opt = comportementService.getBehaviorByUserId(userId);
        if (opt.isEmpty()) return "PERSONNALISEE";
        ComportementUtilisateur c = opt.get();
        if (c.getMetriques() != null) {
            ProfilUtilisateur profil = c.getMetriques().getProfilUtilisateur();
            Double score = c.getMetriques().getScoreEngagement();
            if (profil == ProfilUtilisateur.NOUVEAU) return "ENGAGEMENT";
            if (score != null && score > 70) return "HYBRIDE";
            if (c.getPreferencesSaisonnieres() != null &&
                c.getPreferencesSaisonnieres().getSaisonPreferee() != null) return "SAISONNIERE";
        }
        return "PERSONNALISEE";
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
        RecommandationIA rec = recommandationRepository.findById(recommandationId)
                .orElseThrow(() -> new RuntimeException("Recommandation non trouvée : " + recommandationId));
        rec.setEstUtilise(true);
        comportementService.enregistrerInteraction(rec.getUserId(), "RECOMMANDATION_UTILISEE",
                recommandationId, "score:" + rec.getScore());
        return recommandationRepository.save(rec);
    }

    @Override
    public void deleteRecommandationsUser(Long userId) {
        List<RecommandationIA> list = recommandationRepository.findByUserId(userId);
        if (list != null && !list.isEmpty()) recommandationRepository.deleteAll(list);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  GÉNÉRATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public RecommandationIA genererRecommandationPersonnalisee(Long userId) {
        RecommandationIA rec = initialiser(userId, "PERSONNALISEE");
        Optional<ComportementUtilisateur> opt = comportementService.getBehaviorByUserId(userId);
        List<RecommandationDetail> details;
        double score;
        if (opt.isEmpty()) {
            details = defautDetails(); score = 50.0;
        } else {
            ComportementUtilisateur c = opt.get();
            ProfilUtilisateur profil = profil(c);
            details = switch (profil) {
                case NOUVEAU -> nouveauDetails();
                case ACTIF   -> actifDetails(c);
                case FIDELE  -> fideleDetails(c);
                default      -> generiquesDetails(c);
            };
            score = calculerScore(c, details);
            rec.setComportementUtilisateurId(c.getId());
            rec.setProfilUtilisateurCible(profil.name());
            if (c.getMetriques() != null && c.getMetriques().getScoreEngagement() != null)
                rec.setScoreEngagementReference(c.getMetriques().getScoreEngagement());
        }
        rec.setRecommandation(details); rec.setScore(score);
        RecommandationIA saved = recommandationRepository.save(rec);
        propositionRecommandationService.createProposition(saved.getUserId(), saved.getId(), 3);
        envoyerNotificationSMS(saved);
        return saved;
    }

    @Override
    public RecommandationIA genererRecommandationSaisonniere(Long userId) {
        RecommandationIA rec = initialiser(userId, "SAISONNIERE");
        Optional<ComportementUtilisateur> opt = comportementService.getBehaviorByUserId(userId);
        List<RecommandationDetail> details = new ArrayList<>();
        double score;
        if (opt.isEmpty()) {
            details = defautDetails(); score = 50.0;
        } else {
            ComportementUtilisateur c = opt.get();
            if (c.getPreferencesSaisonnieres() != null) {
                Saison saison = c.getPreferencesSaisonnieres().getSaisonPreferee();
                List<String> ings = obtenirIngredientsSaison(c.getPreferencesSaisonnieres(), saison);
                if (saison != null) {
                    for (String ing : ings.stream().limit(3).collect(Collectors.toList())) {
                        RecommandationDetail d = new RecommandationDetail();
                        d.setTitre("Recettes avec " + ing);
                        d.setDescription("Nos meilleures recettes de " + saison.name().toLowerCase() + " avec " + ing);
                        d.setLien("/recettes/ingredient/" + ing.toLowerCase());
                        details.add(d);
                    }
                }
            }
            score = calculerScore(c, details);
            rec.setComportementUtilisateurId(c.getId());
            if (c.getMetriques() != null && c.getMetriques().getScoreEngagement() != null)
                rec.setScoreEngagementReference(c.getMetriques().getScoreEngagement());
        }
        rec.setRecommandation(details); rec.setScore(score);
        RecommandationIA saved = recommandationRepository.save(rec);
        propositionRecommandationService.createProposition(saved.getUserId(), saved.getId(), 3);
        envoyerNotificationSMS(saved);
        return saved;
    }

    @Override
    public RecommandationIA genererRecommandationHabitudes(Long userId) {
        RecommandationIA rec = initialiser(userId, "HABITUDES");
        Optional<ComportementUtilisateur> opt = comportementService.getBehaviorByUserId(userId);
        List<RecommandationDetail> details = new ArrayList<>();
        double score;
        if (opt.isEmpty()) {
            details = defautDetails(); score = 50.0;
        } else {
            ComportementUtilisateur c = opt.get();
            if (c.getHabitudesNavigation() != null) {
                String type = c.getHabitudesNavigation().getTypeRecettePreferee();
                if (type != null && !type.isEmpty()) {
                    RecommandationDetail d = new RecommandationDetail();
                    d.setTitre("Nouvelles recettes " + type);
                    d.setDescription("Basé sur vos préférences pour les recettes " + type);
                    d.setLien("/recettes/type/" + type.toLowerCase());
                    details.add(d);
                }
                List<String> cats = c.getHabitudesNavigation().getCategoriesPreferees();
                if (cats != null) {
                    for (String cat : cats.stream().limit(2).collect(Collectors.toList())) {
                        RecommandationDetail d = new RecommandationDetail();
                        d.setTitre("Explorez " + cat);
                        d.setDescription("Nouvelles découvertes dans la catégorie " + cat);
                        d.setLien("/recettes/categorie/" + cat.toLowerCase());
                        details.add(d);
                    }
                    rec.setCategoriesRecommandees(cats);
                }
            }
            score = calculerScore(c, details);
            rec.setComportementUtilisateurId(c.getId());
            if (c.getMetriques() != null && c.getMetriques().getScoreEngagement() != null)
                rec.setScoreEngagementReference(c.getMetriques().getScoreEngagement());
        }
        rec.setRecommandation(details); rec.setScore(score);
        RecommandationIA saved = recommandationRepository.save(rec);
        propositionRecommandationService.createProposition(saved.getUserId(), saved.getId(), 3);
        envoyerNotificationSMS(saved);
        return saved;
    }

    @Override
    public RecommandationIA genererRecommandationCreneauActuel(Long userId) {
        RecommandationIA rec = initialiser(userId, "CRENEAU_ACTUEL");
        Optional<ComportementUtilisateur> opt = comportementService.getBehaviorByUserId(userId);
        List<RecommandationDetail> details = new ArrayList<>();
        double score;
        if (opt.isEmpty()) {
            details = defautDetails(); score = 50.0;
        } else {
            ComportementUtilisateur c = opt.get();
            String creneau = c.getCreneauActuel();
            if (c.getCyclesActivite() != null && creneau != null && !creneau.isEmpty()) {
                ComportementUtilisateur.CreneauRepas cr = obtenirCreneauRepas(c.getCyclesActivite(), creneau);
                if (cr != null && cr.getTypeRecettesPreferees() != null) {
                    for (String t : cr.getTypeRecettesPreferees().stream().limit(2).collect(Collectors.toList())) {
                        RecommandationDetail d = new RecommandationDetail();
                        d.setTitre("Parfait pour votre " + creneau);
                        d.setDescription("Recettes " + t + " adaptées à ce moment");
                        d.setLien("/recettes/" + creneau.toLowerCase() + "/" + t.toLowerCase());
                        details.add(d);
                    }
                    rec.setCategoriesRecommandees(cr.getTypeRecettesPreferees());
                }
            }
            score = calculerScore(c, details);
            rec.setComportementUtilisateurId(c.getId());
            rec.setCreneauCible(creneau);
            if (c.getMetriques() != null && c.getMetriques().getScoreEngagement() != null)
                rec.setScoreEngagementReference(c.getMetriques().getScoreEngagement());
        }
        rec.setRecommandation(details); rec.setScore(score);
        RecommandationIA saved = recommandationRepository.save(rec);
        propositionRecommandationService.createProposition(saved.getUserId(), saved.getId(), 3);
        envoyerNotificationSMS(saved);
        return saved;
    }

    @Override
    public RecommandationIA genererRecommandationEngagement(Long userId) {
        RecommandationIA rec = initialiser(userId, "ENGAGEMENT");
        Optional<ComportementUtilisateur> opt = comportementService.getBehaviorByUserId(userId);
        List<RecommandationDetail> details = new ArrayList<>();
        double score;
        if (opt.isEmpty()) {
            details = defautDetails(); score = 50.0;
        } else {
            ComportementUtilisateur c = opt.get();
            double se = (c.getMetriques() != null && c.getMetriques().getScoreEngagement() != null)
                ? c.getMetriques().getScoreEngagement() : 0.0;
            if (se < 30) {
                RecommandationDetail d1 = new RecommandationDetail();
                d1.setTitre("Découvrez nos recettes populaires");
                d1.setDescription("Les recettes les plus appréciées par notre communauté");
                d1.setLien("/recettes/populaires");
                details.add(d1);
                RecommandationDetail d2 = new RecommandationDetail();
                d2.setTitre("Recettes rapides pour débutants");
                d2.setDescription("Des recettes simples et délicieuses en moins de 30 minutes");
                d2.setLien("/recettes/rapides");
                details.add(d2);
            } else {
                details = generiquesDetails(c);
            }
            score = calculerScore(c, details);
            rec.setComportementUtilisateurId(c.getId());
            rec.setScoreEngagementReference(se);
        }
        rec.setRecommandation(details); rec.setScore(score);
        RecommandationIA saved = recommandationRepository.save(rec);
        propositionRecommandationService.createProposition(saved.getUserId(), saved.getId(), 3);
        envoyerNotificationSMS(saved);
        return saved;
    }

    @Override
    public RecommandationIA genererRecommandationHybride(Long userId) {
        return enhancedRecommendationService.genererRecommandationHybride(userId);
    }

    @Override
    public RecommandationIA mettreAJourScoreRecommandation(String recommandationId,
                                                            ComportementUtilisateur comportement) {
        RecommandationIA rec = recommandationRepository.findById(recommandationId)
                .orElseThrow(() -> new RuntimeException("Recommandation non trouvée : " + recommandationId));
        rec.setScore(calculerScore(comportement, rec.getRecommandation()));
        return recommandationRepository.save(rec);
    }

    @Override
    public List<RecommandationIA> getRecommandationsParProfil(Long userId) {
        Optional<ComportementUtilisateur> opt = comportementService.getBehaviorByUserId(userId);
        if (opt.isEmpty() || opt.get().getMetriques() == null ||
            opt.get().getMetriques().getProfilUtilisateur() == null)
            return getRecommandationsByUserId(userId);
        String profil = opt.get().getMetriques().getProfilUtilisateur().name();
        return getRecommandationsByUserIdAndType(userId, "PROFIL_" + profil);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PRIVÉ
    // ═══════════════════════════════════════════════════════════════════════════

    private RecommandationIA initialiser(Long userId, String type) {
        RecommandationIA r = new RecommandationIA();
        r.setUserId(userId); r.setType(type);
        r.setDateRecommandation(LocalDateTime.now()); r.setEstUtilise(false);
        return r;
    }

    private ProfilUtilisateur profil(ComportementUtilisateur c) {
        return (c.getMetriques() != null && c.getMetriques().getProfilUtilisateur() != null)
            ? c.getMetriques().getProfilUtilisateur() : ProfilUtilisateur.NOUVEAU;
    }

    private List<RecommandationDetail> defautDetails() {
        RecommandationDetail d = new RecommandationDetail();
        d.setTitre("Bienvenue ! Découvrez nos recettes");
        d.setDescription("Explorez notre collection de recettes délicieuses");
        d.setLien("/recettes/populaires");
        return List.of(d);
    }

    private List<RecommandationDetail> nouveauDetails() {
        RecommandationDetail d = new RecommandationDetail();
        d.setTitre("Guide pour bien commencer");
        d.setDescription("Découvrez comment utiliser au mieux notre plateforme");
        d.setLien("/guide/debutant");
        return List.of(d);
    }

    private List<RecommandationDetail> actifDetails(ComportementUtilisateur c) {
        RecommandationDetail d = new RecommandationDetail();
        d.setTitre("Nouvelles recettes pour vous");
        d.setDescription("Basé sur votre activité récente");
        d.setLien("/recettes/personnalisees");
        return List.of(d);
    }

    private List<RecommandationDetail> fideleDetails(ComportementUtilisateur c) {
        RecommandationDetail d = new RecommandationDetail();
        d.setTitre("Recettes exclusives");
        d.setDescription("Contenu premium pour nos utilisateurs fidèles");
        d.setLien("/recettes/premium");
        return List.of(d);
    }

    private List<RecommandationDetail> generiquesDetails(ComportementUtilisateur c) {
        RecommandationDetail d = new RecommandationDetail();
        d.setTitre("Recettes du moment");
        d.setDescription("Découvrez les tendances actuelles");
        d.setLien("/recettes/tendances");
        return List.of(d);
    }

    private List<String> obtenirIngredientsSaison(ComportementUtilisateur.PreferencesSaisonnieres p, Saison saison) {
        if (p == null || saison == null) return new ArrayList<>();
        return switch (saison) {
            case PRINTEMPS -> p.getIngredientsPrintemps() != null ? p.getIngredientsPrintemps() : new ArrayList<>();
            case ETE       -> p.getIngredientsEte()       != null ? p.getIngredientsEte()       : new ArrayList<>();
            case AUTOMNE   -> p.getIngredientsAutomne()   != null ? p.getIngredientsAutomne()   : new ArrayList<>();
            case HIVER     -> p.getIngredientsHiver()     != null ? p.getIngredientsHiver()     : new ArrayList<>();
        };
    }

    private ComportementUtilisateur.CreneauRepas obtenirCreneauRepas(
            ComportementUtilisateur.CyclesActivite cycles, String creneau) {
        if (cycles == null || creneau == null) return null;
        return switch (creneau.toLowerCase()) {
            case "petit-dejeuner" -> cycles.getPetitDejeuner();
            case "dejeuner"       -> cycles.getDejeuner();
            case "diner"          -> cycles.getDiner();
            default               -> null;
        };
    }

    private double calculerScore(ComportementUtilisateur c, List<RecommandationDetail> details) {
        double base = 50.0;
        if (c != null && c.getMetriques() != null && c.getMetriques().getScoreEngagement() != null)
            base += c.getMetriques().getScoreEngagement() * 0.3;
        base += details.size() * 5;
        return Math.min(100.0, base);
    }

    private void envoyerNotificationSMS(RecommandationIA rec) {
        if (!smsEnabled) return;
        System.out.println("Tentative d'envoi SMS à : " + smsRecipient);
    }
}