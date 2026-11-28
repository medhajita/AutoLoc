package autoloc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import autoloc.repository.CompteClientRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

import java.time.Duration;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder, CompteClientRepository comptes) {
        var user = User.builder()
                .username("student")
                .password(encoder.encode("student123"))
                .roles("USER")
                .build();

        var admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin123"))
                .roles("ADMIN")
                .build();

        var enMemoire = new InMemoryUserDetailsManager(user, admin);

        // Comptes en mémoire (student, admin) puis comptes clients en base (email, rôle CLIENT)
        return username -> {
            try {
                return enMemoire.loadUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                return comptes.findByEmailIgnoreCase(username)
                        .map(c -> (UserDetails) User.builder()
                                .username(c.getEmail())
                                .password(c.getMotDePasse())
                                .roles("CLIENT")
                                .build())
                        .orElseThrow(() -> e);
            }
        };
    }

    /**
     * 401 avec WWW-Authenticate (curl/Postman), sans l'en-tête pour l'interface React
     * (X-Requested-With) afin d'éviter la fenêtre de login native du navigateur.
     */
    private AuthenticationEntryPoint entryPoint() {
        var basic = new BasicAuthenticationEntryPoint();
        basic.setRealmName("Realm");
        var plain = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        return (request, response, ex) -> {
            if (request.getHeader("X-Requested-With") != null) {
                plain.commence(request, response, ex);
            } else {
                basic.commence(request, response, ex);
            }
        };
    }

    /** BCrypt est coûteux : les authentifications réussies sont gardées 5 minutes en mémoire. */
    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService users, PasswordEncoder encoder) {
        var dao = new DaoAuthenticationProvider(users);
        dao.setPasswordEncoder(encoder);
        return new CachingAuthenticationProvider(dao, Duration.ofMinutes(5));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider provider)
            throws Exception {
        http
                .authenticationProvider(provider)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/espace/inscription").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/espace/vehicules").permitAll()
                        .requestMatchers("/api/espace/**").hasRole("CLIENT")
                        .requestMatchers("/api/me").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                )
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint()));

        return http.build();
    }
}
