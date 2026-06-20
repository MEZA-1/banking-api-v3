package cm.edu.banking.controller;

import cm.edu.banking.dto.request.CreateBanqueRequest;
import cm.edu.banking.dto.request.RetraitAdminRequest;
import cm.edu.banking.dto.response.BanqueResponse;
import cm.edu.banking.dto.response.OperationResponse;
import cm.edu.banking.exception.BankingException;
import cm.edu.banking.exception.ResourceNotFoundException;
import cm.edu.banking.model.enums.StatutBanque;
import cm.edu.banking.model.enums.TypeOperation;
import cm.edu.banking.security.CustomUserDetailsService;
import cm.edu.banking.security.filter.JwtAuthenticationFilter;
import cm.edu.banking.security.jwt.JwtTokenProvider;
import cm.edu.banking.service.AdminService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration de la couche REST {@link AdminController} via MockMvc.
 *
 * <p>Vérifie les contrôles d'accès par rôle, les codes HTTP et la
 * structure JSON des réponses pour les endpoints administrateur.</p>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@WebMvcTest(
        controllers = AdminController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@DisplayName("AdminController — Tests REST (MockMvc)")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private BanqueResponse banqueResponseFixture() {
        return BanqueResponse.builder()
                .id(1L).nom("Banque Test")
                .statut(StatutBanque.ACTIVE)
                .montantInitial(new BigDecimal("5000000"))
                .montantActif(new BigDecimal("4500000"))
                .montantLigne(BigDecimal.ZERO)
                .dateCreation(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    //  POST /api/admin/banques
    // =========================================================================

    @Nested
    @DisplayName("POST /api/admin/banques — Créer une banque")
    class CreerBanqueTests {

        @Test
        @DisplayName("✅ 201 Created — banque créée par ADMIN")
        @WithMockUser(roles = "ADMIN")
        void creerBanque_admin_returns201() throws Exception {
            // GIVEN
            CreateBanqueRequest request = new CreateBanqueRequest(
                    "Banque Test", new BigDecimal("5000000"));
            when(adminService.creerBanque(any())).thenReturn(banqueResponseFixture());

            // WHEN / THEN
            mockMvc.perform(post("/api/admin/banques")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nom").value("Banque Test"))
                    .andExpect(jsonPath("$.statut").value("ACTIVE"))
                    .andExpect(jsonPath("$.montantInitial").value(5000000));
        }

        @Test
        @DisplayName("❌ 403 Forbidden — accès refusé pour rôle CLIENT")
        @WithMockUser(roles = "CLIENT")
        void creerBanque_client_returns403() throws Exception {
            mockMvc.perform(post("/api/admin/banques")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nom":"Test","montantInitial":5000000}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("❌ 400 Bad Request — montantInitial < 2 000 000 FCFA")
        @WithMockUser(roles = "ADMIN")
        void creerBanque_montantInsuffisant_returns400() throws Exception {
            // GIVEN — montant < seuil minimal
            CreateBanqueRequest request = new CreateBanqueRequest(
                    "Petite Banque", new BigDecimal("500000"));

            // WHEN / THEN
            mockMvc.perform(post("/api/admin/banques")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.montantInitial").exists());
        }

        @Test
        @DisplayName("❌ 400 Bad Request — nom absent")
        @WithMockUser(roles = "ADMIN")
        void creerBanque_nomAbsent_returns400() throws Exception {
            mockMvc.perform(post("/api/admin/banques")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"montantInitial":5000000}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.nom").exists());
        }
    }

    // =========================================================================
    //  GET /api/admin/banques
    // =========================================================================

    @Nested
    @DisplayName("GET /api/admin/banques — Lister les banques")
    class ListerBanquesTests {

        @Test
        @DisplayName("✅ 200 OK — liste retournée pour ADMIN")
        @WithMockUser(roles = "ADMIN")
        void listerBanques_admin_returns200() throws Exception {
            // GIVEN
            when(adminService.listerBanques())
                    .thenReturn(List.of(banqueResponseFixture()));

            // WHEN / THEN
            mockMvc.perform(get("/api/admin/banques"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].nom").value("Banque Test"));
        }

        @Test
        @DisplayName("❌ 403 Forbidden — accès refusé pour rôle AGENT")
        @WithMockUser(roles = "AGENT")
        void listerBanques_agent_returns403() throws Exception {
            mockMvc.perform(get("/api/admin/banques"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    //  PATCH /api/admin/banques/{id}/suspendre
    // =========================================================================

    @Nested
    @DisplayName("PATCH /api/admin/banques/{id}/suspendre — Suspension")
    class SuspensionBanqueTests {

        @Test
        @DisplayName("✅ 200 OK — banque suspendue")
        @WithMockUser(roles = "ADMIN")
        void suspendre_success_returns200() throws Exception {
            // GIVEN
            BanqueResponse suspended = banqueResponseFixture();
            suspended.setStatut(StatutBanque.SUSPENDUE);
            when(adminService.suspendrebanque(1L)).thenReturn(suspended);

            // WHEN / THEN
            mockMvc.perform(patch("/api/admin/banques/1/suspendre").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("SUSPENDUE"));
        }

        @Test
        @DisplayName("❌ 400 Bad Request — banque déjà suspendue")
        @WithMockUser(roles = "ADMIN")
        void suspendre_dejasSuspendue_returns400() throws Exception {
            // GIVEN
            when(adminService.suspendrebanque(1L))
                    .thenThrow(new BankingException("La banque est déjà suspendue."));

            // WHEN / THEN
            mockMvc.perform(patch("/api/admin/banques/1/suspendre").with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("La banque est déjà suspendue."));
        }

        @Test
        @DisplayName("❌ 404 Not Found — banque introuvable")
        @WithMockUser(roles = "ADMIN")
        void suspendre_banqueIntrouvable_returns404() throws Exception {
            // GIVEN
            when(adminService.suspendrebanque(99L))
                    .thenThrow(new ResourceNotFoundException("Banque", "id", 99L));

            // WHEN / THEN
            mockMvc.perform(patch("/api/admin/banques/99/suspendre").with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    //  POST /api/admin/banques/{id}/retrait
    // =========================================================================

    @Nested
    @DisplayName("POST /api/admin/banques/{id}/retrait — Retrait admin")
    class RetraitAdminTests {

        @Test
        @DisplayName("✅ 200 OK — retrait effectué et opération retournée")
        @WithMockUser(roles = "ADMIN")
        void retraitAdmin_success_returns200() throws Exception {
            // GIVEN
            RetraitAdminRequest request = new RetraitAdminRequest(
                    new BigDecimal("500000"), "Retrait test");
            OperationResponse opResponse = OperationResponse.builder()
                    .id(1L)
                    .type(TypeOperation.RETRAIT_ADMIN)
                    .montant(new BigDecimal("500000"))
                    .frais(BigDecimal.ZERO)
                    .montantTotal(new BigDecimal("500000"))
                    .build();

            when(adminService.retraitAdmin(eq(1L), any())).thenReturn(opResponse);

            // WHEN / THEN
            mockMvc.perform(post("/api/admin/banques/1/retrait")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("RETRAIT_ADMIN"))
                    .andExpect(jsonPath("$.montant").value(500000))
                    .andExpect(jsonPath("$.frais").value(0));
        }

        @Test
        @DisplayName("❌ 400 Bad Request — montant > 1 000 000 FCFA (plafond dépassé via DTO)")
        @WithMockUser(roles = "ADMIN")
        void retraitAdmin_depassePlafond_returns400() throws Exception {
            // GIVEN — @DecimalMax sur le DTO bloque avant le service
            RetraitAdminRequest request = new RetraitAdminRequest(
                    new BigDecimal("1500000"), null);

            // WHEN / THEN
            mockMvc.perform(post("/api/admin/banques/1/retrait")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.montant").exists());
        }

        @Test
        @DisplayName("❌ 400 Bad Request — montantActif insuffisant (règle service)")
        @WithMockUser(roles = "ADMIN")
        void retraitAdmin_montantActifInsuffisant_returns400() throws Exception {
            // GIVEN
            when(adminService.retraitAdmin(eq(1L), any()))
                    .thenThrow(new BankingException("Retrait impossible : montant actif insuffisant."));

            RetraitAdminRequest request = new RetraitAdminRequest(
                    new BigDecimal("1000000"), null);

            // WHEN / THEN
            mockMvc.perform(post("/api/admin/banques/1/retrait")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Retrait impossible : montant actif insuffisant."));
        }
    }
}
