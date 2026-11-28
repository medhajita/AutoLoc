package autoloc.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Identifiants de connexion d'un client à l'espace client (email + mot de passe BCrypt).
 * Entité séparée de {@link Client} pour ne pas modifier le modèle du sujet.
 */
@Entity
@Table(name = "compte_client")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompteClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCompte;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String motDePasse;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, unique = true)
    private Client client;
}
