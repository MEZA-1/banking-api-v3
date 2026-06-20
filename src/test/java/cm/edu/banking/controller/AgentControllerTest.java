package cm.edu.banking.controller;

import cm.edu.banking.dto.request.DepotRequest;
import cm.edu.banking.dto.request.RetraitRequest;
import cm.edu.banking.dto.response.CompteBancaireResponse;
import cm.edu.banking.dto.response.OperationResponse;
import cm.edu.banking.exception.BankingException;
import cm.edu.banking.model.User;
import cm.edu.banking.model.enums.Role;
import cm.edu.banking.model.enums.StatutCompte;
import cm.edu.banking.model.enums.TypeOperation;
import cm.edu.banking.security.CustomUserDetailsService;
import cm.edu.banking.security.SecurityUtils;
import cm.edu.banking.security.filter.JwtAuthenticationFilter;
import cm.edu.banking.security.jwt.JwtTokenProvider;
import cm.edu.banking.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration de la couche REST {@link AgentController} via MockMvc.
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@WebMvcTest(
        controllers = AgentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@DisplayName("AgentController — Tests REST (MockMvc)")
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private User agentConnecte;

    @BeforeEach
    void setUp() {
        agentConnecte = User.builder()
                .id(10L).nom("Doe").prenom("John")
                .email("agent@bank.cm").role(Role.AGENT).build();
        when(securityUtils.getUtilisateurConnecte()).thenReturn(agentConnecte);
    }

    // =========================================================================
    //  GET /api/agent/mon-compte
    // =========================================================================

    @Test
    @DisplayName("✅ GET /api/agent/mon-compte — 200 OK avec solde agent")
    @WithMockUser(roles = "AGENT")
    void getMonCompte_success_returns200() throws Exception {
        // GIVEN
        CompteBancaireResponse response = CompteBancaireResponse.builder()
                .id(1L).numeroCompte("BK00001-20240315-010")
                .solde(new BigDecimal("500000")).statut(StatutCompte.ACTIF)
                .build();
        when(transactionService.getMonCompte(any(User.class))).thenReturn(response);

        // WHEN / THEN
        mockMvc.perform(get("/api/agent/mon-compte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solde").value(500000))
                .andExpect(jsonPath("$.statut").value("ACTIF"));
    }

    @Test
    @DisplayName("❌ GET /api/agent/mon-compte — 403 Forbidden pour rôle CLIENT")
    @WithMockUser(roles = "CLIENT")
    void getMonCompte_client_returns403() throws Exception {
        mockMvc.perform(get("/api/agent/mon-compte"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    //  POST /api/agent/depot
    // =========================================================================

    @Nested
    @DisplayName("POST /api/agent/depot — Dépôt (frais 0 %)")
    class DepotTests {

        @Test
        @DisplayName("✅ 200 OK — dépôt effectué, frais = 0")
        @WithMockUser(roles = "AGENT")
        void depot_success_returns200() throws Exception {
            // GIVEN
            DepotRequest request = new DepotRequest(20L, new BigDecimal("50000"), null);
            OperationResponse response = OperationResponse.builder()
                    .id(1L).type(TypeOperation.DEPOT)
                    .montant(new BigDecimal("50000"))
                    .frais(BigDecimal.ZERO)
                    .montantTotal(new BigDecimal("50000"))
                    .build();
            when(transactionService.effectuerDepot(any(User.class), any()))
                    .thenReturn(response);

            // WHEN / THEN
            mockMvc.perform(post("/api/agent/depot")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("DEPOT"))
                    .andExpect(jsonPath("$.frais").value(0))
                    .andExpect(jsonPath("$.montantTotal").value(50000));
        }

        @Test
        @DisplayName("❌ 400 Bad Request — solde agent insuffisant")
        @WithMockUser(roles = "AGENT")
        void depot_soldeAgentInsuffisant_returns400() throws Exception {
            // GIVEN
            when(transactionService.effectuerDepot(any(), any()))
                    .thenThrow(new BankingException(
                            "Dépôt impossible : solde agent insuffisant."));

            DepotRequest request = new DepotRequest(
                    20L, new BigDecimal("999999999"), null);

            // WHEN / THEN
            mockMvc.perform(post("/api/agent/depot")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Dépôt impossible : solde agent insuffisant."));
        }

        @Test
        @DisplayName("❌ 400 Bad Request — clientId absent")
        @WithMockUser(roles = "AGENT")
        void depot_clientIdAbsent_returns400() throws Exception {
            mockMvc.perform(post("/api/agent/depot")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"montant": 50000}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.clientId").exists());
        }
    }

    // =========================================================================
    //  POST /api/agent/retrait
    // =========================================================================

    @Nested
    @DisplayName("POST /api/agent/retrait — Retrait (frais 2 %)")
    class RetraitTests {

        @Test
        @DisplayName("✅ 200 OK — retrait effectué, frais = 2 % du montant")
        @WithMockUser(roles = "AGENT")
        void retrait_success_frais2Pourcent_returns200() throws Exception {
            // GIVEN
            RetraitRequest request = new RetraitRequest(
                    20L, new BigDecimal("10000"), null);
            OperationResponse response = OperationResponse.builder()
                    .id(2L).type(TypeOperation.RETRAIT)
                    .montant(new BigDecimal("10000"))
                    .frais(new BigDecimal("200.00"))
                    .montantTotal(new BigDecimal("10200.00"))
                    .build();
            when(transactionService.effectuerRetrait(any(User.class), any()))
                    .thenReturn(response);

            // WHEN / THEN
            mockMvc.perform(post("/api/agent/retrait")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("RETRAIT"))
                    .andExpect(jsonPath("$.frais").value(200.0))
                    .andExpect(jsonPath("$.montantTotal").value(10200.0));
        }

        @Test
        @DisplayName("❌ 400 Bad Request — compte client suspendu (Skill 15)")
        @WithMockUser(roles = "AGENT")
        void retrait_compteClientSuspendu_returns400() throws Exception {
            // GIVEN
            when(transactionService.effectuerRetrait(any(), any()))
                    .thenThrow(new BankingException(
                            "Opération bloquée : le compte client est suspendu (Skill 15)."));

            RetraitRequest request = new RetraitRequest(
                    20L, new BigDecimal("5000"), null);

            // WHEN / THEN
            mockMvc.perform(post("/api/agent/retrait")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "Opération bloquée : le compte client est suspendu (Skill 15)."));
        }

        @Test
        @DisplayName("❌ 400 Bad Request — montant = 0 (validation DTO)")
        @WithMockUser(roles = "AGENT")
        void retrait_montantNul_returns400() throws Exception {
            mockMvc.perform(post("/api/agent/retrait")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"clientId": 20, "montant": 0}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.montant").exists());
        }
    }
}
