package autoloc.web.controller;

import autoloc.domain.Client;
import autoloc.domain.Reservation;
import autoloc.domain.StatutReservation;
import autoloc.domain.Vehicule;
import autoloc.repository.ClientRepository;
import autoloc.repository.ReservationRepository;
import autoloc.repository.VehiculeRepository;
import autoloc.web.dto.ReservationDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Transactional
public class ReservationController {

    private final ReservationRepository reservations;
    private final ClientRepository clients;
    private final VehiculeRepository vehicules;

    public ReservationController(ReservationRepository reservations, ClientRepository clients,
                                 VehiculeRepository vehicules) {
        this.reservations = reservations;
        this.clients = clients;
        this.vehicules = vehicules;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ReservationDto> list() {
        return reservations.findAll().stream().map(ReservationController::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationDto create(@Valid @RequestBody ReservationDto dto) {
        if (dto.dateFin().isBefore(dto.dateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin précède la date de début");
        }
        Client client = clients.findById(dto.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client introuvable"));
        Vehicule vehicule = vehicules.findById(dto.vehiculeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Véhicule introuvable"));
        if (reservations.existsOverlap(vehicule.getIdVehicule(), dto.dateDebut(), dto.dateFin())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce véhicule est déjà réservé sur cette période");
        }
        Reservation r = new Reservation();
        r.setClient(client);
        r.setVehicule(vehicule);
        r.setDateDebut(dto.dateDebut());
        r.setDateFin(dto.dateFin());
        r.setStatut(StatutReservation.EN_ATTENTE);
        return toDto(reservations.save(r));
    }

    @PatchMapping("/{id}/statut")
    public ReservationDto changeStatut(@PathVariable Long id, @RequestParam StatutReservation statut) {
        Reservation r = reservations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable"));
        r.setStatut(statut);
        return toDto(r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!reservations.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable");
        }
        reservations.deleteById(id);
        reservations.flush();
    }

    static ReservationDto toDto(Reservation r) {
        Client c = r.getClient();
        Vehicule v = r.getVehicule();
        return new ReservationDto(r.getIdReservation(),
                c.getIdClient(), c.getPrenom() + " " + c.getNom(),
                v.getIdVehicule(), v.getMarque() + " " + v.getModele() + " (" + v.getImmatriculation() + ")",
                r.getDateDebut(), r.getDateFin(), r.getStatut(),
                r.getContrat() == null ? null : r.getContrat().getIdContrat());
    }
}
