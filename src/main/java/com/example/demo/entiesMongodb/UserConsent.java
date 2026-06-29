package com.example.demo.entiesMongodb;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Stocke les choix de consentement de l'utilisateur
 * Art. 7 RGPD - Consentement explicite
 */
@Document(collection = "user_consents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConsent {

  @Id
  private String id;

  @Indexed(unique = true)
  private Long userId; 

  // Catégories de consentement
  private boolean necessary = true;    // Toujours true (obligatoire)
  private boolean analytics = false;   // Analytics (Google Analytics, etc.)
  private boolean marketing = false;   // Marketing (Facebook, publicités)
  private boolean preferences = false; // Préférences (langue, thème)

  // Timestamps
  private LocalDateTime consentDate;      // Date du dernier consentement
  private LocalDateTime createdAt;        // Date de création
  private LocalDateTime updatedAt;        // Date de dernière modification

  // Historique des consentements (pour audit RGPD)
  private List<Map<String, Object>> consentHistory;

  // Restriction du traitement (Art. 18)
  private boolean restrictedProcessing = false;
  private String restrictionReason;

  // Préférences d'opposition (Art. 21)
  private boolean opposedToMarketing = false;
  private boolean opposedToRecommendations = false;
}