package cm.edu.banking.security;

import cm.edu.banking.dto.request.LoginRequest;
import cm.edu.banking.dto.request.RegisterRequest;
import cm.edu.banking.dto.response.AuthResponse;
import cm.edu.banking.exception.DuplicateResourceException;
import cm.edu.banking.model.User;
import cm.edu.banking.model.enums.Role;
import cm.edu.banking.repository.UserRepository;
import cm.edu.banking.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du {@link AuthService}.
 *
 * <p>Couverture :</p>
 * <ul>
 *   <li>Skill 2.1 : initialisation Super Admin</li>
 *   <li>Skill 2.2 : inscription client, connexion JWT</li>
 * </ul>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Tests unitaires")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void injectProperties() {
        ReflectionTestUtils.setField(authService, "superAdminEmail", "mezatiogeril@gmail.com");
        ReflectionTestUtils.setField(authService, "superAdminPassword", "Admin123");
    }

    // =========================================================================
    //  Skill 2.1 — Initialisation Super Admin
    // =========================================================================

    @Nested
    @DisplayName("Skill 2.1 — Initialisation Super Admin")
    class SuperAdminInitTests {

        @Test
        @DisplayName("✅ Crée le Super Admin si aucun ADMIN n'existe")
        void initSuperAdmin_aucunAdminExistant_creeSuperAdmin() {
            // GIVEN
            when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
            when(passwordEncoder.encode("Admin123")).thenReturn("$2a$10$BCRYPT_HASH");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // WHEN
            authService.initializeSuperAdmin();

            // THEN
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();

            assertThat(saved.getEmail()).isEqualTo("mezatiogeril@gmail.com");
            assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
            assertThat(saved.getMotDePasse()).isEqualTo("$2a$10$BCRYPT_HASH");
            assertThat(saved.getNom()).isEqualTo("Super");
            assertThat(saved.getPrenom()).isEqualTo("Admin");
        }

        @Test
        @DisplayName("✅ Ne crée PAS de Super Admin si un ADMIN existe déjà")
        void initSuperAdmin_adminDejaExistant_neRienFait() {
            // GIVEN
            when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

            // WHEN
            authService.initializeSuperAdmin();

            // THEN — aucun save() ne doit avoir été appelé
            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    //  Skill 2.2 — Inscription
    // =========================================================================

    @Nested
    @DisplayName("Skill 2.2 — Inscription d'un nouveau client")
    class RegisterTests {

        @Test
        @DisplayName("✅ Inscrit un client avec rôle CLIENT et hash BCrypt")
        void register_success_roleClientEtHashBcrypt() {
            // GIVEN
            RegisterRequest request = RegisterRequest.builder()
                    .nom("Kamga").prenom("Rose").email("rose@client.cm")
                    .telephone("677123456").motDePasse("password123").build();

            when(userRepository.existsByEmail("rose@client.cm")).thenReturn(false);
            when(userRepository.existsByTelephone("677123456")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$HASH");
            User savedUser = User.builder()
                    .id(1L).nom("Kamga").prenom("Rose")
                    .email("rose@client.cm").role(Role.CLIENT)
                    .motDePasse("$2a$10$HASH").build();
            when(userRepository.save(any())).thenReturn(savedUser);
            when(jwtTokenProvider.generateTokenFromEmail("rose@client.cm"))
                    .thenReturn("eyJhbGciOiJIUzI1NiJ9.TOKEN");

            // WHEN
            AuthResponse response = authService.register(request);

            // THEN
            assertThat(response.getAccessToken()).isEqualTo("eyJhbGciOiJIUzI1NiJ9.TOKEN");
            assertThat(response.getRole()).isEqualTo(Role.CLIENT);
            assertThat(response.getEmail()).isEqualTo("rose@client.cm");
            assertThat(response.getTokenType()).isEqualTo("Bearer");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getMotDePasse()).isEqualTo("$2a$10$HASH");
            assertThat(captor.getValue().getRole()).isEqualTo(Role.CLIENT);
        }

        @Test
        @DisplayName("❌ Lève DuplicateResourceException si email déjà utilisé")
        void register_emailDuplique_throwsException() {
            // GIVEN
            when(userRepository.existsByEmail("rose@client.cm")).thenReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> authService.register(RegisterRequest.builder()
                    .email("rose@client.cm").telephone("677999999").build()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("❌ Lève DuplicateResourceException si téléphone déjà utilisé")
        void register_telephoneDuplique_throwsException() {
            // GIVEN
            when(userRepository.existsByEmail("nouveau@client.cm")).thenReturn(false);
            when(userRepository.existsByTelephone("677123456")).thenReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> authService.register(RegisterRequest.builder()
                    .email("nouveau@client.cm").telephone("677123456").build()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("téléphone");
        }

        @Test
        @DisplayName("✅ Le token JWT est généré directement (sans second appel /login)")
        void register_tokenRetourneImmediatement() {
            // GIVEN
            RegisterRequest request = RegisterRequest.builder()
                    .nom("A").prenom("B").email("ab@c.cm")
                    .telephone("600000001").motDePasse("pass").build();
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByTelephone(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hash");
            User savedUser = User.builder().id(2L).email("ab@c.cm")
                    .role(Role.CLIENT).nom("A").prenom("B").build();
            when(userRepository.save(any())).thenReturn(savedUser);
            when(jwtTokenProvider.generateTokenFromEmail("ab@c.cm"))
                    .thenReturn("TOKEN_JWT");

            // WHEN
            AuthResponse response = authService.register(request);

            // THEN — generateTokenFromEmail appelé, pas generateToken(Authentication)
            verify(jwtTokenProvider).generateTokenFromEmail("ab@c.cm");
            verify(jwtTokenProvider, never()).generateToken(any());
            assertThat(response.getAccessToken()).isEqualTo("TOKEN_JWT");
        }
    }

    // =========================================================================
    //  Skill 2.2 — Connexion
    // =========================================================================

    @Nested
    @DisplayName("Skill 2.2 — Connexion et génération JWT")
    class LoginTests {

        @Test
        @DisplayName("✅ Connexion réussie — token JWT retourné")
        void login_success_tokenRetourne() {
            // GIVEN
            LoginRequest request = new LoginRequest("rose@client.cm", "password123");

            Authentication auth = mock(Authentication.class);
            when(authenticationManager.authenticate(
                    any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(auth);

            User user = User.builder()
                    .id(1L).nom("Kamga").prenom("Rose")
                    .email("rose@client.cm").role(Role.CLIENT).build();
            when(userRepository.findByEmail("rose@client.cm"))
                    .thenReturn(Optional.of(user));
            when(jwtTokenProvider.generateToken(auth)).thenReturn("JWT_TOKEN");

            // WHEN
            AuthResponse response = authService.login(request);

            // THEN
            assertThat(response.getAccessToken()).isEqualTo("JWT_TOKEN");
            assertThat(response.getEmail()).isEqualTo("rose@client.cm");
            assertThat(response.getRole()).isEqualTo(Role.CLIENT);
            assertThat(response.getNomComplet()).isEqualTo("Rose Kamga");
        }

        @Test
        @DisplayName("❌ Lève BadCredentialsException si identifiants incorrects")
        void login_identifiantsIncorrects_throwsBadCredentials() {
            // GIVEN
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            // WHEN / THEN
            assertThatThrownBy(() -> authService.login(
                    new LoginRequest("wrong@email.cm", "wrongpass")))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }
}
