package autoloc.config;

import autoloc.domain.Agence;
import autoloc.domain.CategorieVehicule;
import autoloc.domain.Client;
import autoloc.domain.CompteClient;
import autoloc.domain.StatutVehicule;
import autoloc.domain.Vehicule;
import autoloc.repository.AgenceRepository;
import autoloc.repository.ClientRepository;
import autoloc.repository.CompteClientRepository;
import autoloc.repository.VehiculeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DemoDataRunner implements CommandLineRunner {

    private final VehiculeRepository vehiculeRepository;
    private final AgenceRepository agenceRepository;
    private final ClientRepository clientRepository;
    private final CompteClientRepository compteClientRepository;
    private final PasswordEncoder encoder;
    private final String demoClientPassword;

    public DemoDataRunner(VehiculeRepository vehiculeRepository, AgenceRepository agenceRepository,
                          ClientRepository clientRepository, CompteClientRepository compteClientRepository,
                          PasswordEncoder encoder,
                          @Value("${AUTOLOC_DEMO_CLIENT_PASSWORD:client123}") String demoClientPassword) {
        this.demoClientPassword = demoClientPassword;
        this.vehiculeRepository = vehiculeRepository;
        this.agenceRepository = agenceRepository;
        this.clientRepository = clientRepository;
        this.compteClientRepository = compteClientRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        Agence agence = agenceRepository.findAll().stream().findFirst().orElseGet(() -> {
            Agence a = new Agence();
            a.setNom("AutoLoc Paris Centre");
            a.setVille("Paris");
            a.setAdresse("12 rue de Rivoli");
            a.setTelephone("0140000000");
            return agenceRepository.save(a);
        });

        if (clientRepository.count() == 0) {
            clientRepository.saveAll(List.of(
                    client("Dupont", "Marie", "marie.dupont@example.com", "PERMIS-0001"),
                    client("Martin", "Lucas", "lucas.martin@example.com", "PERMIS-0002")
            ));
        }

        // Compte de démonstration de l'espace client : marie.dupont@example.com / client123
        if (!compteClientRepository.existsByEmailIgnoreCase("marie.dupont@example.com")) {
            clientRepository.findAll().stream()
                    .filter(c -> "marie.dupont@example.com".equalsIgnoreCase(c.getEmail())).findFirst()
                    .ifPresent(c -> {
                        CompteClient compte = new CompteClient();
                        compte.setEmail(c.getEmail());
                        compte.setMotDePasse(encoder.encode(demoClientPassword));
                        compte.setClient(c);
                        compteClientRepository.save(compte);
                    });
        }

        if (vehiculeRepository.count() == 0) {
            vehiculeRepository.saveAll(List.of(
                    vehicule("AB-123-CD", "Renault", "Clio", CategorieVehicule.CITADINE, "35.00", StatutVehicule.DISPONIBLE, agence),
                    vehicule("EF-456-GH", "Peugeot", "508", CategorieVehicule.BERLINE, "60.00", StatutVehicule.DISPONIBLE, agence),
                    vehicule("IJ-789-KL", "Toyota", "RAV4", CategorieVehicule.SUV, "80.00", StatutVehicule.MAINTENANCE, agence)
            ));
        }
    }

    private Client client(String nom, String prenom, String email, String permis) {
        Client c = new Client();
        c.setNom(nom);
        c.setPrenom(prenom);
        c.setEmail(email);
        c.setNumPermis(permis);
        c.setDateInscription(LocalDate.now());
        return c;
    }

    private Vehicule vehicule(String immatriculation, String marque, String modele, CategorieVehicule categorie,
                              String tarif, StatutVehicule statut, Agence agence) {
        Vehicule v = new Vehicule();
        v.setImmatriculation(immatriculation);
        v.setMarque(marque);
        v.setModele(modele);
        v.setCategorie(categorie);
        v.setTarifJournalier(new BigDecimal(tarif));
        v.setStatut(statut);
        v.setAgence(agence);
        return v;
    }
}
