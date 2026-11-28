package autoloc.web.controller;

import autoloc.domain.Contrat;
import autoloc.domain.Paiement;
import autoloc.domain.Reservation;
import autoloc.repository.ContratRepository;
import autoloc.repository.ReservationRepository;
import autoloc.web.dto.ContratDto;
import autoloc.web.dto.PaiementDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/contrats")
@Transactional
public class ContratController {

    private final ContratRepository contrats;
    private final ReservationRepository reservations;

    public ContratController(ContratRepository contrats, ReservationRepository reservations) {
        this.contrats = contrats;
        this.reservations = reservations;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ContratDto> list() {
        return contrats.findAll().stream().map(ContratController::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContratDto create(@Valid @RequestBody ContratDto dto) {
        Reservation r = reservations.findById(dto.reservationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Réservation introuvable"));
        if (r.getContrat() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette réservation a déjà un contrat");
        }
        long jours = ChronoUnit.DAYS.between(r.getDateDebut(), r.getDateFin()) + 1;
        Contrat c = new Contrat();
        c.setDateSignature(LocalDate.now());
        c.setMontantTotal(r.getVehicule().getTarifJournalier().multiply(BigDecimal.valueOf(jours)));
        c.setValide(true);
        c.setReservation(r);
        r.setContrat(c);
        return toDto(contrats.save(c));
    }

    @PostMapping("/{id}/paiements")
    @ResponseStatus(HttpStatus.CREATED)
    public ContratDto addPaiement(@PathVariable Long id, @Valid @RequestBody PaiementDto dto) {
        Contrat c = find(id);
        Paiement p = new Paiement();
        p.setMontant(dto.montant());
        p.setModePaiement(dto.modePaiement());
        p.setDatePaiement(dto.datePaiement() != null ? dto.datePaiement() : LocalDate.now());
        p.setContrat(c);
        c.getPaiements().add(p);
        contrats.flush();
        return toDto(c);
    }

    @DeleteMapping("/{id}/paiements/{paiementId}")
    public ContratDto removePaiement(@PathVariable Long id, @PathVariable Long paiementId) {
        Contrat c = find(id);
        Paiement p = c.getPaiements().stream()
                .filter(x -> x.getIdPaiement().equals(paiementId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable"));
        c.getPaiements().remove(p);
        p.setContrat(null);
        contrats.flush();
        return toDto(c);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Contrat c = find(id);
        if (c.getReservation() != null) {
            c.getReservation().setContrat(null);
        }
        contrats.delete(c);
        contrats.flush();
    }

    private Contrat find(Long id) {
        return contrats.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrat introuvable"));
    }

    static ContratDto toDto(Contrat c) {
        List<PaiementDto> paiements = c.getPaiements().stream()
                .map(p -> new PaiementDto(p.getIdPaiement(), p.getMontant(), p.getDatePaiement(), p.getModePaiement()))
                .toList();
        BigDecimal total = paiements.stream().map(PaiementDto::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Reservation r = c.getReservation();
        return new ContratDto(c.getIdContrat(),
                r == null ? null : r.getIdReservation(),
                r == null ? null : r.getClient().getPrenom() + " " + r.getClient().getNom(),
                r == null ? null : r.getVehicule().getMarque() + " " + r.getVehicule().getModele(),
                c.getDateSignature(), c.getMontantTotal(), c.isValide(), total, paiements);
    }
}
