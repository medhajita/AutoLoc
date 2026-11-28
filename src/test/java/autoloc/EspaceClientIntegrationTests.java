package autoloc;

import com.jayway.jsonpath.JsonPath;
import autoloc.domain.CategorieVehicule;
import autoloc.domain.StatutVehicule;
import autoloc.domain.Vehicule;
import autoloc.repository.VehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:espaceclient;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class EspaceClientIntegrationTests {

    private static final String JSON_EMMA = """
            {"nom":"Girard","prenom":"Emma","email":"emma.girard@autoloc.com","telephone":"0600000000",
             "numPermis":"38ST61001","motDePasse":"motdepasse1"}""";
    private static final String JSON_HUGO = """
            {"nom":"Fabre","prenom":"Hugo","email":"hugo.fabre@autoloc.com",
             "numPermis":"38ST61002","motDePasse":"motdepasse2"}""";

    private static final String JSON_CLAIRE = """
            {"nom":"Perrin","prenom":"Claire","email":"claire.perrin@autoloc.com",
             "numPermis":"38ST61003","motDePasse":"client123"}""";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private VehiculeRepository vehiculeRepository;

    private long berlineId;

    @BeforeEach
    void flotte() {
        if (vehiculeRepository.count() > 0) {
            berlineId = vehiculeRepository.findAll().stream()
                    .filter(v -> v.getModele().equals("508")).findFirst().orElseThrow().getIdVehicule();
            return;
        }
        vehiculeRepository.save(vehicule("AA-201-BB", "Renault", "Clio", CategorieVehicule.CITADINE, "35.00", StatutVehicule.DISPONIBLE));
        berlineId = vehiculeRepository.save(vehicule("AA-202-BB", "Peugeot", "508", CategorieVehicule.BERLINE, "60.00", StatutVehicule.DISPONIBLE)).getIdVehicule();
        vehiculeRepository.save(vehicule("AA-203-BB", "Toyota", "RAV4", CategorieVehicule.SUV, "80.00", StatutVehicule.MAINTENANCE));
    }

    private static Vehicule vehicule(String plaque, String marque, String modele, CategorieVehicule cat, String tarif,
                                     StatutVehicule statut) {
        Vehicule v = new Vehicule();
        v.setImmatriculation(plaque);
        v.setMarque(marque);
        v.setModele(modele);
        v.setCategorie(cat);
        v.setTarifJournalier(new BigDecimal(tarif));
        v.setStatut(statut);
        return v;
    }

    private void inscrire(String json) throws Exception {
        mockMvc.perform(post("/api/espace/inscription").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void clientCanRegisterReserveModifyAndCancelOwnReservationOnly() throws Exception {
        inscrire(JSON_EMMA);
        inscrire(JSON_HUGO);

        // inscription en double refusée
        mockMvc.perform(post("/api/espace/inscription").contentType(MediaType.APPLICATION_JSON).content(JSON_EMMA))
                .andExpect(status().isConflict());

        // catalogue public, véhicule en maintenance exclu
        mockMvc.perform(get("/api/espace/vehicules?debut=2027-03-01&fin=2027-03-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].disponible").value(true));

        // mes réservations : authentification obligatoire
        mockMvc.perform(get("/api/espace/reservations")).andExpect(status().isUnauthorized());

        // Emma réserve le véhicule 2
        String res = mockMvc.perform(post("/api/espace/reservations").with(httpBasic("emma.girard@autoloc.com", "motdepasse1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehiculeId\":" + berlineId + ",\"dateDebut\":\"2027-03-01\",\"dateFin\":\"2027-03-05\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.nbJours").value(5))
                .andExpect(jsonPath("$.total").value(300.0))
                .andExpect(jsonPath("$.annulable").value(true))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(res, "$.idReservation");

        // le véhicule n'est plus disponible sur ces dates et Hugo ne peut pas le réserver
        mockMvc.perform(get("/api/espace/vehicules?debut=2027-03-03&fin=2027-03-04"))
                .andExpect(jsonPath("$[?(@.idVehicule==" + berlineId + ")].disponible").value(false));
        mockMvc.perform(post("/api/espace/reservations").with(httpBasic("hugo.fabre@autoloc.com", "motdepasse2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehiculeId\":" + berlineId + ",\"dateDebut\":\"2027-03-04\",\"dateFin\":\"2027-03-06\"}"))
                .andExpect(status().isConflict());

        // dates dans le passé refusées
        mockMvc.perform(post("/api/espace/reservations").with(httpBasic("hugo.fabre@autoloc.com", "motdepasse2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehiculeId\":" + berlineId + ",\"dateDebut\":\"2020-01-01\",\"dateFin\":\"2020-01-02\"}"))
                .andExpect(status().isBadRequest());

        // Hugo ne voit ni ne touche la réservation d'Emma
        mockMvc.perform(get("/api/espace/reservations").with(httpBasic("hugo.fabre@autoloc.com", "motdepasse2")))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(patch("/api/espace/reservations/" + id + "/annuler")
                        .with(httpBasic("hugo.fabre@autoloc.com", "motdepasse2")))
                .andExpect(status().isNotFound());

        // Emma modifie ses dates puis annule
        mockMvc.perform(put("/api/espace/reservations/" + id).with(httpBasic("emma.girard@autoloc.com", "motdepasse1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateDebut\":\"2027-03-02\",\"dateFin\":\"2027-03-03\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nbJours").value(2));
        mockMvc.perform(patch("/api/espace/reservations/" + id + "/annuler")
                        .with(httpBasic("emma.girard@autoloc.com", "motdepasse1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"))
                .andExpect(jsonPath("$.annulable").value(false));
    }

    @Test
    void clientRoleCannotUseBackOfficeApi() throws Exception {
        inscrire(JSON_CLAIRE);
        mockMvc.perform(get("/api/clients").with(httpBasic("claire.perrin@autoloc.com", "client123")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reservations").with(httpBasic("claire.perrin@autoloc.com", "client123")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard").with(httpBasic("claire.perrin@autoloc.com", "client123")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/me").with(httpBasic("claire.perrin@autoloc.com", "client123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CLIENT"));
        // le personnel n'a pas accès à l'espace client
        mockMvc.perform(get("/api/espace/reservations").with(httpBasic("admin", "admin123")))
                .andExpect(status().isForbidden());
    }
}
