package autoloc.repository;

import autoloc.domain.StatutVehicule;
import autoloc.domain.Vehicule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehiculeRepository extends JpaRepository<Vehicule, Long> {

    /** Charge l'agence avec les véhicules (évite une requête par véhicule). */
    @Override
    @EntityGraph(attributePaths = "agence")
    List<Vehicule> findAll();

    long countByStatut(StatutVehicule statut);
}
