package autoloc.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContratDto(
        Long idContrat,
        @NotNull Long reservationId,
        String clientNom,
        String vehiculeLabel,
        LocalDate dateSignature,
        BigDecimal montantTotal,
        Boolean valide,
        BigDecimal totalPaye,
        List<PaiementDto> paiements) {
}
