package autoloc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SecurityDemoController {

    /**
     * Endpoint public — aucune authentification requise.
     * GET /api/public/ping → 200 OK pour tous
     */
    @GetMapping("/public/ping")
    public String ping() {
        return "AutoLoc API is running";
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
