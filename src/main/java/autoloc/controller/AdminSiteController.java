package autoloc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Back-office : les fichiers statiques (build Vite de frontend/admin) sont servis sous /admin/.
 * Seule la page d'entrée est publique ; toutes les données passent par /api (HTTP Basic, rôles USER / ADMIN).
 */
@Controller
public class AdminSiteController {

    @GetMapping({"/admin", "/admin/"})
    public String index() {
        return "forward:/admin/index.html";
    }
}
