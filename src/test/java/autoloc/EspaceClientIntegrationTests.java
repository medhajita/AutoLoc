package autoloc;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EspaceClientIntegrationTests {

    private static final String JSON_ALICE = """
            {"nom":"Durand","prenom":"Alice","email":"alice@example.com","telephone":"0600000000",
             "numPermis":"PERMIS-ALICE","motDePasse":"motdepasse1"}""";
    private static final String JSON_BOB = """
            {"nom":"Petit","prenom":"Bob","email":"bob@example.com",
             "numPermis":"PERMIS-BOB","motDePasse":"motdepasse2"}""";

    @Autowired
    private MockMvc mockMvc;

    private void inscrire(String json) throws Exception {
        mockMvc.perform(post("/api/espace/inscription").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void clientCanRegisterReserveModifyAndCancelOwnReservationOnly() throws Exception {
        inscrire(JSON_ALICE);
        inscrire(JSON_BOB);

        // inscription en double refusée
        mockMvc.perform(post("/api/espace/inscription").contentType(MediaType.APPLICATION_JSON).content(JSON_ALICE))
                .andExpect(status().isConflict());

        // catalogue public, véhicule en maintenance exclu
        mockMvc.perform(get("/api/espace/vehicules?debut=2027-03-01&fin=2027-03-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].disponible").value(true));

        // mes réservations : authentification obligatoire
        mockMvc.perform(get("/api/espace/reservations")).andExpect(status().isUnauthorized());

        // Alice réserve le véhicule 2
        String res = mockMvc.perform(post("/api/espace/reservations").with(httpBasic("alice@example.com", "motdepasse1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehiculeId\":2,\"dateDebut\":\"2027-03-01\",\"dateFin\":\"2027-03-05\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.nbJours").value(5))
                .andExpect(jsonPath("$.total").value(300.0))
                .andExpect(jsonPath("$.annulable").value(true))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(res, "$.idReservation");

        // le véhicule n'est plus disponible sur ces dates et Bob ne peut pas le réserver
        mockMvc.perform(get("/api/espace/vehicules?debut=2027-03-03&fin=2027-03-04"))
                .andExpect(jsonPath("$[?(@.idVehicule==2)].disponible").value(false));
        mockMvc.perform(post("/api/espace/reservations").with(httpBasic("bob@example.com", "motdepasse2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehiculeId\":2,\"dateDebut\":\"2027-03-04\",\"dateFin\":\"2027-03-06\"}"))
                .andExpect(status().isConflict());

        // dates dans le passé refusées
        mockMvc.perform(post("/api/espace/reservations").with(httpBasic("bob@example.com", "motdepasse2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehiculeId\":2,\"dateDebut\":\"2020-01-01\",\"dateFin\":\"2020-01-02\"}"))
                .andExpect(status().isBadRequest());

        // Bob ne voit ni ne touche la réservation d'Alice
        mockMvc.perform(get("/api/espace/reservations").with(httpBasic("bob@example.com", "motdepasse2")))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(patch("/api/espace/reservations/" + id + "/annuler")
                        .with(httpBasic("bob@example.com", "motdepasse2")))
                .andExpect(status().isNotFound());

        // Alice modifie ses dates puis annule
        mockMvc.perform(put("/api/espace/reservations/" + id).with(httpBasic("alice@example.com", "motdepasse1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateDebut\":\"2027-03-02\",\"dateFin\":\"2027-03-03\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nbJours").value(2));
        mockMvc.perform(patch("/api/espace/reservations/" + id + "/annuler")
                        .with(httpBasic("alice@example.com", "motdepasse1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"))
                .andExpect(jsonPath("$.annulable").value(false));
    }

    @Test
    void clientRoleCannotUseBackOfficeApi() throws Exception {
        mockMvc.perform(get("/api/clients").with(httpBasic("marie.dupont@example.com", "client123")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reservations").with(httpBasic("marie.dupont@example.com", "client123")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard").with(httpBasic("marie.dupont@example.com", "client123")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/me").with(httpBasic("marie.dupont@example.com", "client123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CLIENT"));
        // le personnel n'a pas accès à l'espace client
        mockMvc.perform(get("/api/espace/reservations").with(httpBasic("student", "student123")))
                .andExpect(status().isForbidden());
    }
}
