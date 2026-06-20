package cm.edu.banking.controller;

import cm.edu.banking.dto.request.CreateCompteBancaireRequest;
import cm.edu.banking.dto.request.TransfertRequest;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration de la couche REST {@link ClientController} via MockMvc.
 *
 * <p>Vérifie les codes HTTP, la structure JSON et les contrôles d'accès
 * pour les endpoints client (compte, transferts, historique).</p>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@WebMvcTest(
        controllers = ClientController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@DisplayName("ClientController — Tests REST (MockMvc)")
class ClientControllerTest {

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

    private User clientConnecte;

    @BeforeEach
    void setUp() {
        clientConnecte = User.builder()
                .id(1L).nom("Kamga").prenom("Rose")
                .email("rose@client.cm").role(Role.CLIENT).build();
        when(securityUtils.getUtilisateurConnecte()).thenReturn(clientConnecte);
    }

    // =========================================================================
    //  POST /api/client/compte
    // =========================================================================

    @Nested
    @DisplayName("POST /api/client/compte — Création de compte")
    class CreerCompteTests {

        @Test
        @DisplayName("✅ 201 Created — compte créé avec solde 0")
        @WithMockUser(roles = "CLIENT")
        void creerCompte_success_returns201() throws Exception {
            // GIVEN
            CompteBancaireResponse response = CompteBancaireResponse.builder()
                    .id(1L).numeroCompte("BK00001-20240315-001")
                    .solde(BigDecimal.ZERO).statut(StatutCompte.ACTIF)
                    .banqueId(1L).nomBanque("Banque Test")
                    .dateCreation(LocalDateTime.now())
                    .build();

            when(transactionService.creerCompteBancaire(
                    any(User.class), any(CreateCompteBancaireRequest.class)))
                    .thenReturn(response);

            // WHEN / THEN
            mockMvc.perform(post("/api/client/compte")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateCompteBancaireRequest(1L))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.numeroCompte").value("BK00001-20240315-001"))
                    .andExpect(jsonPath("$.solde").value(0))
                    .andExpect(jsonPath("$.statut").value("ACTIF"))
                    .andExpect(jsonPath("$.nomBanque").value("Banque Test"));
        }

        @Test
        @DisplayName("❌ 400 Bad Request — client possède déjà un compte")
        @WithMockUser(roles = "CLIENT")
        void creerCompte_dejaExistant_returns400() throws Exception {
            // GIVEN
            when(transactionService.creerCompteBancaire(any(), any()))
                    .thenThrow(new BankingException(
                            "Vous possédez déjà un compte bancaire."));

            // WHEN / THEN
            mockMvc.perform(post("/api/client/compte")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"banqueId": 1}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Vous possédez déjà un compte bancaire."));
        }

        @Test
        @DisplayName("❌ 400 Bad Request — banqueId absent")
        @WithMockUser(roles = "CLIENT")
        void creerCompte_banqueIdAbsent_returns400() throws Exception {
            mockMvc.perform(post("/api/client/compte")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.banqueId").exists());
        }

        @Test
        @DisplayName("❌ 403 Forbidden — accès refusé pour rôle SUPERVISEUR")
        @WithMockUser(roles = "SUPERVISEUR")
        void creerCompte_superviseur_returns403() throws Exception {
            mockMvc.perform(post("/api/client/compte")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"banqueId": 1}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    //  GET /api/client/compte
    // =========================================================================

    @Nested
    @DisplayName("GET /api/client/compte — Consulter le solde")
    class GetCompteTests {

        @Test
        @DisplayName("✅ 200 OK — solde retourné")
        @WithMockUser(roles = "CLIENT")
        void getCompte_success_returns200() throws Exception {
            // GIVEN
            CompteBancaireResponse response = CompteBancaireResponse.builder()
                    .id(1L).numeroCompte("BK00001-20240315-001")
                    .solde(new BigDecimal("150000")).statut(StatutCompte.ACTIF)
                    .build();

            when(transactionService.getMonCompte(any(User.class))).thenReturn(response);

            // WHEN / THEN
            mockMvc.perform(get("/api/client/compte"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.solde").value(150000))
                    .andExpect(jsonPath("$.statut").value("ACTIF"));
        }
    }

    // =========================================================================
    //  POST /api/client/transfert
    // =========================================================================

    @Nested
    @DisplayName("POST /api/client/transfert — Transfert de fonds")
    class TransfertTests {

        @Test
        @DisplayName("✅ 200 OK — transfert interne retourne TRANSFERT_INTERNE")
        @WithMockUser(roles = "CLIENT")
        void transfertInterne_success_returns200() throws Exception {
            // GIVEN
            TransfertRequest request = new TransfertRequest(
                    "BK00001-20240115-002", new BigDecimal("50000"), null);

            OperationResponse response = OperationResponse.builder()
                    .id(1L).type(TypeOperation.TRANSFERT_INTERNE)
                    .montant(new BigDecimal("50000"))
                    .frais(new BigDecimal("1000.00"))
                    .montantTotal(new BigDecimal("51000.00"))
                    .numeroCompteEmetteur("BK00001-20240115-001")
                    .numeroCompteDestinataire("BK00001-20240115-002")
                    .nomBanque("Banque Alpha")
                    .build();

            when(transactionService.effectuerTransfert(any(User.class), any()))
                    .thenReturn(response);

            // WHEN / THEN
            mockMvc.perform(post("/api/client/transfert")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("TRANSFERT_INTERNE"))
                    .andExpect(jsonPath("$.montant").value(50000))
                    .andExpect(jsonPath("$.frais").value(1000.0))
                    .andExpect(jsonPath("$.montantTotal").value(51000.0));
        }

        @Test
        @DisplayName("✅ 200 OK — transfert interbancaire retourne TRANSFERT_INTERBANCAIRE")
        @WithMockUser(roles = "CLIENT")
        void transfertInterbancaire_success_returns200() throws Exception {
            // GIVEN
            TransfertRequest request = new TransfertRequest(
                    "BK00002-20240115-003", new BigDecimal("50000"), null);

            OperationResponse response = OperationResponse.builder()
                    .id(2L).type(TypeOperation.TRANSFERT_INTERBANCAIRE)
                    .montant(new BigDecimal("50000"))
                    .frais(new BigDecimal("2000.00"))
                    .montantTotal(new BigDecimal("52000.00"))
                    .build();

            when(transactionService.effectuerTransfert(any(User.class), any()))
                    .thenReturn(response);

            // WHEN / THEN
            mockMvc.perform(post("/api/client/transfert")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("TRANSFERT_INTERBANCAIRE"))
                    .andExpect(jsonPath("$.frais").value(2000.0));
        }

        @Test
        @DisplayName("❌ 400 Bad Request — solde émetteur insuffisant")
        @WithMockUser(roles = "CLIENT")
        void transfert_soldeInsuffisant_returns400() throws Exception {
            // GIVEN
            when(transactionService.effectuerTransfert(any(), any()))
                    .thenThrow(new BankingException(
                            "Solde insuffisant pour ce transfert interne."));

            TransfertRequest request = new TransfertRequest(
                    "BK00001-20240115-002", new BigDecimal("9999999"), null);

            // WHEN / THEN
            mockMvc.perform(post("/api/client/transfert")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Solde insuffisant pour ce transfert interne."));
        }

        @Test
        @DisplayName("❌ 400 Bad Request — numéro de compte destinataire absent")
        @WithMockUser(roles = "CLIENT")
        void transfert_numeroCompteAbsent_returns400() throws Exception {
            mockMvc.perform(post("/api/client/transfert")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"montant": 50000}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(
                            "$.validationErrors.numeroCompteDestinataire").exists());
        }
    }

    // =========================================================================
    //  GET /api/client/historique
    // =========================================================================

    @Nested
    @DisplayName("GET /api/client/historique — Historique paginé")
    class HistoriqueTests {

        @Test
        @DisplayName("✅ 200 OK — page d'opérations retournée")
        @WithMockUser(roles = "CLIENT")
        void getHistorique_returns200() throws Exception {
            // GIVEN
            OperationResponse op = OperationResponse.builder()
                    .id(1L).type(TypeOperation.TRANSFERT_INTERNE)
                    .montant(new BigDecimal("50000"))
                    .frais(new BigDecimal("1000"))
                    .dateOperation(LocalDateTime.now())
                    .build();

            when(transactionService.getMonHistorique(any(User.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(op)));

            // WHEN / THEN
            mockMvc.perform(get("/api/client/historique")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].type")
                            .value("TRANSFERT_INTERNE"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }
}
