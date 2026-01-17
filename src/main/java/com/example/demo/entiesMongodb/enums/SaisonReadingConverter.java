package com.example.demo.entiesMongodb.enums;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class SaisonReadingConverter implements Converter<String, Saison> {
    @Override
    public Saison convert(String source) {
        if (source == null) return null;
        String normalized = source.toUpperCase().replace("É", "E").trim();
        return Saison.valueOf(normalized);
    }
}
