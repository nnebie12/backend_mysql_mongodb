package com.example.demo.web.mapper;

import com.example.demo.DTO.ComportementUtilisateurRequestDTO;
import com.example.demo.DTO.ComportementUtilisateurResponseDTO;
import com.example.demo.entiesMongodb.ComportementUtilisateur;
import com.example.demo.entiesMongodb.HistoriqueRecherche;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Component
public class ComportementUtilisateurMapper {

    // --- Mappage Entité -> DTO de Réponse ---
    public ComportementUtilisateurResponseDTO toResponseDto(ComportementUtilisateur entity) {
        if (entity == null) {
            return null;
        }
        ComportementUtilisateurResponseDTO dto = new ComportementUtilisateurResponseDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateMiseAJour(entity.getDateMiseAJour());
        dto.setHistoriqueInteractionsIds(entity.getHistoriqueInteractionsIds());
        dto.setHistoriqueRecherchesIds(entity.getHistoriqueRecherchesIds());

        Optional.ofNullable(entity.getPreferencesSaisonnieres())
                .map(this::toPreferencesSaisonnieresDto)
                .ifPresent(dto::setPreferencesSaisonnieres);
        Optional.ofNullable(entity.getHabitudesNavigation())
                .map(this::toHabitudesNavigationDto)
                .ifPresent(dto::setHabitudesNavigation);
        Optional.ofNullable(entity.getCyclesActivite())
                .map(this::toCyclesActiviteDto)
                .ifPresent(dto::setCyclesActivite);
        Optional.ofNullable(entity.getMetriques())
                .map(this::toMetriquesComportementalesDto)
                .ifPresent(dto::setMetriques);

        return dto;
    }

    public List<ComportementUtilisateurResponseDTO> toResponseDtoList(List<ComportementUtilisateur> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    private ComportementUtilisateurResponseDTO.PreferencesSaisonnieresDTO toPreferencesSaisonnieresDto(ComportementUtilisateur.PreferencesSaisonnieres entity) {
        if (entity == null) return null;
        ComportementUtilisateurResponseDTO.PreferencesSaisonnieresDTO dto = new ComportementUtilisateurResponseDTO.PreferencesSaisonnieresDTO();
        dto.setIngredientsPrintemps(entity.getIngredientsPrintemps());
        dto.setIngredientsEte(entity.getIngredientsEte());
        dto.setIngredientsAutomne(entity.getIngredientsAutomne());
        dto.setIngredientsHiver(entity.getIngredientsHiver());
        dto.setSaisonPreferee(entity.getSaisonPreferee()); // C'est déjà un enum
        dto.setScoresPreferenceSaisonniere(entity.getScoresPreferenceSaisonniere());
        dto.setDerniereMiseAJour(entity.getDerniereMiseAJour());
        return dto;
    }

    private ComportementUtilisateurResponseDTO.HabitudesNavigationDTO toHabitudesNavigationDto(ComportementUtilisateur.HabitudesNavigation entity) {
        if (entity == null) return null;
        ComportementUtilisateurResponseDTO.HabitudesNavigationDTO dto = new ComportementUtilisateurResponseDTO.HabitudesNavigationDTO();
        dto.setPagesVisitees(entity.getPagesVisitees());
        dto.setTempsParPage(entity.getTempsParPage());
        dto.setRecherchesFavorites(entity.getRecherchesFavorites());
        dto.setTypeRecettePreferee(entity.getTypeRecettePreferee());
        dto.setNombreConnexionsParJour(entity.getNombreConnexionsParJour());
        dto.setHeuresConnexionHabituelles(entity.getHeuresConnexionHabituelles());
        dto.setParcoursFavoris(entity.getParcoursFavoris());
        dto.setTempsMoyenParSession(entity.getTempsMoyenParSession());
        dto.setNombrePagesParSession(entity.getNombrePagesParSession());
        dto.setCategoriesPreferees(entity.getCategoriesPreferees());
        dto.setFrequenceParCategorie(entity.getFrequenceParCategorie());
        return dto;
    }

    private ComportementUtilisateurResponseDTO.CyclesActiviteDTO toCyclesActiviteDto(ComportementUtilisateur.CyclesActivite entity) {
        if (entity == null) return null;
        ComportementUtilisateurResponseDTO.CyclesActiviteDTO dto = new ComportementUtilisateurResponseDTO.CyclesActiviteDTO();
        dto.setPetitDejeuner(toCreneauRepasDto(entity.getPetitDejeuner()));
        dto.setDejeuner(toCreneauRepasDto(entity.getDejeuner()));
        dto.setDiner(toCreneauRepasDto(entity.getDiner()));
        dto.setActiviteParJour(entity.getActiviteParJour());
        dto.setJoursActifs(entity.getJoursActifs());
        dto.setActivitesParCreneau(entity.getActivitesParCreneau());
        dto.setCreneauLePlusActif(entity.getCreneauLePlusActif());
        dto.setConsistanceHoraire(entity.getConsistanceHoraire());
        return dto;
    }

    private ComportementUtilisateurResponseDTO.CreneauRepasDTO toCreneauRepasDto(ComportementUtilisateur.CreneauRepas entity) {
        if (entity == null) return null;
        ComportementUtilisateurResponseDTO.CreneauRepasDTO dto = new ComportementUtilisateurResponseDTO.CreneauRepasDTO();
        dto.setHeureDebut(entity.getHeureDebut());
        dto.setHeureFin(entity.getHeureFin());
        dto.setTypeRecettesPreferees(entity.getTypeRecettesPreferees());
        dto.setFrequenceConsultation(entity.getFrequenceConsultation());
        dto.setActif(entity.getActif());
        dto.setDureMoyenneConsultation(entity.getDureMoyenneConsultation());
        dto.setIngredientsFavoris(entity.getIngredientsFavoris());
        dto.setComplexitePreferee(entity.getComplexitePreferee()); // C'est déjà un Map<Enum, Integer>
        return dto;
    }

    private ComportementUtilisateurResponseDTO.MetriquesComportementalesDTO toMetriquesComportementalesDto(ComportementUtilisateur.MetriquesComportementales entity) {
        if (entity == null) return null;
        ComportementUtilisateurResponseDTO.MetriquesComportementalesDTO dto = new ComportementUtilisateurResponseDTO.MetriquesComportementalesDTO();
        dto.setNombreFavorisTotal(entity.getNombreFavorisTotal());
        dto.setNoteMoyenneDonnee(entity.getNoteMoyenneDonnee());
        dto.setNombreCommentairesLaisses(entity.getNombreCommentairesLaisses());
        dto.setNombreRecherchesTotales(entity.getNombreRecherchesTotales());
        dto.setTermesRechercheFrequents(entity.getTermesRechercheFrequents());
        dto.setTauxRecherchesFructueuses(entity.getTauxRecherchesFructueuses());
        dto.setScoreEngagement(entity.getScoreEngagement());
        dto.setProfilUtilisateur(entity.getProfilUtilisateur()); // C'est déjà un enum
        dto.setFrequenceActions(entity.getFrequenceActions());
        dto.setScoreRecommandation(entity.getScoreRecommandation());
        dto.setTendancesTemporelles(entity.getTendancesTemporelles());
        dto.setStreakConnexion(entity.getStreakConnexion());
        dto.setDerniereActivite(entity.getDerniereActivite());
        return dto;
    }

    // --- Mappage DTO de Requête -> Entité ---
    public void updateEntityFromRequestDto(ComportementUtilisateurRequestDTO dto, ComportementUtilisateur entity) {
        if (dto == null || entity == null) {
            return;
        }

        // Mise à jour des sous-objets si le DTO les contient
        if (dto.getPreferencesSaisonnieres() != null) {
            if (entity.getPreferencesSaisonnieres() == null) {
                entity.setPreferencesSaisonnieres(new ComportementUtilisateur.PreferencesSaisonnieres());
            }
            updatePreferencesSaisonnieresEntity(dto.getPreferencesSaisonnieres(), entity.getPreferencesSaisonnieres());
        }
        if (dto.getHabitudesNavigation() != null) {
            if (entity.getHabitudesNavigation() == null) {
                entity.setHabitudesNavigation(new ComportementUtilisateur.HabitudesNavigation());
            }
            updateHabitudesNavigationEntity(dto.getHabitudesNavigation(), entity.getHabitudesNavigation());
        }
        if (dto.getCyclesActivite() != null) {
            if (entity.getCyclesActivite() == null) {
                entity.setCyclesActivite(new ComportementUtilisateur.CyclesActivite());
            }
            updateCyclesActiviteEntity(dto.getCyclesActivite(), entity.getCyclesActivite());
        }
        if (dto.getMetriques() != null) {
            if (entity.getMetriques() == null) {
                entity.setMetriques(new ComportementUtilisateur.MetriquesComportementales());
            }
            updateMetriquesComportementalesEntity(dto.getMetriques(), entity.getMetriques());
        }

        // Mettre à jour la date de mise à jour 
        entity.setDateMiseAJour(LocalDateTime.now());
    }


    private void updatePreferencesSaisonnieresEntity(ComportementUtilisateurRequestDTO.PreferencesSaisonnieresRequestDTO dto, ComportementUtilisateur.PreferencesSaisonnieres entity) {
        Optional.ofNullable(dto.getIngredientsPrintemps()).ifPresent(entity::setIngredientsPrintemps);
        Optional.ofNullable(dto.getIngredientsEte()).ifPresent(entity::setIngredientsEte);
        Optional.ofNullable(dto.getIngredientsAutomne()).ifPresent(entity::setIngredientsAutomne);
        Optional.ofNullable(dto.getIngredientsHiver()).ifPresent(entity::setIngredientsHiver);
        Optional.ofNullable(dto.getSaisonPreferee()).ifPresent(entity::setSaisonPreferee);
        Optional.ofNullable(dto.getScoresPreferenceSaisonniere()).ifPresent(entity::setScoresPreferenceSaisonniere);
    }

    private void updateHabitudesNavigationEntity(ComportementUtilisateurRequestDTO.HabitudesNavigationRequestDTO dto, ComportementUtilisateur.HabitudesNavigation entity) {
        Optional.ofNullable(dto.getPagesVisitees()).ifPresent(entity::setPagesVisitees);
        Optional.ofNullable(dto.getTempsParPage()).ifPresent(entity::setTempsParPage);
        Optional.ofNullable(dto.getRecherchesFavorites()).ifPresent(entity::setRecherchesFavorites);
        Optional.ofNullable(dto.getTypeRecettePreferee()).ifPresent(entity::setTypeRecettePreferee);
        Optional.ofNullable(dto.getNombreConnexionsParJour()).ifPresent(entity::setNombreConnexionsParJour);
        Optional.ofNullable(dto.getHeuresConnexionHabituelles()).ifPresent(entity::setHeuresConnexionHabituelles);
        Optional.ofNullable(dto.getParcoursFavoris()).ifPresent(entity::setParcoursFavoris);
        Optional.ofNullable(dto.getTempsMoyenParSession()).ifPresent(entity::setTempsMoyenParSession);
        Optional.ofNullable(dto.getNombrePagesParSession()).ifPresent(entity::setNombrePagesParSession);
        Optional.ofNullable(dto.getCategoriesPreferees()).ifPresent(entity::setCategoriesPreferees);
        Optional.ofNullable(dto.getFrequenceParCategorie()).ifPresent(entity::setFrequenceParCategorie);
    }

    private void updateCyclesActiviteEntity(ComportementUtilisateurRequestDTO.CyclesActiviteRequestDTO dto, ComportementUtilisateur.CyclesActivite entity) {
        
        if (dto.getPetitDejeuner() != null) {
            if (entity.getPetitDejeuner() == null) entity.setPetitDejeuner(new ComportementUtilisateur.CreneauRepas());
            updateCreneauRepasEntity(dto.getPetitDejeuner(), entity.getPetitDejeuner());
        }
        if (dto.getDejeuner() != null) {
            if (entity.getDejeuner() == null) entity.setDejeuner(new ComportementUtilisateur.CreneauRepas());
            updateCreneauRepasEntity(dto.getDejeuner(), entity.getDejeuner());
        }
        if (dto.getDiner() != null) {
            if (entity.getDiner() == null) entity.setDiner(new ComportementUtilisateur.CreneauRepas());
            updateCreneauRepasEntity(dto.getDiner(), entity.getDiner());
        }
        Optional.ofNullable(dto.getActiviteParJour()).ifPresent(entity::setActiviteParJour);
        Optional.ofNullable(dto.getJoursActifs()).ifPresent(entity::setJoursActifs);
        Optional.ofNullable(dto.getActivitesParCreneau()).ifPresent(entity::setActivitesParCreneau);
        Optional.ofNullable(dto.getCreneauLePlusActif()).ifPresent(entity::setCreneauLePlusActif);
        Optional.ofNullable(dto.getConsistanceHoraire()).ifPresent(entity::setConsistanceHoraire);
    }

    private void updateCreneauRepasEntity(ComportementUtilisateurRequestDTO.CreneauRepasRequestDTO dto, ComportementUtilisateur.CreneauRepas entity) {
        Optional.ofNullable(dto.getHeureDebut()).ifPresent(entity::setHeureDebut);
        Optional.ofNullable(dto.getHeureFin()).ifPresent(entity::setHeureFin);
        Optional.ofNullable(dto.getTypeRecettesPreferees()).ifPresent(entity::setTypeRecettesPreferees);
        Optional.ofNullable(dto.getFrequenceConsultation()).ifPresent(entity::setFrequenceConsultation);
        Optional.ofNullable(dto.getActif()).ifPresent(entity::setActif);
        Optional.ofNullable(dto.getDureMoyenneConsultation()).ifPresent(entity::setDureMoyenneConsultation);
        Optional.ofNullable(dto.getIngredientsFavoris()).ifPresent(entity::setIngredientsFavoris);
        Optional.ofNullable(dto.getComplexitePreferee()).ifPresent(entity::setComplexitePreferee);
    }

    private void updateMetriquesComportementalesEntity(ComportementUtilisateurRequestDTO.MetriquesComportementalesRequestDTO dto, ComportementUtilisateur.MetriquesComportementales entity) {
        Optional.ofNullable(dto.getNombreFavorisTotal()).ifPresent(entity::setNombreFavorisTotal);
        Optional.ofNullable(dto.getNoteMoyenneDonnee()).ifPresent(entity::setNoteMoyenneDonnee);
        Optional.ofNullable(dto.getNombreCommentairesLaisses()).ifPresent(entity::setNombreCommentairesLaisses);
        Optional.ofNullable(dto.getNombreRecherchesTotales()).ifPresent(entity::setNombreRecherchesTotales);
        Optional.ofNullable(dto.getTermesRechercheFrequents()).ifPresent(entity::setTermesRechercheFrequents);
        Optional.ofNullable(dto.getTauxRecherchesFructueuses()).ifPresent(entity::setTauxRecherchesFructueuses);
        Optional.ofNullable(dto.getScoreEngagement()).ifPresent(entity::setScoreEngagement);
        Optional.ofNullable(dto.getProfilUtilisateur()).ifPresent(entity::setProfilUtilisateur);
        Optional.ofNullable(dto.getFrequenceActions()).ifPresent(entity::setFrequenceActions);
        Optional.ofNullable(dto.getScoreRecommandation()).ifPresent(entity::setScoreRecommandation);
        Optional.ofNullable(dto.getTendancesTemporelles()).ifPresent(entity::setTendancesTemporelles);
        Optional.ofNullable(dto.getStreakConnexion()).ifPresent(entity::setStreakConnexion);
        Optional.ofNullable(dto.getDerniereActivite()).ifPresent(entity::setDerniereActivite);
    }
}