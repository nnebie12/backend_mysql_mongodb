package com.example.demo.web.controllersMongoDB;


import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.servicesMongoDB.GDPRService;

import lombok.RequiredArgsConstructor;

/**
 * Contrôleur RGPD - Art. 5, 6, 7, 13, 15, 17, 18, 20, 21
 * Endpoints pour gérer les droits RGPD des utilisateurs
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class GDPRController {

  private final GDPRService gdprService;

  /**
   * Art. 15 RGPD - Droit d'accès
   * GET /api/v1/users/gdpr/export
   * Récupère TOUTES les données personnelles de l'utilisateur
   */
  @GetMapping("/gdpr/export")
  public ResponseEntity<?> exportUserData(
      Authentication authentication,
      @RequestParam(value = "format", defaultValue = "json") String format) {
    
    String userId = authentication.getName();
    
    if ("csv".equalsIgnoreCase(format)) {
      // Export en CSV
      String csvData = gdprService.exportToCSV(userId);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=gourmetgo-data.csv")
          .contentType(MediaType.TEXT_PLAIN)
          .body(csvData);
    } else {
      // Export en JSON (défaut)
      Map<String, Object> jsonData = gdprService.exportToJSON(userId);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=gourmetgo-data.json")
          .contentType(MediaType.APPLICATION_JSON)
          .body(jsonData);
    }
  }

  /**
   * Art. 17 RGPD - Droit à l'oubli (Suppression)
   * POST /api/v1/users/gdpr/delete
   * Supprime définitivement le compte et les données associées
   */
  @PostMapping("/gdpr/delete")
  public ResponseEntity<?> deleteUserAccount(
      Authentication authentication,
      @RequestBody Map<String, String> request) {
    
    String userId = authentication.getName();
    String password = request.get("password");

    if (password == null || password.isEmpty()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Le mot de passe est requis"));
    }

    try {
      gdprService.deleteUserAccount(userId, password);
      
      return ResponseEntity.ok(Map.of(
          "message", "Compte supprimé avec succès",
          "info", "Vos données seront effacées dans 30 jours conformément à l'Art. 17 RGPD"
      ));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(401)
          .body(Map.of("error", "Mot de passe incorrect"));
    } catch (Exception e) {
      return ResponseEntity.status(500)
          .body(Map.of("error", "Erreur lors de la suppression: " + e.getMessage()));
    }
  }

  /**
   * Art. 16 RGPD - Droit de rectification
   * PUT /api/v1/users/me
   * Modifie les données personnelles
   */
  @PutMapping("/me")
  public ResponseEntity<?> updateUserData(
      Authentication authentication,
      @RequestBody Map<String, Object> updates) {
    
    String userId = authentication.getName();
    UserEntity updatedUser = gdprService.updateUserData(userId, updates);
    
    return ResponseEntity.ok(Map.of(
        "message", "Données mises à jour avec succès",
        "user", updatedUser
    ));
  }

  /**
   * Art. 7 RGPD - Consentement explicite
   * POST /api/v1/users/consent
   * Enregistre les choix de consentement de l'utilisateur
   */
  @PostMapping("/consent")
  public ResponseEntity<?> saveConsent(
      Authentication authentication,
      @RequestBody Map<String, Object> consentData) {
    
    String userId = authentication.getName();
    gdprService.saveConsent(userId, consentData);
    
    return ResponseEntity.ok(Map.of(
        "message", "Consentements enregistrés",
        "timestamp", System.currentTimeMillis()
    ));
  }

  /**
   * Art. 7 RGPD - Retrait du consentement
   * POST /api/v1/users/gdpr/consent/revoke
   * Révoque le consentement pour une catégorie spécifique
   */
  @PostMapping("/gdpr/consent/revoke")
  public ResponseEntity<?> revokeConsent(
      Authentication authentication,
      @RequestBody Map<String, String> request) {
    
    String userId = authentication.getName();
    String category = request.get("category");
    
    gdprService.revokeConsent(userId, category);
    
    return ResponseEntity.ok(Map.of(
        "message", "Consentement révoqué pour: " + category
    ));
  }

  /**
   * Récupère l'historique des consentements
   * GET /api/v1/users/gdpr/consent-history
   */
  @GetMapping("/gdpr/consent-history")
  public ResponseEntity<?> getConsentHistory(Authentication authentication) {
    String userId = authentication.getName();
    var history = gdprService.getConsentHistory(userId);
    
    return ResponseEntity.ok(Map.of(
        "history", history
    ));
  }

  /**
   * Art. 18 RGPD - Restriction du traitement
   * POST /api/v1/users/gdpr/restrict
   * Restreint l'utilisation des données (ex: pas de recommandations)
   */
  @PostMapping("/gdpr/restrict")
  public ResponseEntity<?> restrictProcessing(
      Authentication authentication,
      @RequestBody Map<String, String> request) {
    
    String userId = authentication.getName();
    String restrictionType = request.get("restrictionType");
    
    gdprService.restrictProcessing(userId, restrictionType);
    
    return ResponseEntity.ok(Map.of(
        "message", "Restriction appliquée: " + restrictionType
    ));
  }

  /**
   * Récupère les données de tracking/cookies actifs
   * GET /api/v1/users/gdpr/trackers
   */
  @GetMapping("/gdpr/trackers")
  public ResponseEntity<?> getActiveTrackers(Authentication authentication) {
    String userId = authentication.getName();
    var trackers = gdprService.getActiveTrackers(userId);
    
    return ResponseEntity.ok(Map.of(
        "trackers", trackers
    ));
  }

  /**
   * Demande formelle RGPD (option modérée)
   * POST /api/v1/users/gdpr/formal-request
   * Crée une requête RGPD formelle pour audit légal
   */
  @PostMapping("/gdpr/formal-request")
  public ResponseEntity<?> submitFormalRequest(
      Authentication authentication,
      @RequestBody Map<String, String> request) {
    
    String userId = authentication.getName();
    String requestType = request.get("requestType"); 
    String reason = request.get("reason");
    
    gdprService.createFormalRequest(userId, requestType, reason);
    
    return ResponseEntity.ok(Map.of(
        "message", "Demande RGPD enregistrée",
        "ticketId", System.currentTimeMillis(),
        "info", "Vous recevrez une réponse dans 30 jours maximum"
    ));
  }
}
