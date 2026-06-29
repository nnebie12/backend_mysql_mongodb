package com.example.demo.entiesMongodb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entité MongoDB - Demandes RGPD formelles
 * Stocke toutes les demandes RGPD (accès, suppression, portabilité)
 * Pour audit légal et conformité
 */
@Document(collection = "gdpr_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GDPRRequest {

  @Id
  private String id;

  @Indexed
  private Long userId;

  // Type de demande (Art. 15, 17, 20, etc.)
  private String requestType; // "access", "deletion", "portability", "rectification", "restriction"

  // Statut de la demande
  private String requestStatus; // "PENDING", "APPROVED", "REJECTED", "COMPLETED"

  // Données temporelles
  private LocalDateTime requestDate;   // Quand la demande a été reçue
  private LocalDateTime responseDate;  // Quand la réponse a été donnée (max 30j)
  private LocalDateTime completionDate; // Quand la demande a été complétée

  // Détails
  private String reason;       // Raison de la demande
  private String ipAddress;    // IP du demandeur (ANONYMISÉ)
  private String userAgent;    // User-Agent (optionnel)

  // Réponse
  private String responseMessage;
  private Map<String, Object> responseData;

  // Audit trail
  @Indexed(expireAfterSeconds = 94608000) // 3 ans (Art. 5 RGPD)
  private LocalDateTime createdAt;
}
