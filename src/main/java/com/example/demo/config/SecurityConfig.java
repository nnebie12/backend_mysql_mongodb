package com.example.demo.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.security.CustomUserDetailsService;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtUtil;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(@Lazy JwtUtil jwtUtil,
                         @Lazy CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize

                // ── Authentification ─────────────────────────────────
                .requestMatchers("/api/v1/auth/**").permitAll()

                // ── Documentation API ────────────────────────────────
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/configuration/**"
                ).permitAll()

                // ── Recettes : lecture publique ──────────────────────
                .requestMatchers("/api/v1/recettes/all").permitAll()
                .requestMatchers("/api/v1/recettes/*/details").permitAll()
                .requestMatchers("/api/v1/recettes/*/ingredients").permitAll()
                .requestMatchers("/api/v1/recettes/*/commentaires").permitAll()
                .requestMatchers("/api/v1/recettes/*/notes").permitAll()
                .requestMatchers("/api/v1/recettes/*/moyenne-notes").permitAll()
                .requestMatchers("/api/v1/recettes/*").permitAll()

                // ── Ingrédients : lecture publique ───────────────────
                .requestMatchers("/api/v1/IngredientEntity/all").permitAll()
                .requestMatchers("/api/v1/IngredientEntity/nom/**").permitAll()
                .requestMatchers("/api/v1/IngredientEntity/*").permitAll()
                .requestMatchers("/api/recetteIngredient/recette/*").permitAll()

                // ── NLP : uniquement les recherches publiques ────────
                // ✅ CORRIGÉ : on n'ouvre plus TOUT /nlp/** au public.
                // Seule la recherche sémantique est publique (fonctionnalité de découverte).
                // Les insights utilisateur et le sentiment nécessitent une authentification.
                .requestMatchers("/api/v1/nlp/search/semantic").permitAll()
                .requestMatchers("/api/v1/nlp/stats").permitAll()

                // ── Recommandations : authentification requise ───────
                // ✅ CORRIGÉ : suppression du .permitAll() global sur /recommandations/**
                // Un utilisateur doit être connecté pour accéder à ses recommandations.
                .requestMatchers("/api/v1/recommandations/**").authenticated()
                .requestMatchers("/api/v1/recommendations/**").authenticated()
                .requestMatchers("/api/v1/ai-recommendations/**").authenticated()

                // NLP insights et sentiment : authentification requise
                .requestMatchers("/api/v1/nlp/**").authenticated()

                // Historique de recherche : authentification requise
                .requestMatchers("/api/v1/historique-recherche/**").authenticated()

                // ── Administration ───────────────────────────────────
                .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "ADMINISTRATEUR")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "ADMINISTRATEUR")
                .requestMatchers("/api/administrateur/**").hasAnyRole("ADMIN", "ADMINISTRATEUR")

                // ── Comportement utilisateur ─────────────────────────
                .requestMatchers("/api/v1/comportement-utilisateur/stats/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATEUR")
                .requestMatchers("/api/v1/comportement-utilisateur/engaged")
                    .hasAnyRole("ADMIN", "ADMINISTRATEUR")
                .requestMatchers("/api/v1/comportement-utilisateur/profil/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATEUR")
                .requestMatchers("/api/v1/comportement-utilisateur/**").authenticated()

                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "chrome-extension://*"
        ));

        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        configuration.setAllowedHeaders(Arrays.asList("*"));

        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Total-Count"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}