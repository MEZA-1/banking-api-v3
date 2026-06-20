package cm.edu.banking.security;

import cm.edu.banking.security.jwt.JwtProperties;
import cm.edu.banking.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du {@link JwtTokenProvider}.
 *
 * <p>Vérifie la génération, la validation et l'extraction de l'email
 * depuis les tokens JWT. Utilise une clé secrète de test en Base64.</p>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider — Tests unitaires")
class JwtTokenProviderTest {

    /** Clé Base64 de 256 bits valide pour HS256 (test uniquement). */
    private static final String TEST_SECRET =
            "dGVzdFNlY3JldEtleUZvckp3dFRva2VuUHJvdmlkZXJVbml0VGVzdA==";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 heure

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setupProperties() {
        when(jwtProperties.getSecret()).thenReturn(TEST_SECRET);
        when(jwtProperties.getExpiration()).thenReturn(EXPIRATION_MS);
    }

    // =========================================================================
    //  Génération de token
    // =========================================================================

    @Nested
    @DisplayName("Génération de token")
    class GenerationTests {

        @Test
        @DisplayName("✅ generateTokenFromEmail retourne un token non vide")
        void generateTokenFromEmail_retourneTokenNonVide() {
            // WHEN
            String token = jwtTokenProvider.generateTokenFromEmail("test@banking.cm");

            // THEN
            assertThat(token).isNotBlank();
            // Format JWT : 3 parties séparées par des points
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("✅ generateToken(Authentication) retourne un token valide")
        void generateToken_avecAuthentication_retourneTokenValide() {
            // GIVEN
            UserDetails userDetails = new User(
                    "agent@banking.cm", "password",
                    List.of(new SimpleGrantedAuthority("ROLE_AGENT")));
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            // WHEN
            String token = jwtTokenProvider.generateToken(authentication);

            // THEN
            assertThat(token).isNotBlank();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }
    }

    // =========================================================================
    //  Extraction de l'email
    // =========================================================================

    @Nested
    @DisplayName("Extraction de l'email")
    class ExtractionTests {

        @Test
        @DisplayName("✅ getEmailFromToken retourne l'email correct")
        void getEmailFromToken_retourneEmailCorrect() {
            // GIVEN
            String email = "alice@banking.cm";
            String token = jwtTokenProvider.generateTokenFromEmail(email);

            // WHEN
            String emailExtrait = jwtTokenProvider.getEmailFromToken(token);

            // THEN
            assertThat(emailExtrait).isEqualTo(email);
        }

        @Test
        @DisplayName("✅ Email extrait via generateToken(Authentication) est correct")
        void getEmailFromToken_viaAuthentication_correct() {
            // GIVEN
            String email = "superviseur@banking.cm";
            UserDetails userDetails = new User(
                    email, "pass",
                    List.of(new SimpleGrantedAuthority("ROLE_SUPERVISEUR")));
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            String token = jwtTokenProvider.generateToken(auth);

            // WHEN / THEN
            assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(email);
        }
    }

    // =========================================================================
    //  Validation de token
    // =========================================================================

    @Nested
    @DisplayName("Validation de token")
    class ValidationTests {

        @Test
        @DisplayName("✅ Token valide retourne true")
        void validateToken_tokenValide_retourneTrue() {
            // GIVEN
            String token = jwtTokenProvider.generateTokenFromEmail("user@banking.cm");

            // WHEN / THEN
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("❌ Token malformé retourne false")
        void validateToken_tokenMalforme_retourneFalse() {
            // WHEN / THEN
            assertThat(jwtTokenProvider.validateToken("token.invalide.xyz")).isFalse();
        }

        @Test
        @DisplayName("❌ Token vide retourne false")
        void validateToken_tokenVide_retourneFalse() {
            assertThat(jwtTokenProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("❌ Token nul retourne false")
        void validateToken_tokenNul_retourneFalse() {
            assertThat(jwtTokenProvider.validateToken(null)).isFalse();
        }

        @Test
        @DisplayName("❌ Token expiré retourne false")
        void validateToken_tokenExpire_retourneFalse() {
            // GIVEN — expiration à 0 ms → token immédiatement expiré
            when(jwtProperties.getExpiration()).thenReturn(0L);
            String tokenExpire =
                    jwtTokenProvider.generateTokenFromEmail("expired@banking.cm");

            // Remettre une vraie expiration pour ne pas biaiser validateToken
            when(jwtProperties.getExpiration()).thenReturn(EXPIRATION_MS);

            // WHEN / THEN
            assertThat(jwtTokenProvider.validateToken(tokenExpire)).isFalse();
        }

        @Test
        @DisplayName("❌ Token signé avec une autre clé retourne false")
        void validateToken_mauvaiseCle_retourneFalse() {
            // GIVEN — token généré avec une AUTRE clé Base64
            String autreSecret =
                    "YXV0cmVDbGVTZWNyZXRlUG91ckxlc1Rlc3RzVW5pdGFpcmVzQXBpQmFua2luZw==";
            JwtProperties autresProps = mock(JwtProperties.class);
            when(autresProps.getSecret()).thenReturn(autreSecret);
            when(autresProps.getExpiration()).thenReturn(EXPIRATION_MS);
            JwtTokenProvider autreProvider = new JwtTokenProvider(autresProps);
            String tokenAutreCle =
                    autreProvider.generateTokenFromEmail("hacker@evil.com");

            // WHEN — tentative de validation avec notre provider (bonne clé)
            assertThat(jwtTokenProvider.validateToken(tokenAutreCle)).isFalse();
        }
    }
}
