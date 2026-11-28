package autoloc.web.dto;

import autoloc.domain.ModePaiement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaiementDto(
        Long idPaiement,
        @NotNull @Positive BigDecimal montant,
        LocalDate datePaiement,
        @NotNull ModePaiement modePaiement) {
}
