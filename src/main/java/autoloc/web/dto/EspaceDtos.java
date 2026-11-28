package autoloc.web.dto;

import autoloc.domain.CategorieVehicule;
import autoloc.domain.StatutReservation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** DTO de l'espace client (inscription, catalogue, réservations du client connecté). */
public final class EspaceDtos {

    private EspaceDtos() {
    }

    public record Inscription(
            @NotBlank @Size(max = 50) String nom,
            @NotBlank @Size(max = 50) String prenom,
            @NotBlank @Email @Size(max = 100) String email,
            @Size(max = 20) String telephone,
            @NotBlank @Size(max = 30) String numPermis,
            @NotBlank @Size(min = 8, max = 72) String motDePasse) {
    }

    public record VehiculeCatalogue(
            Long idVehicule,
            String marque,
            String modele,
            CategorieVehicule categorie,
            BigDecimal tarifJournalier,
            String agenceNom,
            boolean disponible) {
    }

    /** Nombre de véhicules libres un jour donné (calendrier de sélection des dates). */
    public record DisponibiliteJour(LocalDate date, int libres, int total) {
    }

    public record DemandeReservation(
            @NotNull Long vehiculeId,
            @NotNull LocalDate dateDebut,
            @NotNull LocalDate dateFin) {
    }

    public record ModificationReservation(
            @NotNull LocalDate dateDebut,
            @NotNull LocalDate dateFin) {
    }

    public record MaReservation(
            Long idReservation,
            Long vehiculeId,
            String vehiculeLabel,
            LocalDate dateDebut,
            LocalDate dateFin,
            long nbJours,
            BigDecimal total,
            StatutReservation statut,
            Long contratId,
            boolean modifiable,
            boolean annulable) {
    }
}
