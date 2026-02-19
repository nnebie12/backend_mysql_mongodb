package com.example.demo.servicesImplMongoDB;


import com.example.demo.entiesMongodb.CommentaireDocument;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import com.example.demo.DTO.NLPUserInsightsDTO;
import com.example.demo.entiesMongodb.HistoriqueRecherche;
import com.example.demo.repositoryMongoDB.CommentaireMongoRepository;
import com.example.demo.repositoryMongoDB.HistoriqueRechercheRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service NLP pour l'analyse sémantique des recettes
 * Utilise des embeddings textuels et l'analyse de similarité
 */
@Service
public class RecipeNLPService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecipeNLPService.class);
    
    private final String ML_SERVICE_URL = "http://localhost:8000";
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Autowired
    private CommentaireMongoRepository commentaireRepository;

    @Autowired
    private HistoriqueRechercheRepository rechercheRepository;
    
    @Value("${google.cloud.project-id}")
    private String projectId;
    
    @Value("${google.cloud.location}")
    private String location;
    
    @Value("${ml.service.url}")
    private String mlServiceUrl;
    
    private static final String MODEL_NAME = "gemini-1.5-flash";
    
    // Cache des embeddings pour performance
    private Map<Long, double[]> recipeEmbeddingsCache = new HashMap<>();
    
    /**
     * Génère un embedding vectoriel pour une recette
     * Combine titre, description, ingrédients et tags
     */
    public double[] generateRecipeEmbedding(RecetteEntity recette) {
        
        // Vérifier le cache
        if (recipeEmbeddingsCache.containsKey(recette.getId())) {
            return recipeEmbeddingsCache.get(recette.getId());
        }
        
        try {
            // Construire le texte complet de la recette
            String recipeText = buildRecipeText(recette);
            
            // Générer l'embedding avec Gemini
            double[] embedding = generateTextEmbedding(recipeText);
            
            // Mettre en cache
            recipeEmbeddingsCache.put(recette.getId(), embedding);
            
            return embedding;
            
        } catch (Exception e) {
            logger.error("Erreur génération embedding pour recette {}", recette.getId(), e);
            return new double[0];
        }
    }
    
    /**
     * Construit le texte complet d'une recette pour l'embedding
     */
    private String buildRecipeText(RecetteEntity recette) {
        StringBuilder text = new StringBuilder();
        
        // Titre (poids élevé)
        text.append(recette.getTitre()).append(". ");
        text.append(recette.getTitre()).append(". "); // Répéter pour donner plus de poids
        
        // Description
        if (recette.getDescription() != null && !recette.getDescription().isEmpty()) {
            text.append(recette.getDescription()).append(" ");
        }
        
        // Type et cuisine
        text.append("Type: ").append(recette.getTypeRecette()).append(". ");
        if (recette.getCuisine() != null) {
            text.append("Cuisine: ").append(recette.getCuisine()).append(". ");
        }
        
        // Ingrédients (très important pour similarité)
        if (recette.getRecetteIngredients() != null && !recette.getRecetteIngredients().isEmpty()) {
            text.append("Ingrédients: ");
            recette.getRecetteIngredients().forEach(ing -> 
                text.append(ing.getIngredientEntity().getNom()).append(", ")
            );
        }
        
        // Caractéristiques
        if (recette.getVegetarien()) {
            text.append("Végétarien. ");
        }
        
        text.append("Difficulté: ").append(recette.getDifficulte()).append(". ");
        
        return text.toString().trim();
    }
    
 // ── Méthode principale ───────────────────────────────────────────────────────

    /**
     * Génère une analyse NLP complète pour un utilisateur donné.
     * Agrège :
     *  - le sentiment moyen de ses commentaires
     *  - les mots-clés extraits de ses recherches
     *  - les topics dominants
     *  - un profil sémantique pondéré
     *  - un résumé textuel généré par Gemini
     *
     * @param userId ID de l'utilisateur à analyser
     * @return NLPUserInsightsDTO rempli
     */
    public NLPUserInsightsDTO getUserNLPInsights(Long userId) {

        NLPUserInsightsDTO insights = new NLPUserInsightsDTO(userId);

        // 1. Récupération des données brutes
        List<CommentaireDocument> commentaires = commentaireRepository.findByUserId(String.valueOf(userId));
        List<HistoriqueRecherche> recherches    = rechercheRepository.findByUserId(userId);

        // ── Garde-fou : données insuffisantes ────────────────────────────────
        boolean hasComments  = commentaires != null && !commentaires.isEmpty();
        boolean hasSearches  = recherches   != null && !recherches.isEmpty();

        if (!hasComments && !hasSearches) {
            insights.setHasSufficientData(false);
            insights.setDataQualityMessage(
                "Aucun commentaire ni historique de recherche trouvé pour cet utilisateur.");
            insights.setAverageSentimentScore(0.0);
            insights.setTotalCommentsAnalyzed(0);
            insights.setTopKeywords(List.of());
            insights.setTopSearchTerms(List.of());
            insights.setDominantTopics(List.of());
            insights.setSemanticProfile(Map.of());
            insights.setProfileSummary("Profil non disponible — données insuffisantes.");
            return insights;
        }

        insights.setHasSufficientData(true);

        // 2. Analyse de sentiment sur les commentaires
        if (hasComments) {
            double avgSentiment = calculateAverageSentiment(commentaires);
            insights.setAverageSentimentScore(
                Math.round(avgSentiment * 100) / 100.0
            );
            insights.setTotalCommentsAnalyzed(commentaires.size());
        } else {
            insights.setAverageSentimentScore(0.0);
            insights.setTotalCommentsAnalyzed(0);
        }

        // 3. Mots-clés extraits des termes de recherche
        List<String> topSearchTerms = extractTopSearchTerms(recherches, 10);
        insights.setTopSearchTerms(topSearchTerms);

        // 4. Mots-clés NLP extraits des commentaires (via Gemini)
        List<String> commentKeywords = extractKeywordsFromComments(commentaires);
        insights.setTopKeywords(commentKeywords);

        // 5. Topics dominants (fusion recherches + commentaires)
        List<String> dominantTopics = buildDominantTopics(recherches, commentaires);
        insights.setDominantTopics(dominantTopics);

        // 6. Profil sémantique pondéré
        Map<String, Double> semanticProfile = buildSemanticProfile(recherches, commentaires);
        insights.setSemanticProfile(semanticProfile);

        // 7. Résumé textuel Gemini
        String summary = generateProfileSummary(userId, insights);
        insights.setProfileSummary(summary);

        // 8. Taux de recherches fructueuses
        if (hasSearches) {
            long fructueuses = recherches.stream()
                .filter(r -> Boolean.TRUE.equals(r.getRechercheFructueuse()))
                .count();
            double taux = (double) fructueuses / recherches.size() * 100;
            insights.setSuccessfulSearchRate(Math.round(taux * 10) / 10.0);
        }

        return insights;
    }

// ── Méthodes privées d'aide ──────────────────────────────────────────────────

    /**
     * Extrait les termes de recherche les plus fréquents de l'historique.
     */
    private List<String> extractTopSearchTerms(List<HistoriqueRecherche> recherches, int limit) {
        if (recherches == null || recherches.isEmpty()) return List.of();

        return recherches.stream()
            .collect(Collectors.groupingBy(
                HistoriqueRecherche::getTerme,
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Extrait les mots-clés NLP depuis les commentaires via Gemini.
     * Retourne une liste vide en cas d'erreur pour ne pas bloquer le flux.
     */
    private List<String> extractKeywordsFromComments(List<CommentaireDocument> commentaires) {
        if (commentaires == null || commentaires.isEmpty()) return List.of();

        try (VertexAI vertexAI = new VertexAI(projectId, location)) {

            GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);

            // Concaténer jusqu'à 20 commentaires pour rester dans le contexte
            String texteAggrege = commentaires.stream()
                .limit(20)
                .map(CommentaireDocument::getContenu)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.joining(" | "));

            if (texteAggrege.isBlank()) return List.of();

            String prompt = String.format(
                "Analyse ces commentaires d'un utilisateur sur des recettes de cuisine " +
                "et extrait les 10 mots-clés culinaires les plus représentatifs.\n\n" +
                "Commentaires : %s\n\n" +
                "Retourne uniquement les mots-clés séparés par des virgules, sans explication.\n" +
                "Exemple : poulet, rapide, épicé, végétarien, grillé",
                texteAggrege.length() > 1500 ? texteAggrege.substring(0, 1500) : texteAggrege
            );

            GenerateContentResponse response = model.generateContent(prompt);
            String keywordsText = ResponseHandler.getText(response);

            return Arrays.stream(keywordsText.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .limit(10)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Erreur extraction mots-clés commentaires userId: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Construit la liste des topics dominants en fusionnant les catégories
     * de recherches et les mots-clés de commentaires.
     */
    private List<String> buildDominantTopics(
            List<HistoriqueRecherche> recherches,
            List<CommentaireDocument> commentaires) {

        Map<String, Long> topicCount = new HashMap<>();

        // Topics issus des catégories de recherche
        if (recherches != null) {
            recherches.stream()
                .map(HistoriqueRecherche::getCategorieRecherche)
                .filter(Objects::nonNull)
                .forEach(cat -> topicCount.merge(cat.toUpperCase(), 1L, Long::sum));
        }

        // Topics issus des termes de recherche (top 5)
        if (recherches != null) {
            extractTopSearchTerms(recherches, 5)
                .forEach(terme -> topicCount.merge(terme, 2L, Long::sum)); // poids x2
        }

        return topicCount.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Construit un profil sémantique pondéré (catégorie → score normalisé 0..1).
     */
    private Map<String, Double> buildSemanticProfile(
            List<HistoriqueRecherche> recherches,
            List<CommentaireDocument> commentaires) {

        Map<String, Double> raw = new LinkedHashMap<>();

        if (recherches != null && !recherches.isEmpty()) {
            Map<String, Long> catCount = recherches.stream()
                .map(HistoriqueRecherche::getCategorieRecherche)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(String::toUpperCase, Collectors.counting()));

            long total = catCount.values().stream().mapToLong(Long::longValue).sum();
            catCount.forEach((cat, count) ->
                raw.put(cat, Math.round((double) count / total * 100.0) / 100.0)
            );
        }

        // Bonus si l'utilisateur commente beaucoup → profil "ACTIF_COMMENTATEUR"
        if (commentaires != null && commentaires.size() >= 5) {
            raw.put("ACTIF_COMMENTATEUR", Math.min(1.0,
                Math.round(commentaires.size() / 20.0 * 100.0) / 100.0));
        }

        return raw;
    }

    /**
     * Génère un résumé textuel du profil culinaire via Gemini.
     */
    private String generateProfileSummary(Long userId, NLPUserInsightsDTO insights) {
        try (VertexAI vertexAI = new VertexAI(projectId, location)) {

            GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);

            String prompt = String.format(
                "Tu es un expert en analyse culinaire. Rédige en 2-3 phrases un résumé " +
                "du profil culinaire de cet utilisateur basé sur ces données NLP.\n\n" +
                "Sentiment moyen : %s (%s)\n" +
                "Topics dominants : %s\n" +
                "Mots-clés fréquents : %s\n" +
                "Taux recherches fructueuses : %.0f%%\n\n" +
                "Sois concis, positif et personnalisé. Ne mentionne pas de chiffres bruts.",
                insights.getAverageSentimentScore(),
                insights.getSentimentLabel(),
                insights.getDominantTopics(),
                insights.getTopSearchTerms(),
                insights.getSuccessfulSearchRate() != null ? insights.getSuccessfulSearchRate() : 0.0
            );

            GenerateContentResponse response = model.generateContent(prompt);
            return ResponseHandler.getText(response).trim();

        } catch (Exception e) {
            logger.error("Erreur génération résumé profil userId {}: {}", userId, e.getMessage());
            return "Profil en cours de construction — revenez plus tard pour un résumé complet.";
        }
    }
    
    
    /**
     * Génère un embedding à partir de texte avec Gemini
     * Simule un embedding 768-dimensions
     */
    private double[] generateTextEmbedding(String text) throws Exception {
        
        try (VertexAI vertexAI = new VertexAI(projectId, location)) {
            
            GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);
            
            // Prompt pour générer une représentation numérique
            String prompt = String.format(
                "Analyse ce texte et génère un vecteur numérique représentatif.\n" +
                "Texte: %s\n\n" +
                "Retourne 50 nombres décimaux séparés par des virgules représentant " +
                "les caractéristiques sémantiques du texte (thème culinaire, ingrédients, " +
                "complexité, type de plat, etc.).\n" +
                "Format: 0.23,-0.45,0.67,...",
                text.length() > 1000 ? text.substring(0, 1000) : text
            );
            
            GenerateContentResponse response = model.generateContent(prompt);
            String embeddingText = ResponseHandler.getText(response);
            
            // Parser les valeurs
            String[] values = embeddingText.trim().split(",");
            double[] embedding = new double[values.length];
            
            for (int i = 0; i < values.length; i++) {
                try {
                    embedding[i] = Double.parseDouble(values[i].trim());
                } catch (NumberFormatException e) {
                    embedding[i] = 0.0;
                }
            }
            
            // Normaliser le vecteur
            return normalizeVector(embedding);
        }
    }
    
    /**
     * Calcule la similarité cosinus entre deux recettes
     */
    public double calculateCosineSimilarity(RecetteEntity recipe1, RecetteEntity recipe2) {
        
        double[] embedding1 = generateRecipeEmbedding(recipe1);
        double[] embedding2 = generateRecipeEmbedding(recipe2);
        
        if (embedding1.length == 0 || embedding2.length == 0) {
            return 0.0;
        }
        
        return cosineSimilarity(embedding1, embedding2);
    }
    
    /**
     * Calcule la similarité cosinus entre deux vecteurs
     */
    private double cosineSimilarity(double[] vectorA, double[] vectorB) {
        
        if (vectorA.length != vectorB.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * Trouve les K recettes les plus similaires basées sur les embeddings
     */
    public List<RecetteEntity> findMostSimilarRecipes(
            RecetteEntity targetRecipe, 
            List<RecetteEntity> candidateRecipes,
            int k) {
        
        double[] targetEmbedding = generateRecipeEmbedding(targetRecipe);
        
        if (targetEmbedding.length == 0) {
            return new ArrayList<>();
        }
        
        // Calculer les similarités
        Map<RecetteEntity, Double> similarities = new HashMap<>();
        
        for (RecetteEntity candidate : candidateRecipes) {
            if (!candidate.getId().equals(targetRecipe.getId())) {
                double[] candidateEmbedding = generateRecipeEmbedding(candidate);
                if (candidateEmbedding.length > 0) {
                    double similarity = cosineSimilarity(targetEmbedding, candidateEmbedding);
                    similarities.put(candidate, similarity);
                }
            }
        }
        
        // Trier par similarité décroissante
        return similarities.entrySet().stream()
            .sorted(Map.Entry.<RecetteEntity, Double>comparingByValue().reversed())
            .limit(k)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Analyse de sentiment sur les commentaires
     * Retourne un score entre -1 (très négatif) et 1 (très positif)
     */
    public double analyzeSentiment(String commentText) {
        
        try (VertexAI vertexAI = new VertexAI(projectId, location)) {
            
            GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);
            
            String prompt = String.format(
                "Analyse le sentiment de ce commentaire sur une recette.\n" +
                "Commentaire: \"%s\"\n\n" +
                "Retourne UN SEUL nombre décimal entre -1 (très négatif) et 1 (très positif).\n" +
                "Exemples:\n" +
                "- \"Délicieux, mes enfants ont adoré!\" → 0.9\n" +
                "- \"Pas terrible, trop salé\" → -0.6\n" +
                "- \"Moyen, rien d'exceptionnel\" → 0.1\n\n" +
                "Réponse (juste le nombre):",
                commentText
            );
            
            GenerateContentResponse response = model.generateContent(prompt);
            String sentimentText = ResponseHandler.getText(response).trim();
            
            // Extraire le nombre
            sentimentText = sentimentText.replaceAll("[^0-9.-]", "");
            
            try {
                double sentiment = Double.parseDouble(sentimentText);
                // Borner entre -1 et 1
                return Math.max(-1.0, Math.min(1.0, sentiment));
            } catch (NumberFormatException e) {
                return 0.0; // Neutre par défaut
            }
            
        } catch (Exception e) {
            logger.error("Erreur analyse sentiment", e);
            return 0.0;
        }
    }
    
    /**
     * Calcule le sentiment moyen pour une recette basé sur ses commentaires
     */
    public double calculateAverageSentiment(List<CommentaireDocument> commentaires) {
        
        if (commentaires == null || commentaires.isEmpty()) {
            return 0.0;
        }
        
        double totalSentiment = 0.0;
        int count = 0;
        
        for (CommentaireDocument comment : commentaires) {
            double sentiment = analyzeSentiment(comment.getContenu());
            totalSentiment += sentiment;
            count++;
        }
        
        return count > 0 ? totalSentiment / count : 0.0;
    }
    
    /**
     * Extrait les mots-clés importants d'une recette avec NLP
     */
    public List<String> extractKeywords(RecetteEntity recette) {
        
        try (VertexAI vertexAI = new VertexAI(projectId, location)) {
            
            GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);
            
            String recipeText = buildRecipeText(recette);
            
            String prompt = String.format(
                "Extrait les 10 mots-clés les plus importants de cette recette.\n" +
                "Recette: %s\n\n" +
                "Retourne uniquement les mots-clés séparés par des virgules.\n" +
                "Exemple: poulet, tomate, basilic, italien, grillé\n" +
                "Mots-clés:",
                recipeText
            );
            
            GenerateContentResponse response = model.generateContent(prompt);
            String keywordsText = ResponseHandler.getText(response);
            
            // Parser les mots-clés
            return Arrays.stream(keywordsText.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .limit(10)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Erreur extraction mots-clés", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Recherche sémantique: trouve des recettes similaires à une requête texte
     */
    public List<RecetteEntity> semanticSearch(String query, List<RecetteEntity> allRecipes, int limit) {
        try {
            // 1. Préparer les Headers pour dire "C'est du JSON"
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. Créer le corps de la requête (Le "Payload")
            Map<String, Object> map = new HashMap<>();
            map.put("query", query);
            map.put("limit", limit);

            // 3. Créer l'entité qui combine Headers + Body
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(map, headers);

            // 4. Utiliser postForObject avec l'entité
            Map<String, Object> response = restTemplate.postForObject(
                mlServiceUrl + "/search/semantic", 
                entity, // On envoie l'entity ici, pas juste la map
                Map.class
            );

            // ... reste de ton code (traitement des resultIds) ...
            List<Integer> resultIds = (List<Integer>) response.get("results");
            
            return resultIds.stream()
                .map(id -> allRecipes.stream()
                    .filter(r -> r.getId().equals(Long.valueOf(id)))
                    .findFirst()
                    .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Erreur de communication : " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Détecte automatiquement les catégories/tags d'une recette avec NLP
     */
    public List<String> autoDetectCategories(RecetteEntity recette) {
        
        try (VertexAI vertexAI = new VertexAI(projectId, location)) {
            
            GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAI);
            
            String recipeText = buildRecipeText(recette);
            
            String prompt = String.format(
                "Analyse cette recette et attribue-lui des catégories/tags pertinents.\n" +
                "Recette: %s\n\n" +
                "Catégories possibles:\n" +
                "- Type de repas: petit-déjeuner, déjeuner, dîner, apéritif, dessert\n" +
                "- Saison: printemps, été, automne, hiver\n" +
                "- Occasion: quotidien, fête, enfants, romantique\n" +
                "- Régime: végétarien, vegan, sans-gluten, healthy\n" +
                "- Techniques: grillé, bouilli, frit, cru, mariné\n\n" +
                "Retourne 5-8 tags séparés par des virgules.\n" +
                "Tags:",
                recipeText.length() > 500 ? recipeText.substring(0, 500) : recipeText
            );
            
            GenerateContentResponse response = model.generateContent(prompt);
            String tagsText = ResponseHandler.getText(response);
            
            return Arrays.stream(tagsText.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .limit(8)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Erreur détection catégories", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Normalise un vecteur (norme L2)
     */
    private double[] normalizeVector(double[] vector) {
        double norm = 0.0;
        
        for (double v : vector) {
            norm += v * v;
        }
        
        norm = Math.sqrt(norm);
        
        if (norm == 0.0) {
            return vector;
        }
        
        double[] normalized = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        
        return normalized;
    }
    
    /**
     * Nettoie le cache des embeddings (à appeler périodiquement)
     */
    public void clearEmbeddingsCache() {
        recipeEmbeddingsCache.clear();
        logger.info("Cache d'embeddings nettoyé");
    }
    
    /**
     * Taille du cache
     */
    public int getCacheSize() {
        return recipeEmbeddingsCache.size();
    }
}
