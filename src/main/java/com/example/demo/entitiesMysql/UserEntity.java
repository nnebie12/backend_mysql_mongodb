package com.example.demo.entitiesMysql;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.demo.entitiesMysql.ennums.Role;
import com.example.demo.entitiesMysql.FavorisEntity;
import com.example.demo.entiesMongodb.NoteDocument;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    public UserEntity(Long id, String nom, String prenom, String email, String motDePasse,
                      List<String> preferenceAlimentaire, List<String> ingredientsApprecies,
                      List<String> ingredientsEvites, List<String> contraintesAlimentaires,
                      String niveauCuisine, Boolean newsletter, Role role, List<RecetteEntity> recettes) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.preferenceAlimentaire = preferenceAlimentaire;
        this.ingredientsApprecies = ingredientsApprecies;
        this.ingredientsEvites = ingredientsEvites;
        this.contraintesAlimentaires = contraintesAlimentaires;
        this.niveauCuisine = niveauCuisine;
        this.newsletter = newsletter;
        this.actif = true;
        this.role = role;
        this.recettes = recettes;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    // Colonne canonique pour le hash du mot de passe.
    // Les colonnes DB "password" et "motdepasse" sont des artefacts legacy ;
    // exécuter la migration : ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NULL;
    //                         ALTER TABLE users MODIFY COLUMN motdepasse VARCHAR(255) NULL;
    // puis les supprimer une fois le déploiement stabilisé.
    @Column(name = "mot_de_passe", nullable = false, length = 255)
    private String motDePasse;

    @ElementCollection
    @Column(name = "preference_alimentaire", length = 500)
    private List<String> preferenceAlimentaire = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "user_ingredients_apprecies", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "ingredient")
    private List<String> ingredientsApprecies = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "user_ingredients_evites", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "ingredient")
    private List<String> ingredientsEvites = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "user_contraintes_alimentaires", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "contrainte")
    private List<String> contraintesAlimentaires = new ArrayList<>();
    
    @Column(name = "niveau_cuisine", length = 50)
    private String niveauCuisine;

    @Column(name = "newsletter", nullable = false)
    private Boolean newsletter = false;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @OneToMany(mappedBy = "userEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore 
    private List<RecetteEntity> recettes;

    @OneToMany(mappedBy = "userEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<FavorisEntity> favoris = new ArrayList<>(); 

    @Transient
    @JsonIgnore
    private List<NoteDocument> notes = new ArrayList<>();

    // ========== NOUVELLES COLONNES RGPD (À AJOUTER) ==========

  /**
   * Art. 17 RGPD - Soft delete
   * Marquer le compte comme inactif au lieu de supprimer complètement
   * Permet l'anonymisation
   */
  @Column(name = "is_actif", nullable = false)
  @Builder.Default
  private Boolean actif = true;

  @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

  /**
   * Art. 17 RGPD - Timestamp de suppression
   * Enregistrer quand le compte a été supprimé (pour audit trail)
   * Peut être utilisé pour supprimer définitivement après 30 jours
   */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /**
   * Art. 18 RGPD - Restriction du traitement
   * Marquer si l'utilisateur a restreint le traitement de ses données
   * (ex: pas de recommandations IA)
   */
  @Column(name = "processing_restricted", nullable = false)
  @Builder.Default
  private Boolean processingRestricted = false;

  // ========== MÉTHODES HELPER RGPD ==========

  /**
   * Anonymiser le profil (Art. 17 RGPD)
   * Appelée avant la suppression définitive
   */
  public void anonymize() {
    this.prenom = "SUPPRIMÉ";
    this.nom = "SUPPRIMÉ";
    this.email = "[SUPPRIMÉ-" + System.currentTimeMillis() + "]";
    this.motDePasse = "[SUPPRIMÉ]";
    this.preferenceAlimentaire = null;
    this.actif = false;
  }

  /**
   * Vérifier si l'utilisateur est actif
   */
  public boolean isActive() {
    return this.actif != null && this.actif;
  }

  /**
   * Vérifier si le compte a été supprimé
   */
  public boolean isDeleted() {
    return this.deletedAt != null;
  }

  /**
   * Vérifier si le traitement est restreint
   */
  public boolean hasRestrictedProcessing() {
    return this.processingRestricted != null && this.processingRestricted;
  }

  /**
   * Callback Hibernate - Set created_at automatiquement
   */
  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }
 
  /**
   * Callback Hibernate - Update updated_at automatiquement
   */
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

}