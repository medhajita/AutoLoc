package autoloc.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AgenceDto(
        Long idAgence,
        @NotBlank String nom,
        @NotBlank String ville,
        @NotBlank String adresse,
        String telephone) {
}
