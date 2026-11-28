package autoloc.repository;

import autoloc.domain.Reservation;
import autoloc.domain.StatutReservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            select count(r) > 0 from Reservation r
            where r.vehicule.idVehicule = :vehiculeId
              and r.statut <> autoloc.domain.StatutReservation.ANNULEE
              and r.dateDebut <= :fin and r.dateFin >= :debut
            """)
    boolean existsOverlap(@Param("vehiculeId") Long vehiculeId,
                          @Param("debut") LocalDate debut,
                          @Param("fin") LocalDate fin);

    /** Même contrôle que {@link #existsOverlap}, en ignorant une réservation (modification de dates). */
    @Query("""
            select count(r) > 0 from Reservation r
            where r.vehicule.idVehicule = :vehiculeId
              and r.idReservation <> :excludeId
              and r.statut <> autoloc.domain.StatutReservation.ANNULEE
              and r.dateDebut <= :fin and r.dateFin >= :debut
            """)
    boolean existsOverlapExcluding(@Param("vehiculeId") Long vehiculeId,
                                   @Param("debut") LocalDate debut,
                                   @Param("fin") LocalDate fin,
                                   @Param("excludeId") Long excludeId);

    /** Identifiants des véhicules ayant une réservation active sur la période (une seule requête). */
    @Query("""
            select distinct r.vehicule.idVehicule from Reservation r
            where r.statut <> autoloc.domain.StatutReservation.ANNULEE
              and r.dateDebut <= :fin and r.dateFin >= :debut
            """)
    List<Long> vehiculeIdsOccupes(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Override
    @EntityGraph(attributePaths = {"client", "vehicule", "contrat"})
    List<Reservation> findAll();

    @EntityGraph(attributePaths = {"vehicule", "contrat"})
    List<Reservation> findByClientIdClientOrderByDateDebutDesc(Long idClient);

    long countByStatut(StatutReservation statut);
}
