package com.example.demo.DTO;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import lombok.Data;

@Data
public class AnalysePatternsDTO {

    private Map<String, Object> patternsTemporels;
    private Map<String, Object> patternsNavigation;
    private Map<String, Object> patternsRecherche;
    private Map<String, Object> patternsSessions;
    private Map<String, Object> preferences;
    private List<String> sequencesFrequentes;
    private List<String> anomalies;
    private Double scorePredictibilite;
    private String erreur;

    // Constructeurs
    public AnalysePatternsDTO() {}

    public AnalysePatternsDTO(Map<String, Object> donneesAnalyse) {
        this.patternsTemporels = safeGetMap(donneesAnalyse, "patternsTemporels");
        this.patternsNavigation = safeGetMap(donneesAnalyse, "patternsNavigation");
        this.patternsRecherche = safeGetMap(donneesAnalyse, "patternsRecherche");
        this.patternsSessions = safeGetMap(donneesAnalyse, "patternsSessions");
        this.preferences = safeGetMap(donneesAnalyse, "preferences");
        this.sequencesFrequentes = safeGetStringList(donneesAnalyse, "sequencesFrequentes");
        this.anomalies = safeGetStringList(donneesAnalyse, "anomalies");
        this.scorePredictibilite = safeGetDouble(donneesAnalyse, "scorePredictibilite");
        this.erreur = safeGetString(donneesAnalyse, "erreur");
    }

    // Méthodes utilitaires pour la conversion sécurisée des types
    @SuppressWarnings("unchecked")
    private Map<String, Object> safeGetMap(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return new HashMap<>();
        }
        
        Object value = source.get(key);
        if (value == null) {
            return new HashMap<>();
        }
        
        try {
            if (value instanceof Map) {
                // Vérification supplémentaire pour s'assurer que c'est bien Map<String, Object>
                Map<?, ?> rawMap = (Map<?, ?>) value;
                Map<String, Object> result = new HashMap<>();
                
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    if (entry.getKey() instanceof String) {
                        result.put((String) entry.getKey(), entry.getValue());
                    }
                }
                return result;
            }
        } catch (ClassCastException e) {
            // Log l'erreur si nécessaire
            System.err.println("Erreur de cast pour la clé '" + key + "': " + e.getMessage());
        }
        
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<String> safeGetStringList(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return new ArrayList<>();
        }
        
        Object value = source.get(key);
        if (value == null) {
            return new ArrayList<>();
        }
        
        try {
            if (value instanceof List) {
                List<?> rawList = (List<?>) value;
                List<String> result = new ArrayList<>();
                
                for (Object item : rawList) {
                    if (item instanceof String) {
                        result.add((String) item);
                    } else if (item != null) {
                        result.add(item.toString());
                    }
                }
                return result;
            }
        } catch (ClassCastException e) {
            System.err.println("Erreur de cast pour la clé '" + key + "': " + e.getMessage());
        }
        
        return new ArrayList<>();
    }

    private Double safeGetDouble(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return null;
        }
        
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        
        try {
            if (value instanceof Double) {
                return (Double) value;
            } else if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
        } catch (NumberFormatException | ClassCastException e) {
            System.err.println("Erreur de conversion en Double pour la clé '" + key + "': " + e.getMessage());
        }
        
        return null;
    }

    private String safeGetString(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return null;
        }
        
        Object value = source.get(key);
        return value != null ? value.toString() : null;
    }

    // Méthodes utilitaires existantes
    public boolean hasError() {
        return erreur != null && !erreur.isEmpty();
    }

    public boolean isEmpty() {
        return (patternsTemporels == null || patternsTemporels.isEmpty()) && 
               (patternsNavigation == null || patternsNavigation.isEmpty()) &&
               (patternsRecherche == null || patternsRecherche.isEmpty()) && 
               (patternsSessions == null || patternsSessions.isEmpty()) &&
               (preferences == null || preferences.isEmpty()) && 
               (sequencesFrequentes == null || sequencesFrequentes.isEmpty()) &&
               (anomalies == null || anomalies.isEmpty()) && 
               scorePredictibilite == null;
    }

    // Méthodes utilitaires pour accéder aux données de manière sécurisée
    public Map<String, Object> getPatternsTemporelsSafe() {
        return patternsTemporels != null ? new HashMap<>(patternsTemporels) : new HashMap<>();
    }

    public Map<String, Object> getPatternsNavigationSafe() {
        return patternsNavigation != null ? new HashMap<>(patternsNavigation) : new HashMap<>();
    }

    public Map<String, Object> getPatternsRechercheSafe() {
        return patternsRecherche != null ? new HashMap<>(patternsRecherche) : new HashMap<>();
    }

    public Map<String, Object> getPatternsSessionsSafe() {
        return patternsSessions != null ? new HashMap<>(patternsSessions) : new HashMap<>();
    }

    public Map<String, Object> getPreferencesSafe() {
        return preferences != null ? new HashMap<>(preferences) : new HashMap<>();
    }

    public List<String> getSequencesFrequentesSafe() {
        return sequencesFrequentes != null ? new ArrayList<>(sequencesFrequentes) : new ArrayList<>();
    }

    public List<String> getAnomaliesSafe() {
        return anomalies != null ? new ArrayList<>(anomalies) : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "AnalysePatternsDTO{" +
                "patternsTemporels=" + patternsTemporels +
                ", patternsNavigation=" + patternsNavigation +
                ", patternsRecherche=" + patternsRecherche +
                ", patternsSessions=" + patternsSessions +
                ", preferences=" + preferences +
                ", sequencesFrequentes=" + sequencesFrequentes +
                ", anomalies=" + anomalies +
                ", scorePredictibilite=" + scorePredictibilite +
                ", erreur='" + erreur + '\'' +
                '}';
    }
}