package com.example.demo.servicesMongoDB;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


@Service
public class OllamaService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.model:llama3}")
    private String defaultModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────────
    //  API principale
    // ─────────────────────────────────────────────────────────────────

    /**
     * Envoie un prompt au modèle Ollama et retourne la réponse textuelle.
     *
     * @param prompt  Le prompt complet à envoyer
     * @return        La réponse générée par le modèle, ou une chaîne vide en cas d'erreur
     */
    public String generate(String prompt) {
        return generate(prompt, defaultModel);
    }

    /**
     * Envoie un prompt à un modèle Ollama spécifique.
     *
     * @param prompt  Le prompt complet à envoyer
     * @param model   Nom du modèle (ex: "llama3", "mistral", "gemma2")
     * @return        La réponse générée
     */
    public String generate(String prompt, String model) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                "model",  model,
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                    "temperature", 0.3,   // Réponses plus déterministes
                    "num_predict", 512    // Limite de tokens
                )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/generate",
                HttpMethod.POST,
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object responseText = response.getBody().get("response");
                return responseText != null ? responseText.toString().trim() : "";
            }

            logger.warn("Ollama a répondu avec un statut inattendu : {}", response.getStatusCode());
            return "";

        } catch (Exception e) {
            logger.error("Erreur appel Ollama (modèle={}) : {}", model, e.getMessage());
            return "";
        }
    }

    /**
     * Vérifie si le serveur Ollama est accessible.
     */
    public boolean isAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/api/tags", String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.warn("Ollama non disponible sur {} : {}", baseUrl, e.getMessage());
            return false;
        }
    }

    

    /**
     * Analyse le sentiment d'un commentaire culinaire.
     * Retourne un nombre décimal entre -1 (négatif) et 1 (positif).
     */
    public double analyzeSentiment(String text) {
        String prompt = String.format(
            "Analyse le sentiment de ce commentaire sur une recette de cuisine.\n" +
            "Commentaire : \"%s\"\n\n" +
            "IMPORTANT : réponds UNIQUEMENT avec un nombre décimal entre -1.0 et 1.0.\n" +
            "  -1.0 = très négatif\n" +
            "   0.0 = neutre\n" +
            "   1.0 = très positif\n\n" +
            "Exemples :\n" +
            "  \"Délicieux, mes enfants ont adoré !\" → 0.9\n" +
            "  \"Pas terrible, trop salé\"            → -0.6\n" +
            "  \"Recette correcte\"                  → 0.1\n\n" +
            "Réponse (un seul nombre) :",
            text
        );

        String raw = generate(prompt);
        return parseDouble(raw, 0.0, -1.0, 1.0);
    }

    /**
     * Extrait les mots-clés culinaires d'un texte concaténé de commentaires.
     * Retourne une liste de mots séparés par des virgules.
     */
    public String extractKeywordsFromText(String combinedText) {
        String prompt = String.format(
            "Analyse ces commentaires d'un utilisateur sur des recettes de cuisine.\n" +
            "Extrait les 10 mots-clés culinaires les plus représentatifs.\n\n" +
            "Commentaires : %s\n\n" +
            "IMPORTANT : réponds UNIQUEMENT avec les mots-clés séparés par des virgules, sans ponctuation ni explication.\n" +
            "Exemple : poulet, rapide, épicé, végétarien, grillé",
            combinedText.length() > 1500 ? combinedText.substring(0, 1500) : combinedText
        );
        return generate(prompt);
    }

    /**
     * Génère un résumé du profil culinaire d'un utilisateur.
     */
    public String generateProfileSummary(double sentimentScore,
                                          String sentimentLabel,
                                          Object topics,
                                          Object searchTerms,
                                          double searchSuccessRate) {
        String prompt = String.format(
            "Tu es un expert en analyse culinaire. " +
            "Rédige en 2-3 phrases un résumé du profil culinaire de cet utilisateur.\n\n" +
            "Données NLP :\n" +
            "  - Score sentiment moyen  : %.2f (%s)\n" +
            "  - Topics dominants       : %s\n" +
            "  - Termes recherchés      : %s\n" +
            "  - Taux recherches utiles : %.0f%%\n\n" +
            "Sois concis, positif et personnalisé. Ne cite pas de chiffres bruts.",
            sentimentScore, sentimentLabel, topics, searchTerms, searchSuccessRate
        );
        return generate(prompt);
    }

    /**
     * Extrait les mots-clés d'une recette à partir de son texte complet.
     */
    public String extractRecipeKeywords(String recipeText) {
        String prompt = String.format(
            "Extrait les 10 mots-clés les plus importants de cette recette.\n" +
            "Recette : %s\n\n" +
            "IMPORTANT : réponds UNIQUEMENT avec les mots-clés séparés par des virgules.\n" +
            "Exemple : poulet, tomate, basilic, grillé, italien",
            recipeText.length() > 1000 ? recipeText.substring(0, 1000) : recipeText
        );
        return generate(prompt);
    }

    /**
     * Détecte automatiquement les catégories / tags d'une recette.
     */
    public String autoDetectCategories(String recipeText) {
        String prompt = String.format(
            "Analyse cette recette et attribue-lui des catégories / tags pertinents.\n" +
            "Recette : %s\n\n" +
            "Catégories possibles :\n" +
            "  - Repas     : petit-déjeuner, déjeuner, dîner, apéritif, dessert\n" +
            "  - Saison    : printemps, été, automne, hiver\n" +
            "  - Occasion  : quotidien, fête, enfants, romantique\n" +
            "  - Régime    : végétarien, vegan, sans-gluten, healthy\n" +
            "  - Technique : grillé, bouilli, frit, cru, mariné\n\n" +
            "IMPORTANT : réponds UNIQUEMENT avec 5 à 8 tags séparés par des virgules.",
            recipeText.length() > 600 ? recipeText.substring(0, 600) : recipeText
        );
        return generate(prompt);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Utilitaires
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parse un nombre décimal depuis une réponse brute d'Ollama,
     * en le bornant dans [min, max].
     */
    public double parseDouble(String raw, double defaultValue, double min, double max) {
        if (raw == null || raw.isBlank()) return defaultValue;
        try {
            // Garder uniquement les caractères numériques (chiffres, point, signe)
            String cleaned = raw.replaceAll("[^0-9.\\-]", "").trim();
            // Prendre le premier token si plusieurs nombres
            String firstToken = cleaned.split("\\s+")[0];
            double value = Double.parseDouble(firstToken);
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException e) {
            logger.debug("Impossible de parser '{}' en double, retour à {}", raw, defaultValue);
            return defaultValue;
        }
    }
}