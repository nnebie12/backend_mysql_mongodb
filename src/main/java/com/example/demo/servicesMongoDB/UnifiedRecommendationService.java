package com.example.demo.servicesMongoDB;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.DTO.RecetteResponseDTO;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entiesMongodb.RecommandationIA;
import com.example.demo.entiesMongodb.RecetteInteraction;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.repositoryMongoDB.NoteMongoRepository;
import com.example.demo.repositoryMongoDB.RecetteInteractionRepository;
import com.example.demo.repositoryMysql.RecetteRepository;
import com.example.demo.servicesImplMongoDB.RecommandationIAServiceImpl;

@Service
public class UnifiedRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedRecommendationService.class);
    
    private static final double CLASSIC_WEIGHT = 0.6;
    private static final double AI_WEIGHT = 0.4;
    private static final int MAX_RECOMMENDATIONS = 10;

    @Autowired
    private RecommandationIAServiceImpl classicService;

    @Autowired
    private GeminiRecommendationService aiService;
    
    @Autowired
    private RecetteRepository recetteRepository;
    
    @Autowired
    private RecetteInteractionRepository interactionRepository;
    
    @Autowired
    private NoteMongoRepository noteRepository;

    /**
     * Stratégie de recommandation unifiée combinant système classique et IA
     * 
     * @param userId ID de l'utilisateur
     * @return Liste des recommandations optimisées
     */
    public List<RecommandationIA> getSmartRecommendations(Long userId) {
        logger.info("Génération de recommandations intelligentes pour l'utilisateur: {}", userId);
        
        try {
            // 1. Récupérer les recommandations classiques
            List<RecommandationIA> classicRecommendations = classicService.getRecommandationsAvecScore(userId);
            
            if (classicRecommendations.isEmpty()) {
                logger.warn("Aucune recommandation classique trouvée, utilisation IA uniquement");
                return getAIOnlyRecommendations(userId);
            }

            // 2. Récupérer les données pour l'IA
            List<RecetteInteraction> userHistory = interactionRepository.findByIdUser(userId);
            List<RecetteEntity> allRecipes = recetteRepository.findAll();
            List<NoteDocument> userRatings = noteRepository.findByUserId(userId);

            // 3. Obtenir les recommandations IA
            List<RecetteResponseDTO> aiRecommendations = aiService.getPersonalizedRecommendations(
                userId, userHistory, allRecipes, userRatings
            );

            // 4. Enrichir les recommandations classiques avec les scores IA
            enrichWithAIScores(classicRecommendations, aiRecommendations);

            // 5. Ajouter les recommandations IA uniques
            addUniqueAIRecommendations(classicRecommendations, aiRecommendations, allRecipes, userId);

            // 6. Trier et limiter
            return classicRecommendations.stream()
                .sorted(Comparator.comparingDouble(RecommandationIA::getScore).reversed())
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Erreur lors de la génération des recommandations intelligentes", e);
            // Fallback vers recommandations classiques
            return classicService.getRecommandationsAvecScore(userId);
        }
    }

    /**
     * Stratégie avec poids personnalisables
     * 
     * @param userId ID de l'utilisateur
     * @param classicWeight Poids du système classique (0.0 à 1.0)
     * @param aiWeight Poids de l'IA (0.0 à 1.0)
     * @return Liste des recommandations
     */
    public List<RecommandationIA> getSmartRecommendations(Long userId, double classicWeight, double aiWeight) {
        logger.info("Recommandations avec poids personnalisés - Classic: {}, AI: {}", classicWeight, aiWeight);
        
        // Normaliser les poids si nécessaire
        double totalWeight = classicWeight + aiWeight;
        if (totalWeight == 0) {
            logger.warn("Poids totaux à zéro, utilisation des poids par défaut");
            classicWeight = CLASSIC_WEIGHT;
            aiWeight = AI_WEIGHT;
        } else if (totalWeight != 1.0) {
            classicWeight = classicWeight / totalWeight;
            aiWeight = aiWeight / totalWeight;
        }
        
        try {
            List<RecommandationIA> classicRecommendations = classicService.getRecommandationsAvecScore(userId);
            
            if (classicRecommendations.isEmpty()) {
                return getAIOnlyRecommendations(userId);
            }

            List<RecetteInteraction> userHistory = interactionRepository.findByIdUser(userId);
            List<RecetteEntity> allRecipes = recetteRepository.findAll();
            List<NoteDocument> userRatings = noteRepository.findByUserId(userId);

            List<RecetteResponseDTO> aiRecommendations = aiService.getPersonalizedRecommendations(
                userId, userHistory, allRecipes, userRatings
            );

            enrichWithAIScores(classicRecommendations, aiRecommendations, classicWeight, aiWeight);
            addUniqueAIRecommendations(classicRecommendations, aiRecommendations, allRecipes, userId);

            return classicRecommendations.stream()
                .sorted(Comparator.comparingDouble(RecommandationIA::getScore).reversed())
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Erreur lors de la génération des recommandations intelligentes", e);
            return classicService.getRecommandationsAvecScore(userId);
        }
    }

    /**
     * Enrichit les recommandations classiques avec les scores IA
     */
    private void enrichWithAIScores(List<RecommandationIA> classicRecommendations, 
                                     List<RecetteResponseDTO> aiRecommendations) {
        enrichWithAIScores(classicRecommendations, aiRecommendations, CLASSIC_WEIGHT, AI_WEIGHT);
    }

    /**
     * Enrichit les recommandations classiques avec les scores IA (poids personnalisables)
     */
    private void enrichWithAIScores(List<RecommandationIA> classicRecommendations, 
                                     List<RecetteResponseDTO> aiRecommendations,
                                     double classicWeight,
                                     double aiWeight) {
        
        // Créer un map pour recherche rapide
        Map<Long, RecetteResponseDTO> aiMap = aiRecommendations.stream()
            .collect(Collectors.toMap(RecetteResponseDTO::getId, dto -> dto));

        for (RecommandationIA recommendation : classicRecommendations) {
            RecetteEntity recette = recommendation.getRecetteEntity();
            
            if (recette != null && aiMap.containsKey(recette.getId())) {
                // La recette est aussi recommandée par l'IA
                double aiBoost = 0.25; // Bonus de concordance
                double currentScore = recommendation.getScore() != null ? recommendation.getScore() : 0.5;
                double adjustedScore = (currentScore * classicWeight) + 
                                      ((currentScore + aiBoost) * aiWeight);
                
                recommendation.setScore(adjustedScore);
                logger.debug("Recette {} enrichie - Ancien score: {}, Nouveau score: {}", 
                    recette.getId(), currentScore, adjustedScore);
            } else {
                // Recette uniquement dans le système classique
                double currentScore = recommendation.getScore() != null ? recommendation.getScore() : 0.5;
                recommendation.setScore(currentScore * classicWeight);
            }
        }
    }

    /**
     * Ajoute les recommandations IA qui ne sont pas dans la liste classique
     */
    private void addUniqueAIRecommendations(List<RecommandationIA> classicRecommendations,
                                           List<RecetteResponseDTO> aiRecommendations,
                                           List<RecetteEntity> allRecipes,
                                           Long userId) {
        
        // Récupérer les IDs déjà présents
        Set<Long> existingIds = classicRecommendations.stream()
            .filter(r -> r.getRecetteEntity() != null)
            .map(r -> r.getRecetteEntity().getId())
            .collect(Collectors.toSet());

        // Créer un map pour accès rapide aux recettes
        Map<Long, RecetteEntity> recipeMap = allRecipes.stream()
            .collect(Collectors.toMap(RecetteEntity::getId, r -> r));

        // Ajouter les recommandations IA uniques
        for (RecetteResponseDTO aiReco : aiRecommendations) {
            if (!existingIds.contains(aiReco.getId()) && recipeMap.containsKey(aiReco.getId())) {
                RecommandationIA newRecommendation = createRecommendationFromDTO(
                    aiReco, 
                    recipeMap.get(aiReco.getId()), 
                    userId
                );
                
                classicRecommendations.add(newRecommendation);
                logger.debug("Ajout recommandation IA unique: {}", aiReco.getId());
            }
        }
    }

    /**
     * Recommandations basées uniquement sur l'IA (fallback)
     */
    private List<RecommandationIA> getAIOnlyRecommendations(Long userId) {
        logger.info("Génération de recommandations IA uniquement pour l'utilisateur: {}", userId);
        
        try {
            List<RecetteInteraction> userHistory = interactionRepository.findByIdUser(userId);
            List<RecetteEntity> allRecipes = recetteRepository.findAll();
            List<NoteDocument> userRatings = noteRepository.findByUserId(userId);

            List<RecetteResponseDTO> aiRecommendations = aiService.getPersonalizedRecommendations(
                userId, userHistory, allRecipes, userRatings
            );

            Map<Long, RecetteEntity> recipeMap = allRecipes.stream()
                .collect(Collectors.toMap(RecetteEntity::getId, r -> r));

            return aiRecommendations.stream()
                .filter(dto -> recipeMap.containsKey(dto.getId()))
                .map(dto -> createRecommendationFromDTO(dto, recipeMap.get(dto.getId()), userId))
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Erreur lors de la génération des recommandations IA uniquement", e);
            return Collections.emptyList();
        }
    }

    /**
     * Crée un objet RecommandationIA à partir d'un DTO et d'une RecetteEntity
     */
    private RecommandationIA createRecommendationFromDTO(RecetteResponseDTO dto, 
                                                         RecetteEntity recette, 
                                                         Long userId) {
        RecommandationIA recommendation = new RecommandationIA();
        
        // Champs obligatoires
        recommendation.setUserId(userId);
        recommendation.setRecetteEntity(recette);
        recommendation.setScore(0.7 * AI_WEIGHT);
        recommendation.setDateRecommandation(LocalDateTime.now());
        recommendation.setType("AI_RECOMMENDATION");
        recommendation.setEstUtilise(false);
        
        // Créer un détail de recommandation
        List<RecommandationIA.RecommandationDetail> details = new ArrayList<>();
        RecommandationIA.RecommandationDetail detail = new RecommandationIA.RecommandationDetail();
        detail.setTitre(dto.getTitre());
        detail.setDescription(dto.getDescription());
        detail.setCategorie(dto.getTypeRecette() != null ? dto.getTypeRecette().toString() : "N/A");
        detail.setScoreRelevance(0.7);
        
        // Tags basés sur les caractéristiques de la recette
        List<String> tags = new ArrayList<>();
        if (dto.getTypeRecette() != null) {
            tags.add(dto.getTypeRecette().toString());
        }
        if (dto.getDifficulte() != null) {
            tags.add("Difficulté: " + dto.getDifficulte().toString());
        }
        if (dto.getCuisine() != null && !dto.getCuisine().isEmpty()) {
            tags.add("Cuisine: " + dto.getCuisine());
        }
        if (dto.getVegetarien() != null && dto.getVegetarien()) {
            tags.add("Végétarien");
        }
        detail.setTags(tags);
        
        details.add(detail);
        recommendation.setRecommandation(details);
        
        // Catégories recommandées
        List<String> categories = new ArrayList<>();
        if (dto.getTypeRecette() != null) {
            categories.add(dto.getTypeRecette().toString());
        }
        recommendation.setCategoriesRecommandees(categories);
        
        return recommendation;
    }

    /**
     * Obtenir des recommandations par stratégie
     */
    public List<RecommandationIA> getRecommendationsByStrategy(Long userId, String strategy) {
        logger.info("Recommandations avec stratégie '{}' pour l'utilisateur: {}", strategy, userId);
        
        if (strategy == null) {
            strategy = "HYBRID";
        }
        
        switch (strategy.toUpperCase()) {
            case "CLASSIC":
                return classicService.getRecommandationsAvecScore(userId);
                
            case "AI_ONLY":
                return getAIOnlyRecommendations(userId);
                
            case "HYBRID":
            case "SMART":
            default:
                return getSmartRecommendations(userId);
        }
    }
    
    /**
     * Obtenir des recommandations similaires à une recette donnée
     */
    public List<RecommandationIA> getSimilarRecommendations(Long recetteId, Long userId) {
        logger.info("Recherche de recommandations similaires à la recette {} pour l'utilisateur {}", 
            recetteId, userId);
        
        try {
            Optional<RecetteEntity> targetRecipeOpt = recetteRepository.findById(recetteId);
            if (!targetRecipeOpt.isPresent()) {
                logger.warn("Recette {} introuvable", recetteId);
                return Collections.emptyList();
            }
            
            RecetteEntity targetRecipe = targetRecipeOpt.get();
            List<RecetteEntity> allRecipes = recetteRepository.findAll();
            
            List<RecetteResponseDTO> similarRecipes = aiService.findSimilarRecipes(targetRecipe, allRecipes);
            
            return similarRecipes.stream()
                .map(dto -> {
                    Optional<RecetteEntity> recetteOpt = recetteRepository.findById(dto.getId());
                    return recetteOpt.map(recette -> 
                        createRecommendationFromDTO(dto, recette, userId)
                    ).orElse(null);
                })
                .filter(Objects::nonNull)
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche de recommandations similaires", e);
            return Collections.emptyList();
        }
    }
}