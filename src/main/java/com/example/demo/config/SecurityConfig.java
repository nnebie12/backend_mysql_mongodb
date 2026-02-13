package com.example.demo.config;

import com.example.demo.security.CustomUserDetailsService;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtUtil;
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

import java.util.Arrays;
import java.util.List;

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
                // Auth endpoints - publics
                .requestMatchers("/api/v1/auth/**").permitAll()
                
                // Swagger/OpenAPI - publics
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/configuration/**"
                ).permitAll()
                
                // Recettes - publics (lecture seule)
                .requestMatchers("/api/v1/recettes/all").permitAll()
                .requestMatchers("/api/v1/recettes/{id}").permitAll()
                .requestMatchers("/api/v1/recettes/{recetteId}/details").permitAll()
                .requestMatchers("/api/v1/recettes/{recetteId}/ingredients").permitAll()
                .requestMatchers("/api/v1/recettes/{recetteId}/commentaires").permitAll()
                .requestMatchers("/api/v1/recettes/{recetteId}/notes").permitAll()
                .requestMatchers("/api/v1/recettes/{recetteId}/moyenne-notes").permitAll()
                
                // Ingrédients - publics (lecture)
                .requestMatchers("/api/v1/IngredientEntity/all").permitAll()
                .requestMatchers("/api/v1/IngredientEntity/{id}").permitAll()
                .requestMatchers("/api/v1/IngredientEntity/nom/{nom}").permitAll()
                
                // RecetteIngredient - publics (lecture)
                .requestMatchers("/api/recetteIngredient/recette/{recetteId}").permitAll()
                
                // Recommandation - publics (lecture)
                .requestMatchers("/api/v1/recommandations/**").permitAll()
                .requestMatchers("/api/v1/historique-recherche/**").permitAll()
                .requestMatchers("/api/v1/recommendations/**").permitAll()
                .requestMatchers("/api/v1/ai-recommendations/**").permitAll()
                .requestMatchers("/api/v1/nlp/**").permitAll()
                
                .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "ADMINISTRATEUR")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "ADMINISTRATEUR")
                .requestMatchers("/api/administrateur/**").hasAnyRole("ADMIN", "ADMINISTRATEUR")
                
                .requestMatchers("/api/v1/comportement-utilisateur/stats/**").hasAnyRole("ADMIN", "ADMINISTRATEUR")
                .requestMatchers("/api/v1/comportement-utilisateur/engaged").hasAnyRole("ADMIN", "ADMINISTRATEUR")
                .requestMatchers("/api/v1/comportement-utilisateur/profil/**").hasAnyRole("ADMIN", "ADMINISTRATEUR")
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
        
        // 1. On autorise explicitement les origines de développement
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:[*]",
            "http://127.0.0.1:[*]",
            "http://*.localhost:[*]"
        ));
        
        // 2. On autorise TOUTES les méthodes
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // 3. On autorise TOUS les headers (indispensable pour les tests)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 4. On expose les headers dont le frontend a besoin (notamment le JWT)
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        // 5. On autorise les cookies/auth-headers
        configuration.setAllowCredentials(true);
        
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}