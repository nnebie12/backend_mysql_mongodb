package com.example.demo.web.controllersMongoDB;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.servicesMongoDB.ComportementUtilisateurService;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/comportement-utilisateur")
public class ComportementTrackingController {
    
    @Autowired
    private ComportementUtilisateurService comportementService;
    
    /**
     * Recevoir données de tracking depuis extension
     */
    @PostMapping("/track")
    public ResponseEntity<?> trackBehavior(@RequestBody Map<String, Object> data) {
        try {
            Long userId = Long.parseLong(data.get("userId").toString());
            String type = data.get("type").toString();
            
            // Enregistrer dans MongoDB
            comportementService.enregistrerInteraction(
                userId,
                type,
                data.get("site") != null ? data.get("site").toString() : null,
                new ObjectMapper().writeValueAsString(data)
            );
            
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Recevoir résumé de session
     */
    @PostMapping("/session-summary")
    public ResponseEntity<?> sessionSummary(@RequestBody Map<String, Object> summary) {
        // Enregistrer dans MongoDB
        // Mettre à jour métriques utilisateur
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
