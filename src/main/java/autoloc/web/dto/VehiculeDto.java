package autoloc.web.dto;

import autoloc.domain.CategorieVehicule;
import autoloc.domain.StatutVehicule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record VehiculeDto(
        Long idVehicule,
        @NotBlank String immatriculation,
        @NotBlank String marque,
        @NotBlank String modele,
        @NotNull CategorieVehicule categorie,
        @NotNull @Positive BigDecimal tarifJournalier,
        @NotNull StatutVehicule statut,
        Long agenceId,
        String agenceNom) {
}
