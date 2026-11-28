package autoloc.web.controller;

import autoloc.domain.StatutReservation;
import autoloc.domain.StatutVehicule;
import autoloc.repository.ClientRepository;
import autoloc.repository.ContratRepository;
import autoloc.repository.ReservationRepository;
import autoloc.repository.VehiculeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final VehiculeRepository vehicules;
    private final ClientRepository clients;
    private final ReservationRepository reservations;
    private final ContratRepository contrats;

    public DashboardController(VehiculeRepository vehicules, ClientRepository clients,
                               ReservationRepository reservations, ContratRepository contrats) {
        this.vehicules = vehicules;
        this.clients = clients;
        this.reservations = reservations;
        this.contrats = contrats;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return Map.of("username", auth.getName(), "roles", roles);
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        BigDecimal revenus = contrats.totalPaiements();
        long disponibles = vehicules.countByStatut(StatutVehicule.DISPONIBLE);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("vehicules", vehicules.count());
        out.put("vehiculesDisponibles", disponibles);
        out.put("clients", clients.count());
        out.put("reservationsEnAttente", reservations.countByStatut(StatutReservation.EN_ATTENTE));
        out.put("reservationsConfirmees", reservations.countByStatut(StatutReservation.CONFIRMEE));
        out.put("contrats", contrats.count());
        out.put("revenus", revenus);
        return out;
    }
}
