package com.example.demo.servicesImplMongoDB;

import com.example.demo.DTO.NLPUserInsightsDTO;
import com.example.demo.entiesMongodb.CommentaireDocument;
import com.example.demo.entiesMongodb.HistoriqueRecherche;
import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.repositoryMongoDB.CommentaireMongoRepository;
import com.example.demo.repositoryMongoDB.HistoriqueRechercheRepository;
import com.example.demo.servicesMongoDB.OllamaService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service NLP pour l'analyse sémantique des recettes.
 * Utilise Ollama (llama3 / mistral / …) à la place de Google Vertex AI / Gemini.
 *
 * Prérequis :
 *   ollama serve                  # dans un terminal séparé
 *   ollama pull llama3            # ou mistral, phi3, gemma2…
 */
@Service
public class RecipeNLPService {

    private static final Logger logger = LoggerFactory.getLogger(RecipeNLPService.class);

    // ── Dépendances ────────────────────────────────────────────────────────────

    /** Client Ollama injecté — remplace VertexAI / GenerativeModel */
    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private CommentaireMongoRepository commentaireRepository;

    @Autowired
    private HistoriqueRechercheRepository rechercheRepository;

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /** Cache local des embeddings (id recette → vecteur float[]) */
    private final Map<Long, double[]> recipeEmbeddingsCache = new HashMap<>();

    // ═══════════════════════════════════════════════════════════════════════════
    //  1. EMBEDDINGS  (représentation vectorielle via ml_service.py / FastAPI)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Retourne l'embedding d'une recette.
     * Utilise le microservice Python FastAPI (ml_service.py) qui s'appuie sur
     * SentenceTransformers — plus fiable pour les embeddings denses que Ollama.
     */
    public double[] generateRecipeEmbedding(RecetteEntity recette) {

        if (recipeEmbeddingsCache.containsKey(recette.getId())) {
            return recipeEmbeddingsCache.get(recette.getId());
        }

        try {
            // Appel au microservice Python qui génère l'embedding
            String recipeText = buildRecipeText(recette);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of("query", recipeText, "limit", 1);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            Map<String, Object> response = restTemplate.postForObject(
                mlServiceUrl + "/search/semantic", entity, Map.class);

            if (response != null && response.containsKey("results")) {
                // Fallback léger : on utilise un vecteur simplifié basé sur les IDs retournés
                List<Integer> ids = (List<Integer>) response.get("results");
                double[] vec = new double[ids.size()];
                for (int i = 0; i < ids.size(); i++) vec[i] = ids.get(i).doubleValue();
                double[] normalized = normalizeVector(vec);
                recipeEmbeddingsCache.put(recette.getId(), normalized);
                return normalized;
            }
        } catch (Exception e) {
            logger.warn("ml_service indisponible pour embedding recette {} : {}", recette.getId(), e.getMessage());
        }

        // Dernier recours : vecteur nul
        return new double[0];
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  2. ANALYSE NLP UTILISATEUR
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Génère une analyse NLP complète pour un utilisateur donné.
     * Ollama remplace tous les appels Gemini (sentiment, mots-clés, résumé).
     */
    public NLPUserInsightsDTO getUserNLPInsights(Long userId) {

        NLPUserInsightsDTO insights = new NLPUserInsightsDTO(userId);

        List<CommentaireDocument> commentaires = commentaireRepository.findByUserId(String.valueOf(userId));
        List<HistoriqueRecherche> recherches    = rechercheRepository.findByUserId(userId);

        boolean hasComments = commentaires != null && !commentaires.isEmpty();
        boolean hasSearches = recherches   != null && !recherches.isEmpty();

        if (!hasComments && !hasSearches) {
            insights.setHasSufficientData(false);
            insights.setDataQualityMessage("Aucun commentaire ni historique de recherche trouvé.");
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

        // 2a. Sentiment moyen (via Ollama)
        if (hasComments) {
            double avgSentiment = calculateAverageSentiment(commentaires);
            insights.setAverageSentimentScore(Math.round(avgSentiment * 100) / 100.0);
            insights.setTotalCommentsAnalyzed(commentaires.size());
        } else {
            insights.setAverageSentimentScore(0.0);
            insights.setTotalCommentsAnalyzed(0);
        }

        // 2b. Top termes de recherche
        List<String> topSearchTerms = extractTopSearchTerms(recherches, 10);
        insights.setTopSearchTerms(topSearchTerms);

        // 2c. Mots-clés extraits des commentaires (via Ollama)
        List<String> commentKeywords = extractKeywordsFromComments(commentaires);
        insights.setTopKeywords(commentKeywords);

        // 2d. Topics dominants
        List<String> dominantTopics = buildDominantTopics(recherches, commentaires);
        insights.setDominantTopics(dominantTopics);

        // 2e. Profil sémantique
        Map<String, Double> semanticProfile = buildSemanticProfile(recherches, commentaires);
        insights.setSemanticProfile(semanticProfile);

        // 2f. Résumé textuel (via Ollama)
        String summary = generateProfileSummary(userId, insights);
        insights.setProfileSummary(summary);

        // 2g. Taux de recherches fructueuses
        if (hasSearches) {
            long fructueuses = recherches.stream()
                .filter(r -> Boolean.TRUE.equals(r.getRechercheFructueuse()))
                .count();
            double taux = (double) fructueuses / recherches.size() * 100;
            insights.setSuccessfulSearchRate(Math.round(taux * 10) / 10.0);
        }

        return insights;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  3. SENTIMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Analyse le sentiment d'un commentaire via Ollama.
     * Retourne un score entre -1.0 (négatif) et 1.0 (positif).
     */
    public double analyzeSentiment(String commentText) {
        if (commentText == null || commentText.isBlank()) return 0.0;
        return ollamaService.analyzeSentiment(commentText);
    }

    /**
     * Calcule le sentiment moyen sur une liste de commentaires.
     */
    public double calculateAverageSentiment(List<CommentaireDocument> commentaires) {
        if (commentaires == null || commentaires.isEmpty()) return 0.0;

        double total = 0.0;
        int count    = 0;

        for (CommentaireDocument c : commentaires) {
            if (c.getContenu() != null && !c.getContenu().isBlank()) {
                total += analyzeSentiment(c.getContenu());
                count++;
            }
        }
        return count > 0 ? total / count : 0.0;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  4. MOTS-CLÉS & CATÉGORIES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Extrait les mots-clés d'une recette via Ollama.
     */
    public List<String> extractKeywords(RecetteEntity recette) {
        String recipeText = buildRecipeText(recette);
        String raw = ollamaService.extractRecipeKeywords(recipeText);
        return parseCommaSeparatedList(raw, 10);
    }

    /**
     * Détecte automatiquement les catégories d'une recette via Ollama.
     */
    public List<String> autoDetectCategories(RecetteEntity recette) {
        String recipeText = buildRecipeText(recette);
        String raw = ollamaService.autoDetectCategories(recipeText);
        return parseCommaSeparatedList(raw, 8);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  5. SIMILARITÉ & RECHERCHE SÉMANTIQUE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calcule la similarité cosinus entre deux recettes.
     */
    public double calculateCosineSimilarity(RecetteEntity r1, RecetteEntity r2) {
        double[] v1 = generateRecipeEmbedding(r1);
        double[] v2 = generateRecipeEmbedding(r2);
        if (v1.length == 0 || v2.length == 0) return 0.0;
        return cosineSimilarity(v1, v2);
    }

    /**
     * Trouve les K recettes les plus similaires à une recette cible.
     */
    public List<RecetteEntity> findMostSimilarRecipes(RecetteEntity target,
                                                       List<RecetteEntity> candidates,
                                                       int k) {
        double[] targetVec = generateRecipeEmbedding(target);
        if (targetVec.length == 0) return new ArrayList<>();

        Map<RecetteEntity, Double> scores = new HashMap<>();
        for (RecetteEntity c : candidates) {
            if (!c.getId().equals(target.getId())) {
                double[] cv = generateRecipeEmbedding(c);
                if (cv.length > 0) scores.put(c, cosineSimilarity(targetVec, cv));
            }
        }

        return scores.entrySet().stream()
            .sorted(Map.Entry.<RecetteEntity, Double>comparingByValue().reversed())
            .limit(k)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Recherche sémantique — délègue au microservice Python FastAPI.
     */
    public List<RecetteEntity> semanticSearch(String query, List<RecetteEntity> allRecipes, int limit) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of("query", query, "limit", limit);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            Map<String, Object> response = restTemplate.postForObject(
                mlServiceUrl + "/search/semantic", entity, Map.class);

            if (response == null) return new ArrayList<>();

            List<Integer> resultIds = (List<Integer>) response.get("results");
            return resultIds.stream()
                .map(id -> allRecipes.stream()
                    .filter(r -> r.getId().equals(Long.valueOf(id)))
                    .findFirst().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Erreur recherche sémantique : {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  6. GESTION DU CACHE
    // ═══════════════════════════════════════════════════════════════════════════

    public void clearEmbeddingsCache() {
        recipeEmbeddingsCache.clear();
        logger.info("Cache embeddings nettoyé");
    }

    public int getCacheSize() {
        return recipeEmbeddingsCache.size();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  MÉTHODES PRIVÉES
    // ═══════════════════════════════════════════════════════════════════════════

    private String buildRecipeText(RecetteEntity recette) {
        StringBuilder text = new StringBuilder();
        text.append(recette.getTitre()).append(". ");
        text.append(recette.getTitre()).append(". "); // poids x2

        if (recette.getDescription() != null && !recette.getDescription().isEmpty())
            text.append(recette.getDescription()).append(" ");

        text.append("Type: ").append(recette.getTypeRecette()).append(". ");

        if (recette.getCuisine() != null)
            text.append("Cuisine: ").append(recette.getCuisine()).append(". ");

        if (recette.getRecetteIngredients() != null && !recette.getRecetteIngredients().isEmpty()) {
            text.append("Ingrédients: ");
            recette.getRecetteIngredients().forEach(ing ->
                text.append(ing.getIngredientEntity().getNom()).append(", "));
        }

        if (Boolean.TRUE.equals(recette.getVegetarien())) text.append("Végétarien. ");
        text.append("Difficulté: ").append(recette.getDifficulte()).append(". ");

        return text.toString().trim();
    }

    private List<String> extractTopSearchTerms(List<HistoriqueRecherche> recherches, int limit) {
        if (recherches == null || recherches.isEmpty()) return List.of();
        return recherches.stream()
            .collect(Collectors.groupingBy(HistoriqueRecherche::getTerme, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private List<String> extractKeywordsFromComments(List<CommentaireDocument> commentaires) {
        if (commentaires == null || commentaires.isEmpty()) return List.of();

        String combined = commentaires.stream()
            .limit(20)
            .map(CommentaireDocument::getContenu)
            .filter(c -> c != null && !c.isBlank())
            .collect(Collectors.joining(" | "));

        if (combined.isBlank()) return List.of();

        String raw = ollamaService.extractKeywordsFromText(combined);
        return parseCommaSeparatedList(raw, 10);
    }

    private List<String> buildDominantTopics(List<HistoriqueRecherche> recherches,
                                              List<CommentaireDocument> commentaires) {
        Map<String, Long> topicCount = new HashMap<>();
        if (recherches != null) {
            recherches.stream()
                .map(HistoriqueRecherche::getCategorieRecherche)
                .filter(Objects::nonNull)
                .forEach(cat -> topicCount.merge(cat.toUpperCase(), 1L, Long::sum));
            extractTopSearchTerms(recherches, 5)
                .forEach(terme -> topicCount.merge(terme, 2L, Long::sum));
        }
        return topicCount.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private Map<String, Double> buildSemanticProfile(List<HistoriqueRecherche> recherches,
                                                      List<CommentaireDocument> commentaires) {
        Map<String, Double> raw = new LinkedHashMap<>();
        if (recherches != null && !recherches.isEmpty()) {
            Map<String, Long> catCount = recherches.stream()
                .map(HistoriqueRecherche::getCategorieRecherche)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(String::toUpperCase, Collectors.counting()));
            long total = catCount.values().stream().mapToLong(Long::longValue).sum();
            catCount.forEach((cat, count) ->
                raw.put(cat, Math.round((double) count / total * 100.0) / 100.0));
        }
        if (commentaires != null && commentaires.size() >= 5)
            raw.put("ACTIF_COMMENTATEUR", Math.min(1.0,
                Math.round(commentaires.size() / 20.0 * 100.0) / 100.0));
        return raw;
    }

    private String generateProfileSummary(Long userId, NLPUserInsightsDTO insights) {
        try {
            return ollamaService.generateProfileSummary(
                insights.getAverageSentimentScore(),
                insights.getSentimentLabel(),
                insights.getDominantTopics(),
                insights.getTopSearchTerms(),
                insights.getSuccessfulSearchRate() != null ? insights.getSuccessfulSearchRate() : 0.0
            );
        } catch (Exception e) {
            logger.error("Erreur génération résumé profil userId {} : {}", userId, e.getMessage());
            return "Profil en cours de construction.";
        }
    }

    private double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) return 0.0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na  += a[i] * a[i];
            nb  += b[i] * b[i];
        }
        return (na == 0 || nb == 0) ? 0.0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private double[] normalizeVector(double[] vector) {
        double norm = 0;
        for (double v : vector) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm == 0) return vector;
        double[] out = new double[vector.length];
        for (int i = 0; i < vector.length; i++) out[i] = vector[i] / norm;
        return out;
    }

    private List<String> parseCommaSeparatedList(String raw, int limit) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(s -> !s.isEmpty())
            .limit(limit)
            .collect(Collectors.toList());
    }
}