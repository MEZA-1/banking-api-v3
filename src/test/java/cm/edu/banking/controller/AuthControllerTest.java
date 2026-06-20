package cm.edu.banking.controller;

import cm.edu.banking.dto.request.LoginRequest;
import cm.edu.banking.dto.request.RegisterRequest;
import cm.edu.banking.dto.response.AuthResponse;
import cm.edu.banking.exception.DuplicateResourceException;
import cm.edu.banking.model.enums.Role;
import cm.edu.banking.security.AuthService;
import cm.edu.banking.security.CustomUserDetailsService;
import cm.edu.banking.security.filter.JwtAuthenticationFilter;
import cm.edu.banking.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration de la couche REST {@link AuthController} via MockMvc.
 *
 * <p>Vérifie les codes HTTP, la structure JSON des réponses et la gestion
 * des erreurs pour les endpoints publics d'authentification.</p>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@DisplayName("AuthController — Tests REST (MockMvc)")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    // =========================================================================
    //  POST /api/auth/register
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterEndpointTests {

        @Test
        @DisplayName("✅ 201 Created — inscription réussie avec token retourné")
        @WithMockUser
        void register_success_returns201() throws Exception {
            // GIVEN
            RegisterRequest request = RegisterRequest.builder()
                    .nom("Kamga").prenom("Rose").email("rose@client.cm")
                    .telephone("677123456").motDePasse("password123").build();

            AuthResponse response = AuthResponse.builder()
                    .accessToken("eyJhbGciOiJIUzI1NiJ9.TOKEN")
                    .tokenType("Bearer")
                    .userId(1L)
                    .nomComplet("Rose Kamga")
                    .email("rose@client.cm")
                    .role(Role.CLIENT)
                    .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            // WHEN / THEN
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.accessToken").value("eyJhbGciOiJIUzI1NiJ9.TOKEN"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.role").value("CLIENT"))
                    .andExpect(jsonPath("$.email").value("rose@client.cm"))
                    .andExpect(jsonPath("$.nomComplet").value("Rose Kamga"));
        }

        @Test
        @DisplayName("❌ 409 Conflict — email déjà utilisé")
        @WithMockUser
        void register_emailDuplique_returns409() throws Exception {
            // GIVEN
            RegisterRequest request = RegisterRequest.builder()
                    .nom("A").prenom("B").email("existing@client.cm")
                    .telephone("677000001").motDePasse("pass123").build();

            when(authService.register(any()))
                    .thenThrow(new DuplicateResourceException(
                            "Utilisateur", "email", "existing@client.cm"));

            // WHEN / THEN
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("❌ 400 Bad Request — email invalide (Bean Validation)")
        @WithMockUser
        void register_emailInvalide_returns400() throws Exception {
            // GIVEN — email mal formé
            RegisterRequest request = RegisterRequest.builder()
                    .nom("A").prenom("B").email("NOT_AN_EMAIL")
                    .telephone("677000001").motDePasse("pass123").build();

            // WHEN / THEN
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.email").exists());
        }

        @Test
        @DisplayName("❌ 400 Bad Request — champs obligatoires manquants")
        @WithMockUser
        void register_champObligatoireManquant_returns400() throws Exception {
            // GIVEN — nom absent
            String bodyIncomplet = """
                    {"prenom":"A","email":"a@b.cm","telephone":"000","motDePasse":"pass"}
                    """;

            // WHEN / THEN
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyIncomplet))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.nom").exists());
        }

        @Test
        @DisplayName("❌ 400 Bad Request — mot de passe trop court (< 6 caractères)")
        @WithMockUser
        void register_motDePasseTropCourt_returns400() throws Exception {
            // GIVEN
            RegisterRequest request = RegisterRequest.builder()
                    .nom("A").prenom("B").email("a@b.cm")
                    .telephone("677000001").motDePasse("abc").build(); // < 6 chars

            // WHEN / THEN
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.motDePasse").exists());
        }
    }

    // =========================================================================
    //  POST /api/auth/login
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("✅ 200 OK — connexion réussie avec token JWT")
        @WithMockUser
        void login_success_returns200() throws Exception {
            // GIVEN
            LoginRequest request = new LoginRequest("rose@client.cm", "password123");

            AuthResponse response = AuthResponse.builder()
                    .accessToken("eyJhbGciOiJIUzI1NiJ9.VALID_TOKEN")
                    .tokenType("Bearer")
                    .userId(1L)
                    .nomComplet("Rose Kamga")
                    .email("rose@client.cm")
                    .role(Role.CLIENT)
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            // WHEN / THEN
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.role").value("CLIENT"))
                    .andExpect(jsonPath("$.email").value("rose@client.cm"));
        }

        @Test
        @DisplayName("❌ 401 Unauthorized — identifiants incorrects")
        @WithMockUser
        void login_identifiantsIncorrects_returns401() throws Exception {
            // GIVEN
            when(authService.login(any()))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            LoginRequest request = new LoginRequest("wrong@email.cm", "wrongpass");

            // WHEN / THEN
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("❌ 400 Bad Request — email manquant dans la requête")
        @WithMockUser
        void login_emailManquant_returns400() throws Exception {
            // GIVEN — email absent
            String body = """
                    {"motDePasse":"password123"}
                    """;

            // WHEN / THEN
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.email").exists());
        }
    }
}
