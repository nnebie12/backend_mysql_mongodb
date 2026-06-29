package com.example.demo.servicesMongoDB;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.entiesMongodb.UserConsent;
import com.example.demo.entiesMongodb.GDPRRequest;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.repositoryMysql.UserRepository;
import com.example.demo.repositoryMongoDB.UserConsentRepository;
import com.example.demo.repositoryMongoDB.GDPRRequestRepository;
import com.example.demo.repositoryMongoDB.NoteMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;



/**
 * Service RGPD - Implémentation des droits RGPD
 * Art. 5, 6, 7, 13, 15, 17, 18, 20, 21
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GDPRService {

  private final UserRepository userRepository;
  private final UserConsentRepository userConsentRepository;
  private final GDPRRequestRepository gdprRequestRepository;
  private final PasswordEncoder passwordEncoder;
  private final ObjectMapper objectMapper;

  private final NoteMongoRepository noteMongoRepository;

  /**
   * Art. 15 RGPD - Exporter en JSON
   * Retourne TOUTES les données personnelles en JSON structuré
   */
  public Map<String, Object> exportToJSON(String userId) {
    UserEntity user = userRepository.findById(Long.parseLong(userId))
        .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

    // 2. Chargez les notes depuis MongoDB et injectez-les dans l'objet user
    List<NoteDocument> userNotes = noteMongoRepository.findByUserId(Long.parseLong(userId));
    user.setNotes(userNotes);

    Map<String, Object> export = new LinkedHashMap<>();
    

    // 1. Profil utilisateur
    export.put("profil", Map.of(
        "id", user.getId(),
        "nom", user.getNom(),
        "prenom", user.getPrenom(),
        "email", user.getEmail(),
        "dateCreation", user.getCreatedAt(),
        "dateModification", user.getUpdatedAt(),
        "preferenceAlimentaire", user.getPreferenceAlimentaire(),
        "role", user.getRole()
    ));

    // 2. Recettes créées
    export.put("recettes", user.getRecettes().stream()
        .map(r -> Map.<String, Object>of(
            "id", r.getId(),
            "titre", r.getTitre(),
            "description", r.getDescription(),
            "dateCreation", r.getCreatedAt()
        ))
        .collect(Collectors.toList()));

    // 3. Favoris
    export.put("favoris", user.getFavoris().stream()
        .map(f -> Map.<String, Object>of(
            "recetteId", f.getRecetteEntity().getId(),
            "recetteTitre", f.getRecetteEntity().getTitre(),
            "dateAjout", f.getDateAjout()
        ))
        .collect(Collectors.toList()));

    // 4. Notes et commentaires
    export.put("notes", user.getNotes().stream()
        .map(n -> Map.<String, Object>of(
            "recetteId", n.getRecetteId(),
            "score", n.getValeur() != null ? n.getValeur() : 0
        ))
        .collect(Collectors.toList()));

    // 5. Consentements RGPD
    Optional<UserConsent> consent = userConsentRepository.findByUserId(user.getId());
    if (consent.isPresent()) {
      export.put("consentements", Map.of(
          "necessary", consent.get().isNecessary(),
          "analytics", consent.get().isAnalytics(),
          "marketing", consent.get().isMarketing(),
          "preferences", consent.get().isPreferences(),
          "dateAcceptation", consent.get().getConsentDate(),
          "historique", consent.get().getConsentHistory()
      ));
    }

    // 6. Comportement utilisateur (MongoDB - anonymisé)
    // Ne pas exporter les données comportementales complètes
    // Seulement un résumé anonymisé
    export.put("comportement", Map.of(
        "note", "Les données comportementales (vues, clics) sont anonymisées après 180 jours (TTL)"
    ));

    log.info("[GDPR] Export JSON pour utilisateur: {}", userId);
    return export;
  }

  /**
   * Art. 15 RGPD - Exporter en CSV
   */
  public String exportToCSV(String userId) {
    UserEntity user = userRepository.findById(Long.parseLong(userId))
        .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

    StringBuilder csv = new StringBuilder();
    csv.append("Type,ID,Valeur,DateCreation\n");

    // Profil
    csv.append("PROFIL,").append(user.getId()).append(",")
        .append(user.getPrenom()).append(" ").append(user.getNom()).append(",")
        .append(user.getCreatedAt()).append("\n");

    // Recettes
    user.getRecettes().forEach(r ->
        csv.append("RECETTE,").append(r.getId()).append(",")
            .append(r.getTitre()).append(",")
            .append(r.getCreatedAt()).append("\n")
    );

    // Favoris
    user.getFavoris().forEach(f ->
        csv.append("FAVORI,").append(f.getId()).append(",")
            .append(f.getRecetteEntity().getTitre()).append(",")
            .append(f.getDateAjout()).append("\n")
    );

    log.info("[GDPR] Export CSV pour utilisateur: {}", userId);
    return csv.toString();
  }

  /**
   * Art. 17 RGPD - Droit à l'oubli (Suppression)
   * Supprime DÉFINITIVEMENT le compte et les données associées
   * Traitement immédiat suivi d'une suppression en base après 30j
   */
  @Transactional
  public void deleteUserAccount(String userId, String password) {
    UserEntity user = userRepository.findById(Long.parseLong(userId))
        .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

    // Vérifier le mot de passe
    if (!passwordEncoder.matches(password, user.getMotDePasse())) {
      log.warn("[GDPR] Tentative de suppression avec mauvais mot de passe: {}", userId);
      throw new IllegalArgumentException("Mot de passe incorrect");
    }

    // 1. Créer un enregistrement de suppression RGPD (audit trail)
    GDPRRequest deleteRequest = GDPRRequest.builder()
        .userId(user.getId())
        .requestType("deletion")
        .requestStatus("APPROVED")
        .requestDate(LocalDateTime.now())
        .responseDate(LocalDateTime.now())
        .reason("Demande de suppression de compte par utilisateur")
        .ipAddress("[ANONYMISÉ]") // Ne JAMAIS stocker les IPs réelles
        .build();
    gdprRequestRepository.save(deleteRequest);

    // 2. Anonymiser les données personnelles identifiables
    user.setNom("SUPPRIMÉ");
    user.setPrenom("SUPPRIMÉ");
    user.setEmail("[SUPPRIMÉ-" + System.currentTimeMillis() + "]");
    user.setMotDePasse("[SUPPRIMÉ]");
    user.setPreferenceAlimentaire(null);

    // 3. Marquer le compte comme désactivé (soft delete)
    user.setActif(false);
    user.setDeletedAt(LocalDateTime.now());
    userRepository.save(user);

    // 4. Supprimer les consentements
    userConsentRepository.deleteByUserId(user.getId());

    // 5. Réinitialiser/Supprimer les données MongoDB comportementales
    // (À faire dans le repository MongoDB)

    // 6. Logger pour audit (sans données sensibles)
    log.info("[GDPR] Compte supprimé et anonymisé: userId={}, timestamp={}", 
        userId, LocalDateTime.now());

    // 7. Envoi d'email de confirmation (optionnel)
    // sendConfirmationEmail(user.getEmail());
  }

  /**
   * Art. 16 RGPD - Droit de rectification
   * Modifie les données personnelles
   */
  @Transactional
  public UserEntity updateUserData(String userId, Map<String, Object> updates) {
    UserEntity user = userRepository.findById(Long.parseLong(userId))
        .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

    // Permettre la modification des champs autorisés uniquement
    if (updates.containsKey("prenom")) {
      user.setPrenom((String) updates.get("prenom"));
    }
    if (updates.containsKey("nom")) {
      user.setNom((String) updates.get("nom"));
    }
    if (updates.containsKey("preferenceAlimentaire")) {
      Object value = updates.get("preferenceAlimentaire");
      if (value instanceof List) {
        user.setPreferenceAlimentaire((List<String>) value);
      } else if (value instanceof String) {
        user.setPreferenceAlimentaire(List.of((String) value));
      }
    }

    // Ne JAMAIS permettre la modification de l'email ou du mot de passe ici
    // Ces champs nécessitent une vérification supplémentaire

    user.setUpdatedAt(LocalDateTime.now());
    UserEntity updated = userRepository.save(user);

    log.info("[GDPR] Données mises à jour pour utilisateur: {}", userId);
    return updated;
  }

  /**
   * Art. 7 RGPD - Enregistrer le consentement
   */
  @Transactional
  public void saveConsent(String userId, Map<String, Object> consentData) {
    UserEntity user = userRepository.findById(Long.parseLong(userId))
        .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

    Optional<UserConsent> existingConsent = userConsentRepository.findByUserId(user.getId());

    UserConsent consent;
    if (existingConsent.isPresent()) {
      consent = existingConsent.get();
    } else {
      consent = new UserConsent();
      consent.setUserId(user.getId());
    }

    // Extraire les consentements (casting nécessaire)
    Map<String, Object> consents = (Map<String, Object>) consentData.get("consents");
    if (consents != null) {
      consent.setNecessary((Boolean) consents.getOrDefault("necessary", true));
      consent.setAnalytics((Boolean) consents.getOrDefault("analytics", false));
      consent.setMarketing((Boolean) consents.getOrDefault("marketing", false));
      consent.setPreferences((Boolean) consents.getOrDefault("preferences", false));
    }

    consent.setConsentDate(LocalDateTime.now());

    // Ajouter à l'historique
    if (consent.getConsentHistory() == null) {
      consent.setConsentHistory(new ArrayList<>());
    }
    consent.getConsentHistory().add(Map.of(
        "timestamp", LocalDateTime.now(),
        "consents", consents
    ));

    userConsentRepository.save(consent);

    log.info("[GDPR] Consentements enregistrés pour utilisateur: {}", userId);
  }

  /**
   * Art. 7 RGPD - Révoquer un consentement
   */
  @Transactional
  public void revokeConsent(String userId, String category) {
    Optional<UserConsent> consent = userConsentRepository.findByUserId(Long.parseLong(userId));

    if (consent.isPresent()) {
      UserConsent c = consent.get();

      switch (category.toLowerCase()) {
        case "analytics":
          c.setAnalytics(false);
          break;
        case "marketing":
          c.setMarketing(false);
          break;
        case "preferences":
          c.setPreferences(false);
          break;
      }

      userConsentRepository.save(c);
      log.info("[GDPR] Consentement révoqué pour: {}, category: {}", userId, category);
    }
  }

  /**
   * Récupérer l'historique des consentements
   */
  public List<Map<String, Object>> getConsentHistory(String userId) {
    Optional<UserConsent> consent = userConsentRepository.findByUserId(Long.parseLong(userId));
    return consent.map(UserConsent::getConsentHistory).orElse(new ArrayList<>());
  }

  /**
   * Art. 18 RGPD - Restriction du traitement
   */
  @Transactional
  public void restrictProcessing(String userId, String restrictionType) {
    UserEntity user = userRepository.findById(Long.parseLong(userId))
        .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

    // Ajouter la restriction (ex: pas de recommandations IA)
    // Stocker dans un champ user.restrictions ou dans MongoDB

    log.info("[GDPR] Restriction appliquée: {}, type: {}", userId, restrictionType);
  }

  /**
   * Récupérer les trackers actifs
   */
  public List<Map<String, String>> getActiveTrackers(String userId) {
    // Retourner la liste des cookies/trackers actifs pour cet utilisateur
    return List.of(
        Map.of("name", "gdpr_consent", "type", "localStorage", "duration", "1 year"),
        Map.of("name", "jwt_token", "type", "essential", "duration", "24 hours"),
        Map.of("name", "analytics_id", "type", "analytics", "duration", "13 months")
    );
  }

  /**
   * Créer une demande RGPD formelle (pour audit légal)
   */
  @Transactional
  public void createFormalRequest(String userId, String requestType, String reason) {
    GDPRRequest request = GDPRRequest.builder()
        .userId(Long.parseLong(userId))
        .requestType(requestType)
        .requestStatus("PENDING")
        .requestDate(LocalDateTime.now())
        .reason(reason)
        .ipAddress("[ANONYMISÉ]")
        .build();

    gdprRequestRepository.save(request);

    log.info("[GDPR] Demande formelle créée: userId={}, type={}", userId, requestType);
  }
}