package autoloc;

import com.jayway.jsonpath.JsonPath;
import autoloc.domain.CategorieVehicule;
import autoloc.domain.Client;
import autoloc.domain.StatutVehicule;
import autoloc.domain.Vehicule;
import autoloc.repository.ClientRepository;
import autoloc.repository.VehiculeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:rentalflow;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class RentalFlowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private VehiculeRepository vehiculeRepository;

    @Test
    void reservationContratPaiementFlowThenAdminOnlyDelete() throws Exception {
        Client emma = new Client();
        emma.setNom("Girard");
        emma.setPrenom("Emma");
        emma.setEmail("emma.girard@autoloc.com");
        emma.setNumPermis("38ST61001");
        emma.setDateInscription(LocalDate.now());
        long clientId = clientRepository.save(emma).getIdClient();
        Vehicule clio = new Vehicule();
        clio.setImmatriculation("AA-101-BB");
        clio.setMarque("Renault");
        clio.setModele("Clio");
        clio.setCategorie(CategorieVehicule.CITADINE);
        clio.setTarifJournalier(new BigDecimal("35.00"));
        clio.setStatut(StatutVehicule.DISPONIBLE);
        long vehiculeId = vehiculeRepository.save(clio).getIdVehicule();

        String res = mockMvc.perform(post("/api/reservations").with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"vehiculeId\":" + vehiculeId + ",\"dateDebut\":\"2030-12-01\",\"dateFin\":\"2030-12-03\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int reservationId = JsonPath.read(res, "$.idReservation");

        String contrat = mockMvc.perform(post("/api/contrats").with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":" + reservationId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andReturn().getResponse().getContentAsString();
        int contratId = JsonPath.read(contrat, "$.idContrat");

        mockMvc.perform(post("/api/contrats/" + contratId + "/paiements").with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":50,\"modePaiement\":\"CARTE_BANCAIRE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paiements.length()").value(1));

        mockMvc.perform(get("/api/dashboard").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contrats").value(1))
                .andExpect(jsonPath("$.revenus").value(50));

        mockMvc.perform(delete("/api/contrats/" + contratId).with(user("emma.girard@autoloc.com").roles("CLIENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/contrats/" + contratId).with(httpBasic("admin", "admin123")))
                .andExpect(status().isNoContent());
    }
}
