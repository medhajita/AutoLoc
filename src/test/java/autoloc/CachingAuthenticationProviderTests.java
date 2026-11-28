package autoloc;

import autoloc.config.CachingAuthenticationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachingAuthenticationProviderTests {

    private final AtomicInteger delegateCalls = new AtomicInteger();

    private final AuthenticationProvider delegate = new AuthenticationProvider() {
        @Override
        public Authentication authenticate(Authentication a) {
            delegateCalls.incrementAndGet();
            if (!"secret".equals(a.getCredentials())) {
                throw new BadCredentialsException("bad");
            }
            return UsernamePasswordAuthenticationToken.authenticated(
                    a.getName(), null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        }

        @Override
        public boolean supports(Class<?> c) {
            return UsernamePasswordAuthenticationToken.class.isAssignableFrom(c);
        }
    };

    private static Authentication request(String user, String pass) {
        return UsernamePasswordAuthenticationToken.unauthenticated(user, pass);
    }

    @Test
    void successfulAuthenticationIsServedFromCacheWithoutCallingDelegate() {
        var provider = new CachingAuthenticationProvider(delegate, Duration.ofMinutes(1));
        provider.authenticate(request("bob", "secret"));
        Authentication second = provider.authenticate(request("bob", "secret"));

        assertEquals(1, delegateCalls.get());
        assertTrue(second.isAuthenticated());
        assertEquals("bob", second.getName());
    }

    @Test
    void wrongPasswordIsNeverServedFromCache() {
        var provider = new CachingAuthenticationProvider(delegate, Duration.ofMinutes(1));
        provider.authenticate(request("bob", "secret"));

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(request("bob", "wrong")));
        assertEquals(2, delegateCalls.get());
    }

    @Test
    void expiredEntryIsCheckedAgainstDelegate() {
        var provider = new CachingAuthenticationProvider(delegate, Duration.ZERO);
        provider.authenticate(request("bob", "secret"));
        provider.authenticate(request("bob", "secret"));

        assertEquals(2, delegateCalls.get());
    }
}
