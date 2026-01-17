package com.example.demo.entiesMongodb.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ProfilUtilisateur {
    NOUVEAU, DEBUTANT, OCCASIONNEL, ACTIF, FIDELE;
	
	@JsonCreator
    public static ProfilUtilisateur fromString(String value) {
        if (value == null) return null;
        // Normalise la chaîne : majuscules et suppression des accents
        String normalized = value.toUpperCase()
            .replace("É", "E")
            .replace("È", "E");
        return ProfilUtilisateur.valueOf(normalized);
    }
}
