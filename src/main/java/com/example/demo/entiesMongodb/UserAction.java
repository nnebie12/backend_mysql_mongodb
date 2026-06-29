package com.example.demo.entiesMongodb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * Entité MongoDB - Historique des actions utilisateur (anonymisé)
 * Suppression automatique après 180 jours (TTL)
 * Art. 5 RGPD - Limitation de la conservation
 */
@Document(collection = "user_actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAction {

  @Id
  private String id;

  @Indexed
  private Long userId;

  // Action
  private String actionType; // "VIEW_RECIPE", "SEARCH", "RATE", "COMMENT"
  private String actionDetails;

  // Timestamp
  @Indexed(expireAfterSeconds = 15552000) // 180 jours = 15,552,000 secondes
  private LocalDateTime actionDate;

  // Données anonymisées seulement
  // PAS d'IP, PAS d'identifiants de tracking
}

