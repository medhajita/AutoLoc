package autoloc.web.controller;

import autoloc.domain.Client;
import autoloc.domain.CompteClient;
import autoloc.domain.Reservation;
import autoloc.domain.StatutReservation;
import autoloc.domain.StatutVehicule;
import autoloc.domain.Vehicule;
import autoloc.repository.ClientRepository;
import autoloc.repository.CompteClientRepository;
import autoloc.repository.ReservationRepository;
import autoloc.repository.VehiculeRepository;
import autoloc.web.dto.ClientDto;
import autoloc.web.dto.EspaceDtos.DemandeReservation;
import autoloc.web.dto.EspaceDtos.Inscription;
import autoloc.web.dto.EspaceDtos.MaReservation;
import autoloc.web.dto.EspaceDtos.ModificationReservation;
import autoloc.web.dto.EspaceDtos.VehiculeCatalogue;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Espace client : inscription, catalogue, et gestion des réservations du client connecté (rôle CLIENT).
 * Un client ne voit et ne modifie que ses propres réservations.
 */
@RestController
@RequestMapping("/api/espace")
@Transactional
public class EspaceClientController {

    private final ClientRepository clients;
    private final CompteClientRepository comptes;
    private final VehiculeRepository vehicules;
    private final ReservationRepository reservations;
    private final PasswordEncoder encoder;

    public EspaceClientController(ClientRepository clients, CompteClientRepository comptes,
                                  VehiculeRepository vehicules, ReservationRepository reservations,
                                  PasswordEncoder encoder) {
        this.clients = clients;
        this.comptes = comptes;
        this.vehicules = vehicules;
        this.reservations = reservations;
        this.encoder = encoder;
    }

    // ---------- public ----------

    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> inscription(@Valid @RequestBody Inscription in) {
        String email = in.email().trim().toLowerCase();
        if (comptes.existsByEmailIgnoreCase(email) || clients.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }
        if (clients.existsByNumPermis(in.numPermis().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro de permis est déjà enregistré");
        }
        Client c = new Client();
        c.setNom(in.nom().trim());
        c.setPrenom(in.prenom().trim());
        c.setEmail(email);
        c.setTelephone(in.telephone() == null || in.telephone().isBlank() ? null : in.telephone().trim());
        c.setNumPermis(in.numPermis().trim());
        c.setDateInscription(LocalDate.now());
        clients.save(c);

        CompteClient compte = new CompteClient();
        compte.setEmail(email);
        compte.setMotDePasse(encoder.encode(in.motDePasse()));
        compte.setClient(c);
        comptes.save(compte);
        return Map.of("email", email);
    }

    /** Catalogue : véhicules hors maintenance ; avec dates, la disponibilité tient compte des réservations. */
    @GetMapping("/vehicules")
    @Transactional(readOnly = true)
    public List<VehiculeCatalogue> catalogue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        if ((debut == null) != (fin == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez la date de début et la date de fin");
        }
        if (debut != null) {
            verifierDates(debut, fin);
        }
        Set<Long> occupes = debut == null ? Set.of() : new HashSet<>(reservations.vehiculeIdsOccupes(debut, fin));
        return vehicules.findAll().stream()
                .filter(v -> v.getStatut() != StatutVehicule.MAINTENANCE)
                .map(v -> new VehiculeCatalogue(v.getIdVehicule(), v.getMarque(), v.getModele(), v.getCategorie(),
                        v.getTarifJournalier(), v.getAgence() == null ? null : v.getAgence().getNom(),
                        !occupes.contains(v.getIdVehicule())))
                .toList();
    }

    // ---------- rôle CLIENT ----------

    @GetMapping("/moi")
    @Transactional(readOnly = true)
    public ClientDto moi(Authentication auth) {
        Client c = clientConnecte(auth);
        return new ClientDto(c.getIdClient(), c.getNom(), c.getPrenom(), c.getEmail(), c.getTelephone(),
                c.getNumPermis(), c.getDateInscription());
    }

    @GetMapping("/reservations")
    @Transactional(readOnly = true)
    public List<MaReservation> mesReservations(Authentication auth) {
        Client c = clientConnecte(auth);
        return reservations.findByClientIdClientOrderByDateDebutDesc(c.getIdClient()).stream()
                .map(EspaceClientController::toDto).toList();
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public MaReservation reserver(@Valid @RequestBody DemandeReservation demande, Authentication auth) {
        Client c = clientConnecte(auth);
        verifierDates(demande.dateDebut(), demande.dateFin());
        Vehicule v = vehicules.findById(demande.vehiculeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Véhicule introuvable"));
        if (v.getStatut() == StatutVehicule.MAINTENANCE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce véhicule est en maintenance");
        }
        if (reservations.existsOverlap(v.getIdVehicule(), demande.dateDebut(), demande.dateFin())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce véhicule est déjà réservé sur cette période");
        }
        Reservation r = new Reservation();
        r.setClient(c);
        r.setVehicule(v);
        r.setDateDebut(demande.dateDebut());
        r.setDateFin(demande.dateFin());
        r.setStatut(StatutReservation.EN_ATTENTE);
        return toDto(reservations.save(r));
    }

    @PutMapping("/reservations/{id}")
    public MaReservation modifier(@PathVariable Long id, @Valid @RequestBody ModificationReservation m,
                                  Authentication auth) {
        Reservation r = maReservation(id, auth);
        if (!estModifiable(r)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seule une réservation en attente et non commencée peut être modifiée");
        }
        verifierDates(m.dateDebut(), m.dateFin());
        if (reservations.existsOverlapExcluding(r.getVehicule().getIdVehicule(), m.dateDebut(), m.dateFin(),
                r.getIdReservation())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce véhicule est déjà réservé sur cette période");
        }
        r.setDateDebut(m.dateDebut());
        r.setDateFin(m.dateFin());
        return toDto(r);
    }

    @PatchMapping("/reservations/{id}/annuler")
    public MaReservation annuler(@PathVariable Long id, Authentication auth) {
        Reservation r = maReservation(id, auth);
        if (!estAnnulable(r)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette réservation ne peut plus être annulée en ligne (contrat signé, terminée ou déjà annulée)");
        }
        r.setStatut(StatutReservation.ANNULEE);
        return toDto(r);
    }

    // ---------- helpers ----------

    private Client clientConnecte(Authentication auth) {
        return comptes.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte client introuvable"))
                .getClient();
    }

    private Reservation maReservation(Long id, Authentication auth) {
        Client c = clientConnecte(auth);
        return reservations.findById(id)
                .filter(r -> r.getClient().getIdClient().equals(c.getIdClient()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable"));
    }

    private static void verifierDates(LocalDate debut, LocalDate fin) {
        if (debut.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de début est dans le passé");
        }
        if (fin.isBefore(debut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin précède la date de début");
        }
    }

    private static boolean estModifiable(Reservation r) {
        return r.getStatut() == StatutReservation.EN_ATTENTE && !r.getDateDebut().isBefore(LocalDate.now());
    }

    private static boolean estAnnulable(Reservation r) {
        return (r.getStatut() == StatutReservation.EN_ATTENTE || r.getStatut() == StatutReservation.CONFIRMEE)
                && r.getContrat() == null && !r.getDateDebut().isBefore(LocalDate.now());
    }

    static MaReservation toDto(Reservation r) {
        Vehicule v = r.getVehicule();
        long jours = ChronoUnit.DAYS.between(r.getDateDebut(), r.getDateFin()) + 1;
        return new MaReservation(r.getIdReservation(), v.getIdVehicule(),
                v.getMarque() + " " + v.getModele(), r.getDateDebut(), r.getDateFin(), jours,
                v.getTarifJournalier().multiply(BigDecimal.valueOf(jours)), r.getStatut(),
                r.getContrat() == null ? null : r.getContrat().getIdContrat(),
                estModifiable(r), estAnnulable(r));
    }
}
