package com.example.demo.entiesMongodb.enums;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class ProfilUtilisateurReadingConverter implements Converter<String, ProfilUtilisateur> {
    @Override
    public ProfilUtilisateur convert(String source) {
        if (source == null) return null;
        
        // On applique la même logique de normalisation que votre JsonCreator
        String normalized = source.toUpperCase()
            .replace("É", "E")
            .replace("È", "E")
            .trim();
            
        try {
            return ProfilUtilisateur.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Optionnel : retourner une valeur par défaut au lieu de crasher
            return ProfilUtilisateur.NOUVEAU; 
        }
    }
}