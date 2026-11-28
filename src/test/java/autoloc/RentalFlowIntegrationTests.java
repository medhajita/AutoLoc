package autoloc;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RentalFlowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reservationContratPaiementFlowThenAdminOnlyDelete() throws Exception {
        String res = mockMvc.perform(post("/api/reservations").with(httpBasic("student", "student123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"vehiculeId\":1,\"dateDebut\":\"2026-12-01\",\"dateFin\":\"2026-12-03\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int reservationId = JsonPath.read(res, "$.idReservation");

        String contrat = mockMvc.perform(post("/api/contrats").with(httpBasic("student", "student123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":" + reservationId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andReturn().getResponse().getContentAsString();
        int contratId = JsonPath.read(contrat, "$.idContrat");

        mockMvc.perform(post("/api/contrats/" + contratId + "/paiements").with(httpBasic("student", "student123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":50,\"modePaiement\":\"CARTE_BANCAIRE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paiements.length()").value(1));

        mockMvc.perform(get("/api/dashboard").with(httpBasic("student", "student123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contrats").value(1))
                .andExpect(jsonPath("$.revenus").value(50));

        mockMvc.perform(delete("/api/contrats/" + contratId).with(httpBasic("student", "student123")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/contrats/" + contratId).with(httpBasic("admin", "admin123")))
                .andExpect(status().isNoContent());
    }
}
