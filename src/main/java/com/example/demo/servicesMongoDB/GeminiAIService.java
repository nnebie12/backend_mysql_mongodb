package com.example.demo.servicesMongoDB;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAIService {

	@Value("${GOOGLE_API_KEY}")
	private String apiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiAIService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Génère une recommandation personnalisée via Claude AI
     */
    public AIRecommendationResponse generatePersonalizedRecommendation(
            String profil, 
            Integer scoreEngagement,
            List<String> categories,
            List<String> types) {
        
        String prompt = buildPersonalizedPrompt(profil, scoreEngagement, categories, types);
        return callGeminiAPI(prompt);
    }

    /**
     * Génère une recommandation saisonnière via Claude AI
     */
    public AIRecommendationResponse generateSeasonalRecommendation(
            String saison,
            List<String> ingredients) {
        
        String prompt = buildSeasonalPrompt(saison, ingredients);
        return callGeminiAPI(prompt);
    }

    /**
     * Génère une recommandation basée sur les habitudes
     */
    public AIRecommendationResponse generateHabitBasedRecommendation(
            String typeRecette,
            String tempsPreparation,
            String difficulte,
            List<String> categoriesPreferees) {
        
        String prompt = buildHabitPrompt(typeRecette, tempsPreparation, difficulte, categoriesPreferees);
        return callGeminiAPI(prompt);
    }

    /**
     * Génère une recommandation par créneau horaire
     */
    public AIRecommendationResponse generateTimeslotRecommendation(
            String creneau,
            List<String> preferences) {
        
        String prompt = buildTimeslotPrompt(creneau, preferences);
        return callGeminiAPI(prompt);
    }

    /**
     * Génère une recommandation d'engagement
     */
    public AIRecommendationResponse generateEngagementRecommendation(
            Integer engagementScore) {
        
        String prompt = buildEngagementPrompt(engagementScore);
        return callGeminiAPI(prompt);
    }

    /**
     * Appel générique à l'API Gemini (Remplace callClaudeAPI)
     */
    private AIRecommendationResponse callGeminiAPI(String prompt) {
        try {
            // 1. Préparer les headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. Préparer le body au format Google Gemini
            // Structure : { "contents": [ { "parts": [ { "text": "votre prompt" } ] } ] }
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> parts = new HashMap<>();
            parts.put("parts", List.of(textPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(parts));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 3. Appel API (Notez l'ajout de la clé dans l'URL)
            ResponseEntity<String> response = restTemplate.exchange(
                GEMINI_API_URL + apiKey,
                HttpMethod.POST,
                request,
                String.class
            );

            return parseGeminiResponse(response.getBody());

        } catch (Exception e) {
            System.err.println("Erreur API Gemini: " + e.getMessage());
            return getFallbackRecommendation();
        }
    }

    /**
     * Parse la réponse de Gemini
     */
    private AIRecommendationResponse parseGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            // Chemin dans le JSON Gemini : candidates[0].content.parts[0].text
            String textContent = root.path("candidates")
                                     .get(0)
                                     .path("content")
                                     .path("parts")
                                     .get(0)
                                     .path("text")
                                     .asText();
            
            // Nettoyage Markdown
            textContent = textContent.replaceAll("```json\\n?", "")
                                   .replaceAll("```\\n?", "")
                                   .trim();
            
            JsonNode recommendationData = objectMapper.readTree(textContent);
            
            AIRecommendationResponse response = new AIRecommendationResponse();
            response.setSuccess(true);
            
            // Le reste de votre logique de mapping reste identique...
            List<RecommendationDetail> details = new ArrayList<>();
            JsonNode recommendations = recommendationData.path("recommendations");
            // ... (boucle for identique à votre code Claude)
            
            response.setRecommendations(details);
            response.setScore(recommendationData.path("score").asDouble(75.0));
            
            return response;
        } catch (Exception e) {
            return getFallbackRecommendation();
        }
    }

    /**
     * Recommandations de secours en cas d'erreur
     */
    private AIRecommendationResponse getFallbackRecommendation() {
        AIRecommendationResponse response = new AIRecommendationResponse();
        response.setSuccess(false);
        
        List<RecommendationDetail> fallback = new ArrayList<>();
        
        RecommendationDetail detail1 = new RecommendationDetail();
        detail1.setTitre("Recettes Populaires");
        detail1.setDescription("Découvrez nos recettes les plus appréciées par la communauté");
        detail1.setLien("/recettes/populaires");
        detail1.setTags(List.of("populaire", "communauté", "favoris"));
        detail1.setRaison("Ces recettes plaisent à la majorité des utilisateurs");
        
        RecommendationDetail detail2 = new RecommendationDetail();
        detail2.setTitre("Recettes Rapides");
        detail2.setDescription("Des recettes délicieuses prêtes en moins de 30 minutes");
        detail2.setLien("/recettes/rapides");
        detail2.setTags(List.of("rapide", "facile", "30min"));
        detail2.setRaison("Idéal pour les emplois du temps chargés");
        
        RecommendationDetail detail3 = new RecommendationDetail();
        detail3.setTitre("Recettes de Saison");
        detail3.setDescription("Profitez des ingrédients frais et de saison");
        detail3.setLien("/recettes/saison");
        detail3.setTags(List.of("saison", "frais", "local"));
        detail3.setRaison("Les meilleurs ingrédients du moment");
        
        fallback.add(detail1);
        fallback.add(detail2);
        fallback.add(detail3);
        
        response.setRecommendations(fallback);
        response.setScore(50.0);
        
        return response;
    }

    // ============= Construction des prompts =============

    private String buildPersonalizedPrompt(String profil, Integer scoreEngagement, 
                                          List<String> categories, List<String> types) {
        return String.format("""
            Tu es un assistant spécialisé dans les recommandations de recettes culinaires.
            
            Données utilisateur :
            - Profil : %s
            - Score d'engagement : %d/100
            - Catégories préférées : %s
            - Types de recettes préférés : %s
            
            Ta mission : Génère 3 recommandations de recettes personnalisées pour cet utilisateur.
            
            IMPORTANT : Réponds UNIQUEMENT avec un objet JSON valide suivant exactement cette structure (sans markdown, sans préambule) :
            
            {
              "recommendations": [
                {
                  "titre": "Titre de la recette",
                  "description": "Description engageante de 2-3 phrases",
                  "lien": "/recettes/[categorie]/[id]",
                  "tags": ["tag1", "tag2", "tag3"],
                  "raison": "Pourquoi cette recette est pertinente pour l'utilisateur"
                }
              ],
              "score": 75.5
            }
            
            Les recommandations doivent être variées, pertinentes et adaptées au profil de l'utilisateur.
            """,
            profil != null ? profil : "Nouveau",
            scoreEngagement != null ? scoreEngagement : 0,
            categories != null && !categories.isEmpty() ? String.join(", ", categories) : "Non défini",
            types != null && !types.isEmpty() ? String.join(", ", types) : "Non défini"
        );
    }

    private String buildSeasonalPrompt(String saison, List<String> ingredients) {
        return String.format("""
            Tu es un assistant spécialisé dans les recommandations de recettes saisonnières.
            
            Saison actuelle : %s
            Préférences utilisateur : %s
            
            Ta mission : Génère 3 recommandations de recettes de %s avec des ingrédients de saison.
            
            IMPORTANT : Réponds UNIQUEMENT avec un objet JSON valide suivant exactement cette structure (sans markdown, sans préambule) :
            
            {
              "recommendations": [
                {
                  "titre": "Titre de la recette",
                  "description": "Description mettant en avant les ingrédients de saison",
                  "lien": "/recettes/saison/%s/[id]",
                  "tags": ["%s", "ingredient1", "ingredient2"],
                  "raison": "Pourquoi cette recette est parfaite pour cette saison"
                }
              ],
              "score": 80.0
            }
            
            Privilégie les ingrédients frais et locaux disponibles en %s.
            """,
            saison != null ? saison.toLowerCase() : "printemps",
            ingredients != null && !ingredients.isEmpty() ? String.join(", ", ingredients) : "Ouvert à tout",
            saison != null ? saison.toLowerCase() : "printemps",
            saison != null ? saison.toLowerCase() : "printemps",
            saison != null ? saison.toLowerCase() : "printemps",
            saison != null ? saison.toLowerCase() : "printemps"
        );
    }

    private String buildHabitPrompt(String typeRecette, String tempsPreparation, 
                                   String difficulte, List<String> categoriesPreferees) {
        return String.format("""
            Tu es un assistant spécialisé dans les recommandations basées sur les habitudes culinaires.
            
            Habitudes de l'utilisateur :
            - Type de recette préféré : %s
            - Temps de préparation préféré : %s
            - Niveau de difficulté : %s
            - Catégories fréquemment consultées : %s
            
            Ta mission : Génère 3 recommandations de recettes alignées avec ces habitudes.
            
            IMPORTANT : Réponds UNIQUEMENT avec un objet JSON valide (sans markdown) :
            
            {
              "recommendations": [
                {
                  "titre": "Titre de la recette",
                  "description": "Description alignée avec les habitudes",
                  "lien": "/recettes/[categorie]/[id]",
                  "tags": ["habitude1", "habitude2", "habitude3"],
                  "raison": "Comment cette recette correspond aux habitudes de l'utilisateur"
                }
              ],
              "score": 85.0
            }
            """,
            typeRecette != null ? typeRecette : "Varié",
            tempsPreparation != null ? tempsPreparation : "Moyen",
            difficulte != null ? difficulte : "Intermédiaire",
            categoriesPreferees != null && !categoriesPreferees.isEmpty() ? String.join(", ", categoriesPreferees) : "Diverses"
        );
    }

    private String buildTimeslotPrompt(String creneau, List<String> preferences) {
        return String.format("""
            Tu es un assistant spécialisé dans les recommandations de recettes par moment de la journée.
            
            Créneau horaire : %s
            Préférences : %s
            
            Ta mission : Génère 3 recommandations de recettes parfaites pour le %s.
            
            IMPORTANT : Réponds UNIQUEMENT avec un objet JSON valide (sans markdown) :
            
            {
              "recommendations": [
                {
                  "titre": "Titre de la recette",
                  "description": "Description adaptée au %s",
                  "lien": "/recettes/creneau/%s/[id]",
                  "tags": ["%s", "rapide", "tag3"],
                  "raison": "Pourquoi cette recette est idéale pour ce moment"
                }
              ],
              "score": 82.0
            }
            """,
            creneau != null ? creneau : "repas",
            preferences != null && !preferences.isEmpty() ? String.join(", ", preferences) : "Standard",
            creneau != null ? creneau : "repas",
            creneau != null ? creneau : "repas",
            creneau != null ? creneau : "repas",
            creneau != null ? creneau : "repas"
        );
    }

    private String buildEngagementPrompt(Integer engagementScore) {
        boolean isLowEngagement = engagementScore != null && engagementScore < 30;
        
        return String.format("""
            Tu es un assistant spécialisé dans l'amélioration de l'engagement utilisateur.
            
            Score d'engagement actuel : %d/100
            %s
            
            Ta mission : Génère 3 recommandations pour %s l'utilisateur.
            
            IMPORTANT : Réponds UNIQUEMENT avec un objet JSON valide (sans markdown) :
            
            {
              "recommendations": [
                {
                  "titre": "Titre de la recette",
                  "description": "Description motivante et accessible",
                  "lien": "/recettes/[categorie]/[id]",
                  "tags": ["%s", "populaire", "tag3"],
                  "raison": "Comment cette recette peut améliorer l'engagement"
                }
              ],
              "score": %.1f
            }
            
            %s
            """,
            engagementScore != null ? engagementScore : 0,
            isLowEngagement ? "ATTENTION : L'utilisateur a un faible engagement, propose des recettes simples et populaires." : "L'utilisateur est actif, propose des recettes plus élaborées.",
            isLowEngagement ? "re-engager" : "maintenir l'engagement de",
            isLowEngagement ? "facile" : "challenge",
            isLowEngagement ? 65.0 : 88.0,
            isLowEngagement ? "Privilégie des recettes simples, rapides et populaires pour redonner confiance." : "Propose des recettes intéressantes qui maintiennent l'intérêt."
        );
    }

    // ============= Classes internes =============

    public static class AIRecommendationResponse {
        private boolean success;
        private List<RecommendationDetail> recommendations;
        private Double score;

        // Getters et Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public List<RecommendationDetail> getRecommendations() { return recommendations; }
        public void setRecommendations(List<RecommendationDetail> recommendations) { this.recommendations = recommendations; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
    }

    public static class RecommendationDetail {
        private String titre;
        private String description;
        private String lien;
        private List<String> tags;
        private String raison;

        // Getters et Setters
        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getLien() { return lien; }
        public void setLien(String lien) { this.lien = lien; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public String getRaison() { return raison; }
        public void setRaison(String raison) { this.raison = raison; }
    }
}