package autoloc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Site client : les fichiers statiques (build Vite de frontend/client) sont servis sous /client/.
 * Cette route renvoie la page d'entrée pour /client et /client/.
 */
@Controller
public class ClientSiteController {

    @GetMapping({"/client", "/client/"})
    public String index() {
        return "forward:/client/index.html";
    }
}
