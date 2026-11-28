package autoloc.repository;

import autoloc.domain.Contrat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface ContratRepository extends JpaRepository<Contrat, Long> {

    /** Charge réservation, client, véhicule et paiements en une seule requête. */
    @Override
    @EntityGraph(attributePaths = {"reservation", "reservation.client", "reservation.vehicule", "paiements"})
    List<Contrat> findAll();

    /** Total encaissé, calculé en base (le tableau de bord ne charge plus tous les contrats). */
    @Query("select coalesce(sum(p.montant), 0) from Paiement p")
    BigDecimal totalPaiements();
}
