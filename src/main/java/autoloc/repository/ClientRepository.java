package autoloc.repository;

import autoloc.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByNumPermis(String numPermis);
}
