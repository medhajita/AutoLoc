package autoloc.config;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Évite de recalculer BCrypt (~50 ms) à chaque requête HTTP Basic (API sans session).
 * <p>
 * Seules les authentifications réussies sont mises en cache, quelques minutes, en mémoire du processus.
 * La clé est un SHA-256 salé (sel aléatoire propre au processus) de « utilisateur + mot de passe » :
 * un mot de passe incorrect ne produit jamais de correspondance et repasse par la vérification BCrypt.
 */
public class CachingAuthenticationProvider implements AuthenticationProvider {

    private record Entry(Authentication result, long expiresAtNanos) {
    }

    private static final int MAX_ENTRIES = 1_000;

    private final AuthenticationProvider delegate;
    private final long ttlNanos;
    private final byte[] salt = new byte[16];
    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    public CachingAuthenticationProvider(AuthenticationProvider delegate, Duration ttl) {
        this.delegate = delegate;
        this.ttlNanos = ttl.toNanos();
        new SecureRandom().nextBytes(salt);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String key = key(authentication);
        long now = System.nanoTime();
        if (key != null) {
            Entry hit = cache.get(key);
            if (hit != null && now - hit.expiresAtNanos() < 0) {
                return copy(hit.result(), authentication);
            }
            cache.remove(key);
        }
        Authentication result = delegate.authenticate(authentication);
        if (key != null && result != null && result.isAuthenticated()) {
            if (cache.size() >= MAX_ENTRIES) {
                cache.entrySet().removeIf(e -> now - e.getValue().expiresAtNanos() >= 0);
                if (cache.size() >= MAX_ENTRIES) {
                    cache.clear();
                }
            }
            cache.put(key, new Entry(result, now + ttlNanos));
        }
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }

    private static Authentication copy(Authentication cached, Authentication request) {
        var copy = UsernamePasswordAuthenticationToken.authenticated(
                cached.getPrincipal(), null, cached.getAuthorities());
        copy.setDetails(request.getDetails());
        return copy;
    }

    private String key(Authentication authentication) {
        if (authentication.getName() == null || !(authentication.getCredentials() instanceof String password)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            digest.update(authentication.getName().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
