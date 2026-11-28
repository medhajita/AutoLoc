package autoloc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = "text/plain;charset=UTF-8")
    public String home() {
        return """
                AutoLoc API is running

                GET /api/public/ping       public
                GET /api/vehicules         USER / ADMIN (Basic Auth)
                GET /api/admin/dashboard   ADMIN (Basic Auth)
                GET /actuator/health       public
                GET /actuator/prometheus   public
                """;
    }
}
