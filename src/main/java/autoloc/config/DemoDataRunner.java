package autoloc.config;

import autoloc.domain.Agence;
import autoloc.domain.CategorieVehicule;
import autoloc.domain.Client;
import autoloc.domain.CompteClient;
import autoloc.domain.Contrat;
import autoloc.domain.Maintenance;
import autoloc.domain.ModePaiement;
import autoloc.domain.Paiement;
import autoloc.domain.Reservation;
import autoloc.domain.StatutReservation;
import autoloc.domain.StatutVehicule;
import autoloc.domain.Vehicule;
import autoloc.repository.AgenceRepository;
import autoloc.repository.ClientRepository;
import autoloc.repository.CompteClientRepository;
import autoloc.repository.ReservationRepository;
import autoloc.repository.VehiculeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Jeu de données réaliste, chargé uniquement quand la base est vide :
 * 1 agence, 10 véhicules, 9 clients (@autoloc.com) avec compte, 16 réservations de tous statuts
 * dont une période où toute la flotte est réservée (calendrier « complet »).
 * Les dates sont calculées par rapport à aujourd'hui pour que le site reste cohérent.
 * Désactivable avec {@code autoloc.seed.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "autoloc.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataRunner implements CommandLineRunner {

    private final VehiculeRepository vehiculeRepository;
    private final AgenceRepository agenceRepository;
    private final ClientRepository clientRepository;
    private final CompteClientRepository compteClientRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder encoder;
    private final String clientPassword;

    public DemoDataRunner(VehiculeRepository vehiculeRepository, AgenceRepository agenceRepository,
                          ClientRepository clientRepository, CompteClientRepository compteClientRepository,
                          ReservationRepository reservationRepository, PasswordEncoder encoder,
                          @Value("${AUTOLOC_DEMO_CLIENT_PASSWORD:client123}") String clientPassword) {
        this.vehiculeRepository = vehiculeRepository;
        this.agenceRepository = agenceRepository;
        this.clientRepository = clientRepository;
        this.compteClientRepository = compteClientRepository;
        this.reservationRepository = reservationRepository;
        this.encoder = encoder;
        this.clientPassword = clientPassword;
    }

    @Override
    public void run(String... args) {
        if (vehiculeRepository.count() > 0 || clientRepository.count() > 0) {
            return; // base déjà alimentée : on ne touche à rien
        }
        LocalDate today = LocalDate.now();

        Agence agence = agenceRepository.findAll().stream().findFirst().orElseGet(() -> {
            Agence a = new Agence();
            a.setNom("AutoLoc Paris Centre");
            a.setVille("Paris");
            a.setAdresse("12 rue de Rivoli");
            a.setTelephone("0140000000");
            return agenceRepository.save(a);
        });

        Vehicule toyota = vehicule("PD-649-XE", "Toyota", "RAV4", CategorieVehicule.SUV, "80.00", StatutVehicule.MAINTENANCE, agence);
        // Le RAV4 est en atelier : c'est ce qui l'exclut du catalogue client
        Maintenance revision = new Maintenance();
        revision.setDateDebut(today.minusDays(2));
        revision.setDateFin(today.plusDays(6));
        revision.setDescription("Révision des 60 000 km et changement des plaquettes");
        revision.setVehicule(toyota);
        toyota.getMaintenances().add(revision);

        List<Vehicule> v = vehiculeRepository.saveAll(List.of(
                vehicule("GH-214-KL", "Renault", "Clio", CategorieVehicule.CITADINE, "35.00", StatutVehicule.DISPONIBLE, agence),
                vehicule("JP-587-MA", "Peugeot", "208", CategorieVehicule.CITADINE, "38.00", StatutVehicule.DISPONIBLE, agence),
                vehicule("KT-931-RD", "Fiat", "500", CategorieVehicule.CITADINE, "32.00", StatutVehicule.DISPONIBLE, agence),
                vehicule("LB-402-NC", "Peugeot", "508", CategorieVehicule.BERLINE, "60.00", StatutVehicule.DISPONIBLE, agence),
                vehicule("MC-776-ZS", "BMW", "Série 3", CategorieVehicule.BERLINE, "85.00", StatutVehicule.LOUE, agence),
                vehicule("NF-158-VT", "Tesla", "Model 3", CategorieVehicule.BERLINE, "90.00", StatutVehicule.DISPONIBLE, agence),
                toyota,
                vehicule("QR-305-BH", "Volkswagen", "Tiguan", CategorieVehicule.SUV, "75.00", StatutVehicule.DISPONIBLE, agence),
                vehicule("RS-813-GJ", "Dacia", "Duster", CategorieVehicule.SUV, "55.00", StatutVehicule.DISPONIBLE, agence),
                vehicule("TV-092-PL", "Renault", "Master", CategorieVehicule.UTILITAIRE, "70.00", StatutVehicule.DISPONIBLE, agence)
        ));
        Vehicule clio = v.get(0), p208 = v.get(1), fiat = v.get(2), p508 = v.get(3), bmw = v.get(4),
                tesla = v.get(5), tiguan = v.get(7), duster = v.get(8), master = v.get(9);

        List<Client> c = clientRepository.saveAll(List.of(
                client("Dupont", "Marie", "marie.dupont@autoloc.com", "0612345601", "75AB12301", today.minusMonths(14)),
                client("Martin", "Lucas", "lucas.martin@autoloc.com", "0612345602", "69CD45602", today.minusMonths(11)),
                client("Bernard", "Camille", "camille.bernard@autoloc.com", "0612345603", "33EF78903", today.minusMonths(9)),
                client("Petit", "Thomas", "thomas.petit@autoloc.com", "0612345604", "13GH10404", today.minusMonths(7)),
                client("Moreau", "Sarah", "sarah.moreau@autoloc.com", "0612345605", "31JK23505", today.minusMonths(5)),
                client("Benali", "Karim", "karim.benali@autoloc.com", "0612345606", "59LM36706", today.minusMonths(4)),
                client("Lambert", "Julie", "julie.lambert@autoloc.com", "0612345607", "44NP49807", today.minusMonths(2)),
                client("Roux", "Antoine", "antoine.roux@autoloc.com", "0612345608", "67QR52908", today.minusWeeks(3)),
                client("Garnier", "Léa", "lea.garnier@autoloc.com", "0612345609", "06ST63009", today.minusWeeks(1))
        ));
        List<CompteClient> comptes = new ArrayList<>();
        for (Client client : c) {
            CompteClient compte = new CompteClient();
            compte.setEmail(client.getEmail());
            compte.setMotDePasse(encoder.encode(clientPassword));
            compte.setClient(client);
            comptes.add(compte);
        }
        compteClientRepository.saveAll(comptes);

        // 16 réservations : 7 confirmées, 6 en attente, 2 annulées, 1 terminée
        // Terminée, contrat signé et soldé
        Reservation r1 = reservation(c.get(1), master, today.minusDays(20), today.minusDays(17), StatutReservation.TERMINEE);
        signer(r1, today.minusDays(21), paiement("280.00", ModePaiement.CARTE_BANCAIRE, today.minusDays(21)));
        // Confirmée, en cours : contrat signé, acompte versé
        Reservation r2 = reservation(c.get(0), bmw, today.minusDays(1), today.plusDays(3), StatutReservation.CONFIRMEE);
        signer(r2, today.minusDays(2), paiement("200.00", ModePaiement.CARTE_BANCAIRE, today.minusDays(2)));
        // Confirmée à venir, contrat signé
        Reservation r3 = reservation(c.get(3), tesla, today.plusDays(10), today.plusDays(14), StatutReservation.CONFIRMEE);
        signer(r3, today, paiement("150.00", ModePaiement.VIREMENT, today));
        // Confirmée à venir, pas encore de contrat
        Reservation r4 = reservation(c.get(2), p508, today.plusDays(5), today.plusDays(8), StatutReservation.CONFIRMEE);
        // En attente de validation par l'agence
        Reservation r5 = reservation(c.get(4), clio, today.plusDays(3), today.plusDays(6), StatutReservation.EN_ATTENTE);
        Reservation r6 = reservation(c.get(5), duster, today.plusDays(7), today.plusDays(9), StatutReservation.EN_ATTENTE);
        Reservation r7 = reservation(c.get(6), tiguan, today.plusDays(12), today.plusDays(15), StatutReservation.EN_ATTENTE);
        // Annulées
        Reservation r8 = reservation(c.get(7), p208, today.plusDays(2), today.plusDays(4), StatutReservation.ANNULEE);
        Reservation r9 = reservation(c.get(4), fiat, today.plusDays(20), today.plusDays(22), StatutReservation.ANNULEE);

        // Période de pointe : les 9 véhicules réservables sont tous pris les jours +13 et +14 (Tesla et Tiguan
        // ci-dessus, plus les 7 réservations suivantes) → ces jours apparaissent « complet » dans le calendrier
        Reservation p1 = reservation(c.get(7), clio, today.plusDays(12), today.plusDays(15), StatutReservation.CONFIRMEE);
        Reservation p2 = reservation(c.get(1), p208, today.plusDays(13), today.plusDays(16), StatutReservation.EN_ATTENTE);
        Reservation p3 = reservation(c.get(2), fiat, today.plusDays(12), today.plusDays(14), StatutReservation.CONFIRMEE);
        Reservation p4 = reservation(c.get(4), p508, today.plusDays(13), today.plusDays(14), StatutReservation.CONFIRMEE);
        Reservation p5 = reservation(c.get(5), bmw, today.plusDays(13), today.plusDays(17), StatutReservation.CONFIRMEE);
        Reservation p6 = reservation(c.get(0), duster, today.plusDays(11), today.plusDays(14), StatutReservation.EN_ATTENTE);
        Reservation p7 = reservation(c.get(8), master, today.plusDays(13), today.plusDays(15), StatutReservation.EN_ATTENTE);

        reservationRepository.saveAll(List.of(r1, r2, r3, r4, r5, r6, r7, r8, r9, p1, p2, p3, p4, p5, p6, p7));
    }

    private Client client(String nom, String prenom, String email, String telephone, String permis, LocalDate inscrit) {
        Client c = new Client();
        c.setNom(nom);
        c.setPrenom(prenom);
        c.setEmail(email);
        c.setTelephone(telephone);
        c.setNumPermis(permis);
        c.setDateInscription(inscrit);
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

    private Reservation reservation(Client client, Vehicule vehicule, LocalDate debut, LocalDate fin,
                                    StatutReservation statut) {
        Reservation r = new Reservation();
        r.setClient(client);
        r.setVehicule(vehicule);
        r.setDateDebut(debut);
        r.setDateFin(fin);
        r.setStatut(statut);
        return r;
    }

    private Paiement paiement(String montant, ModePaiement mode, LocalDate date) {
        Paiement p = new Paiement();
        p.setMontant(new BigDecimal(montant));
        p.setModePaiement(mode);
        p.setDatePaiement(date);
        return p;
    }

    /** Contrat valide (montant = tarif × jours, comme l'API) avec ses paiements. */
    private void signer(Reservation r, LocalDate dateSignature, Paiement... paiements) {
        long jours = ChronoUnit.DAYS.between(r.getDateDebut(), r.getDateFin()) + 1;
        Contrat contrat = new Contrat();
        contrat.setDateSignature(dateSignature);
        contrat.setMontantTotal(r.getVehicule().getTarifJournalier().multiply(BigDecimal.valueOf(jours)));
        contrat.setValide(true);
        contrat.setReservation(r);
        for (Paiement p : paiements) {
            p.setContrat(contrat);
            contrat.getPaiements().add(p);
        }
        r.setContrat(contrat);
    }
}
