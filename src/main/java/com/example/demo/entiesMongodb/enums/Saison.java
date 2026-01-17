package com.example.demo.entiesMongodb.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Saison {
    PRINTEMPS, ETE, AUTOMNE, HIVER;
	
	@JsonCreator
    public static Saison fromString(String value) {
        if (value == null) return null;
        // Normalise la chaîne : majuscules et suppression des accents
        String normalized = value.toUpperCase()
            .replace("É", "E")
            .replace("È", "E");
        return Saison.valueOf(normalized);
    }
}