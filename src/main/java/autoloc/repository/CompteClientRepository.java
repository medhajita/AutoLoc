package autoloc.repository;

import autoloc.domain.CompteClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompteClientRepository extends JpaRepository<CompteClient, Long> {

    Optional<CompteClient> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
