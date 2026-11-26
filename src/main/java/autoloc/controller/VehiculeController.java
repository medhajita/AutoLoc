package autoloc.controller;

import autoloc.repository.VehiculeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class VehiculeController {

    private final VehiculeRepository vehiculeRepository;

    public VehiculeController(VehiculeRepository vehiculeRepository) {
        this.vehiculeRepository = vehiculeRepository;
    }

    /**
     * Endpoint public — aucune authentification requise.
     * GET /api/public/ping → 200 OK pour tous
     */
    @GetMapping("/public/ping")
    public String ping() {
        return "AutoLoc API is running";
    }

    /**
     * Endpoint USER + ADMIN — authentification requise (rôle USER ou ADMIN).
     * GET /api/vehicules → 200 OK, 401 si anonyme
     */
    @GetMapping("/vehicules")
    public String listVehicules() {
        long count = vehiculeRepository.count();
        return "Vehicules: " + count + " enregistrement(s)";
    }

    /**
     * Endpoint ADMIN uniquement — rôle ADMIN requis.
     * GET /api/admin/dashboard → 200 OK pour ADMIN, 403 pour USER, 401 si anonyme
     */
    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "Admin dashboard — restricted access";
    }
}
