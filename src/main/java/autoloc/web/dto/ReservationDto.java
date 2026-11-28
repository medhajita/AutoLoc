package autoloc.web.dto;

import autoloc.domain.StatutReservation;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReservationDto(
        Long idReservation,
        @NotNull Long clientId,
        String clientNom,
        @NotNull Long vehiculeId,
        String vehiculeLabel,
        @NotNull LocalDate dateDebut,
        @NotNull LocalDate dateFin,
        StatutReservation statut,
        Long contratId) {
}
