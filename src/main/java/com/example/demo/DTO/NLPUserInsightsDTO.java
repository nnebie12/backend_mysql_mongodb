package com.example.demo.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO structurant l'analyse NLP complète d'un utilisateur.
 * Agrège sentiment, mots-clés, topics et profil sémantique.
 */
public class NLPUserInsightsDTO {

    private Long userId;
    private LocalDateTime generatedAt;

    // ── Analyse des commentaires ──────────────────────────────────────────────
    /** Score moyen entre -1 (très négatif) et 1 (très positif) */
    private Double averageSentimentScore;
    /** Label lisible : "Très positif", "Positif", "Neutre", "Négatif", "Très négatif" */
    private String sentimentLabel;
    /** Nombre de commentaires analysés */
    private Integer totalCommentsAnalyzed;

    // ── Mots-clés & topics ───────────────────────────────────────────────────
    /** Top 10 mots-clés extraits des recherches et commentaires */
    private List<String> topKeywords;
    /** Topics dominants (ex : "végétarien", "rapide", "dessert") */
    private List<String> dominantTopics;

    // ── Profil sémantique ────────────────────────────────────────────────────
    /**
     * Catégories préférées avec leur poids relatif.
     * Ex : {"FACILE": 0.6, "VÉGÉTARIEN": 0.3, "DESSERT": 0.1}
     */
    private Map<String, Double> semanticProfile;

    /** Résumé textuel généré par Gemini décrivant le profil culinaire */
    private String profileSummary;

    // ── Tendances de recherche ────────────────────────────────────────────────
    /** Termes les plus recherchés (top 10) */
    private List<String> topSearchTerms;
    /** Taux de recherches fructueuses (%) */
    private Double successfulSearchRate;

    // ── Métadonnées ───────────────────────────────────────────────────────────
    /** Indique si l'analyse s'appuie sur suffisamment de données */
    private Boolean hasSufficientData;
    /** Message d'information si les données sont insuffisantes */
    private String dataQualityMessage;

    // ── Constructeurs ─────────────────────────────────────────────────────────

    public NLPUserInsightsDTO() {
        this.generatedAt = LocalDateTime.now();
        this.hasSufficientData = false;
    }

    public NLPUserInsightsDTO(Long userId) {
        this();
        this.userId = userId;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public Double getAverageSentimentScore() { return averageSentimentScore; }
    public void setAverageSentimentScore(Double averageSentimentScore) {
        this.averageSentimentScore = averageSentimentScore;
        this.sentimentLabel = resolveSentimentLabel(averageSentimentScore);
    }

    public String getSentimentLabel() { return sentimentLabel; }
    public void setSentimentLabel(String sentimentLabel) { this.sentimentLabel = sentimentLabel; }

    public Integer getTotalCommentsAnalyzed() { return totalCommentsAnalyzed; }
    public void setTotalCommentsAnalyzed(Integer totalCommentsAnalyzed) {
        this.totalCommentsAnalyzed = totalCommentsAnalyzed;
    }

    public List<String> getTopKeywords() { return topKeywords; }
    public void setTopKeywords(List<String> topKeywords) { this.topKeywords = topKeywords; }

    public List<String> getDominantTopics() { return dominantTopics; }
    public void setDominantTopics(List<String> dominantTopics) { this.dominantTopics = dominantTopics; }

    public Map<String, Double> getSemanticProfile() { return semanticProfile; }
    public void setSemanticProfile(Map<String, Double> semanticProfile) {
        this.semanticProfile = semanticProfile;
    }

    public String getProfileSummary() { return profileSummary; }
    public void setProfileSummary(String profileSummary) { this.profileSummary = profileSummary; }

    public List<String> getTopSearchTerms() { return topSearchTerms; }
    public void setTopSearchTerms(List<String> topSearchTerms) { this.topSearchTerms = topSearchTerms; }

    public Double getSuccessfulSearchRate() { return successfulSearchRate; }
    public void setSuccessfulSearchRate(Double successfulSearchRate) {
        this.successfulSearchRate = successfulSearchRate;
    }

    public Boolean getHasSufficientData() { return hasSufficientData; }
    public void setHasSufficientData(Boolean hasSufficientData) {
        this.hasSufficientData = hasSufficientData;
    }

    public String getDataQualityMessage() { return dataQualityMessage; }
    public void setDataQualityMessage(String dataQualityMessage) {
        this.dataQualityMessage = dataQualityMessage;
    }

    // ── Helper privé ──────────────────────────────────────────────────────────

    private String resolveSentimentLabel(Double score) {
        if (score == null) return "Inconnu";
        if (score >  0.5) return "Très positif";
        if (score >  0.2) return "Positif";
        if (score > -0.2) return "Neutre";
        if (score > -0.5) return "Négatif";
        return "Très négatif";
    }
}