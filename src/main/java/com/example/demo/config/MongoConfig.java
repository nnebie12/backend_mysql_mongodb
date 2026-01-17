package com.example.demo.config;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import com.example.demo.entiesMongodb.enums.ProfilUtilisateurReadingConverter;
import com.example.demo.entiesMongodb.enums.SaisonReadingConverter;

@Configuration
public class MongoConfig {
    @Bean
    public MongoCustomConversions customConversions() {
        // Mettez tous vos convertisseurs dans la même liste
        return new MongoCustomConversions(Arrays.asList(
            new ProfilUtilisateurReadingConverter(),
            new SaisonReadingConverter()
        ));
    }
}