package autoloc;

import autoloc.config.DemoDataRunner;
import autoloc.domain.Client;
import autoloc.domain.CompteClient;
import autoloc.domain.Reservation;
import autoloc.domain.StatutReservation;
import autoloc.domain.StatutVehicule;
import autoloc.domain.Vehicule;
import autoloc.repository.ClientRepository;
import autoloc.repository.CompteClientRepository;
import autoloc.repository.ContratRepository;
import autoloc.repository.ReservationRepository;
import autoloc.repository.VehiculeRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie le jeu de données chargé au premier démarrage : flotte, clients @autoloc.com, 9 réservations de
 * statuts variés, un seul administrateur, cohérence des contrats et des disponibilités.
 * Ces tests ne modifient pas les données.
 */
@SpringBootTest(properties = {
        "autoloc.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:seed;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class SeedDataTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private VehiculeRepository vehicules;
    @Autowired private ClientRepository clients;
    @Autowired private CompteClientRepository comptes;
    @Autowired private ReservationRepository reservations;
    @Autowired private ContratRepository contrats;
    @Autowired private DemoDataRunner runner;
    @Value("${AUTOLOC_DEMO_CLIENT_PASSWORD:client123}") private String clientPassword;

    private long compter(StatutReservation statut) {
        return reservations.countByStatut(statut);
    }

    @Test
    void flotteDeDixVehiculesAvecImmatriculationsUniques() {
        List<Vehicule> flotte = vehicules.findAll();
        assertThat(flotte).hasSize(10);
        assertThat(flotte.stream().map(Vehicule::getImmatriculation).distinct()).hasSize(10);
        assertThat(flotte).allSatisfy(v -> {
            assertThat(v.getTarifJournalier()).isPositive();
            assertThat(v.getAgence()).isNotNull();
        });
        assertThat(flotte.stream().map(Vehicule::getCategorie).distinct()).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void seizeReservationsDeStatutsMelanges() {
        assertThat(reservations.count()).isEqualTo(16);
        assertThat(compter(StatutReservation.CONFIRMEE)).isEqualTo(7);
        assertThat(compter(StatutReservation.EN_ATTENTE)).isEqualTo(6);
        assertThat(compter(StatutReservation.ANNULEE)).isEqualTo(2);
        assertThat(compter(StatutReservation.TERMINEE)).isEqualTo(1);
    }

    @Test
    void clientsEnAutolocPointComSansNomDeTest() {
        List<Client> liste = clients.findAll();
        assertThat(liste).hasSize(9);
        assertThat(liste).allSatisfy(c -> {
            assertThat(c.getEmail()).endsWith("@autoloc.com");
            assertThat(c.getNumPermis()).isNotBlank();
            String identite = (c.getEmail() + c.getNom() + c.getPrenom()).toLowerCase();
            assertThat(identite).doesNotContain("test", "demo", "example", "fake");
        });
        // chaque client a un compte de connexion associé à son email
        assertThat(comptes.findAll().stream().map(CompteClient::getEmail).collect(Collectors.toSet()))
                .isEqualTo(liste.stream().map(Client::getEmail).collect(Collectors.toSet()));
    }

    @Test
    void aucuneReservationActiveNeSeChevauchePourUnMemeVehicule() {
        Map<Long, List<Reservation>> parVehicule = reservations.findAll().stream()
                .filter(r -> r.getStatut() != StatutReservation.ANNULEE)
                .collect(Collectors.groupingBy(r -> r.getVehicule().getIdVehicule()));
        parVehicule.values().forEach(liste -> {
            for (int i = 0; i < liste.size(); i++) {
                for (int j = i + 1; j < liste.size(); j++) {
                    Reservation a = liste.get(i), b = liste.get(j);
                    boolean chevauche = !a.getDateDebut().isAfter(b.getDateFin())
                            && !b.getDateDebut().isAfter(a.getDateFin());
                    assertThat(chevauche).as("%s / %s", a.getIdReservation(), b.getIdReservation()).isFalse();
                }
            }
        });
        assertThat(reservations.findAll()).allSatisfy(r ->
                assertThat(r.getDateFin()).isAfterOrEqualTo(r.getDateDebut()));
    }

    @Test
    void unSeulAdministrateurEtLesClientsNeSontPasAdmin() throws Exception {
        mockMvc.perform(get("/api/me").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
        for (CompteClient compte : comptes.findAll()) {
            mockMvc.perform(get("/api/me").with(httpBasic(compte.getEmail(), clientPassword)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roles[?(@=='ROLE_CLIENT')]").isNotEmpty())
                    .andExpect(jsonPath("$.roles[?(@=='ROLE_ADMIN')]").isEmpty());
            mockMvc.perform(get("/api/admin/dashboard").with(httpBasic(compte.getEmail(), clientPassword)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void ancienCompteStudentSupprimeEtRolesPorteursParAdmin() throws Exception {
        mockMvc.perform(get("/api/me").with(httpBasic("student", "student123")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me").with(httpBasic("admin", "admin123")))
                .andExpect(jsonPath("$.roles[?(@=='ROLE_ADMIN')]").isNotEmpty())
                .andExpect(jsonPath("$.roles[?(@=='ROLE_USER')]").isNotEmpty());
    }

    @Test
    void chaqueClientNeVoitQueSesReservations() throws Exception {
        int total = 0;
        for (CompteClient compte : comptes.findAll()) {
            String json = mockMvc.perform(get("/api/espace/reservations").with(httpBasic(compte.getEmail(), clientPassword)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            total += JsonPath.<List<Object>>read(json, "$").size();
        }
        assertThat(total).isEqualTo(16);

        mockMvc.perform(get("/api/espace/reservations").with(httpBasic("marie.dupont@autoloc.com", clientPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.vehiculeLabel=='BMW Série 3')].statut").value("CONFIRMEE"))
                .andExpect(jsonPath("$[?(@.vehiculeLabel=='BMW Série 3')].annulable").value(false)) // contrat signé
                .andExpect(jsonPath("$[?(@.vehiculeLabel=='Dacia Duster')].annulable").value(true));
    }

    @Test
    void catalogueExclutLaMaintenanceEtMontreLesIndisponibilites() throws Exception {
        mockMvc.perform(get("/api/espace/vehicules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[?(@.modele=='RAV4')]").isEmpty());

        // La Clio est réservée (en attente) du jour+3 au jour+6 ; la Tiguan est libre à ces dates
        LocalDate debut = LocalDate.now().plusDays(4);
        LocalDate fin = LocalDate.now().plusDays(5);
        mockMvc.perform(get("/api/espace/vehicules?debut=" + debut + "&fin=" + fin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.modele=='Clio')].disponible").value(false))
                .andExpect(jsonPath("$[?(@.modele=='Tiguan')].disponible").value(true));

        // Les réservations annulées libèrent le véhicule : la 208 est libre aux dates de son annulation
        mockMvc.perform(get("/api/espace/vehicules?debut=" + LocalDate.now().plusDays(2)
                        + "&fin=" + LocalDate.now().plusDays(4)))
                .andExpect(jsonPath("$[?(@.modele=='208')].disponible").value(true));
    }

    @Test
    void calendrierDeDisponibiliteParJour() throws Exception {
        LocalDate debut = LocalDate.now();
        // 9 véhicules hors maintenance. Occupés : BMW (j-1→j+3), Clio (j+3→j+6), 508 (j+5→j+8), Duster (j+7→j+9)
        mockMvc.perform(get("/api/espace/disponibilites?debut=" + debut + "&fin=" + debut.plusDays(9)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].total").value(9))
                .andExpect(jsonPath("$[0].libres").value(8))  // BMW
                .andExpect(jsonPath("$[2].libres").value(8))  // la 208 annulée ne compte pas
                .andExpect(jsonPath("$[3].libres").value(7))  // BMW + Clio
                .andExpect(jsonPath("$[5].libres").value(7))  // Clio + 508
                .andExpect(jsonPath("$[9].libres").value(8)); // Duster
        mockMvc.perform(get("/api/espace/disponibilites?debut=" + debut + "&fin=" + debut.minusDays(1)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/espace/disponibilites?debut=" + debut + "&fin=" + debut.plusDays(200)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void periodeDePointeToutesLesVoituresSontPrises() throws Exception {
        LocalDate debut = LocalDate.now().plusDays(11);
        // jours +13 et +14 : les 9 véhicules réservables sont pris → « complet » dans le calendrier
        mockMvc.perform(get("/api/espace/disponibilites?debut=" + debut + "&fin=" + debut.plusDays(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].libres").value(0))
                .andExpect(jsonPath("$[3].libres").value(0))
                .andExpect(jsonPath("$[0].libres").isNumber())
                .andExpect(jsonPath("$[1].libres").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$[5].libres").value(org.hamcrest.Matchers.greaterThan(0)));
        // le catalogue ne propose plus aucun véhicule ces jours-là
        mockMvc.perform(get("/api/espace/vehicules?debut=" + debut.plusDays(2) + "&fin=" + debut.plusDays(3)))
                .andExpect(jsonPath("$[?(@.disponible==true)]").isEmpty());
        // un client ne peut plus réserver sur ces dates (409)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/espace/reservations")
                        .with(httpBasic("marie.dupont@autoloc.com", clientPassword))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"vehiculeId\":" + vehicules.findAll().get(0).getIdVehicule()
                                + ",\"dateDebut\":\"" + debut.plusDays(2) + "\",\"dateFin\":\"" + debut.plusDays(3) + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void tableauDeBordCoherentAvecLesDonnees() throws Exception {
        long louees = vehicules.countByStatut(StatutVehicule.LOUE);
        long maintenance = vehicules.countByStatut(StatutVehicule.MAINTENANCE);
        assertThat(louees).isEqualTo(1);
        assertThat(maintenance).isEqualTo(1);

        mockMvc.perform(get("/api/dashboard").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicules").value(10))
                .andExpect(jsonPath("$.vehiculesDisponibles").value(8))
                .andExpect(jsonPath("$.clients").value(9))
                .andExpect(jsonPath("$.reservationsEnAttente").value(6))
                .andExpect(jsonPath("$.reservationsConfirmees").value(7))
                .andExpect(jsonPath("$.contrats").value(3))
                .andExpect(jsonPath("$.revenus").value(630));
    }

    @Test
    void contratsValidesAvecPaiementsSansDepassement() throws Exception {
        String json = mockMvc.perform(get("/api/contrats").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> liste = JsonPath.read(json, "$");
        assertThat(liste).hasSize(3);
        for (Map<String, Object> c : liste) {
            BigDecimal montant = new BigDecimal(c.get("montantTotal").toString());
            BigDecimal paye = new BigDecimal(c.get("totalPaye").toString());
            assertThat(c.get("valide")).isEqualTo(true);
            assertThat(paye).isPositive().isLessThanOrEqualTo(montant);
        }
        assertThat(contrats.totalPaiements()).isEqualByComparingTo("630");
        // un contrat n'existe que pour une réservation confirmée ou terminée
        assertThat(reservations.findAll().stream().filter(r -> r.getContrat() != null))
                .allSatisfy(r -> assertThat(r.getStatut())
                        .isIn(StatutReservation.CONFIRMEE, StatutReservation.TERMINEE));
    }

    @Test
    void relancerLeChargementNeDupliquePas() {
        runner.run();
        assertThat(vehicules.count()).isEqualTo(10);
        assertThat(clients.count()).isEqualTo(9);
        assertThat(reservations.count()).isEqualTo(16);
        assertThat(comptes.count()).isEqualTo(9);
    }
}
