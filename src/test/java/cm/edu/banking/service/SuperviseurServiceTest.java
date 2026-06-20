package cm.edu.banking.service;

import cm.edu.banking.dto.request.ApprovisionnerAgentRequest;
import cm.edu.banking.dto.request.CreateAgentRequest;
import cm.edu.banking.dto.response.CompteBancaireResponse;
import cm.edu.banking.dto.response.OperationResponse;
import cm.edu.banking.dto.response.UserResponse;
import cm.edu.banking.exception.BankingException;
import cm.edu.banking.model.Banque;
import cm.edu.banking.model.CompteBancaire;
import cm.edu.banking.model.Operation;
import cm.edu.banking.model.User;
import cm.edu.banking.model.enums.*;
import cm.edu.banking.repository.BanqueRepository;
import cm.edu.banking.repository.CompteBancaireRepository;
import cm.edu.banking.repository.OperationRepository;
import cm.edu.banking.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du {@link SuperviseurService}.
 *
 * <p>Couverture :</p>
 * <ul>
 *   <li>Skill 5 : création d'agent, approvisionnement</li>
 *   <li>Skill 13 : recalcul montantLigne après approvisionnement</li>
 *   <li>Skill 15 : blocages suspension</li>
 * </ul>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SuperviseurService — Tests unitaires")
class SuperviseurServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BanqueRepository banqueRepository;
    @Mock private CompteBancaireRepository compteBancaireRepository;
    @Mock private OperationRepository operationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private BankingMapper mapper;

    @InjectMocks
    private SuperviseurService superviseurService;

    private Banque banque;
    private User superviseur;
    private User agentUser;
    private CompteBancaire compteAgent;

    @BeforeEach
    void setUp() {
        banque = Banque.builder()
                .id(1L).nom("Banque Test")
                .statut(StatutBanque.ACTIVE)
                .montantActif(new BigDecimal("2000000"))
                .montantLigne(BigDecimal.ZERO)
                .build();

        superviseur = User.builder()
                .id(1L).email("superviseur@banque.cm")
                .role(Role.SUPERVISEUR).banque(banque).build();

        agentUser = User.builder()
                .id(10L).nom("Nkomo").prenom("Jean")
                .email("jean@banque.cm").role(Role.AGENT).banque(banque).build();

        compteAgent = CompteBancaire.builder()
                .id(100L).numeroCompte("BK00001-20240115-010")
                .solde(new BigDecimal("100000"))
                .statut(StatutCompte.ACTIF)
                .utilisateur(agentUser).banque(banque)
                .dateCreation(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    //  Skill 5 — Création d'agent
    // =========================================================================

    @Nested
    @DisplayName("Skill 5 — Création d'agent")
    class CreationAgentTests {

        @Test
        @DisplayName("✅ Crée un agent et son compte bancaire (solde=0) dans la banque du superviseur")
        void creerAgent_success_compteCreeAutomatiquement() {
            // GIVEN
            CreateAgentRequest request = CreateAgentRequest.builder()
                    .nom("Fotso").prenom("Marc").email("marc@banque.cm")
                    .telephone("690000001").motDePasse("Pass123").build();

            when(userRepository.existsByEmail("marc@banque.cm")).thenReturn(false);
            when(userRepository.existsByTelephone("690000001")).thenReturn(false);
            when(passwordEncoder.encode("Pass123")).thenReturn("$2a$10$HASHED");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                return User.builder().id(11L).nom(u.getNom()).prenom(u.getPrenom())
                        .email(u.getEmail()).role(u.getRole()).banque(u.getBanque()).build();
            });
            when(compteBancaireRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.toUserResponse(any())).thenReturn(
                    UserResponse.builder().id(11L).role(Role.AGENT).build());

            // WHEN
            superviseurService.creerAgent(superviseur, request);

            // THEN — l'agent est sauvegardé avec le bon rôle
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.AGENT);
            assertThat(userCaptor.getValue().getBanque()).isEqualTo(banque);

            // Le compte bancaire de l'agent est créé avec solde = 0
            ArgumentCaptor<CompteBancaire> compteCaptor =
                    ArgumentCaptor.forClass(CompteBancaire.class);
            verify(compteBancaireRepository).save(compteCaptor.capture());
            assertThat(compteCaptor.getValue().getSolde())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(compteCaptor.getValue().getStatut()).isEqualTo(StatutCompte.ACTIF);
        }

        @Test
        @DisplayName("❌ Bloque si la banque du superviseur est suspendue")
        void creerAgent_banqueSuspendue_throwsBankingException() {
            // GIVEN
            banque.setStatut(StatutBanque.SUSPENDUE);

            // WHEN / THEN
            assertThatThrownBy(() -> superviseurService.creerAgent(
                    superviseur, new CreateAgentRequest(
                            "A", "B", "c@d.cm", "000", "pass")))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("suspendue");
        }
    }

    // =========================================================================
    //  Skill 5 — Approvisionnement agent (+ Skill 13)
    // =========================================================================

    @Nested
    @DisplayName("Skill 5 — Approvisionnement agent (+ recalcul montantLigne Skill 13)")
    class ApprovisionnerAgentTests {

        @BeforeEach
        void setupMock() {
            when(compteBancaireRepository.findByUtilisateurId(agentUser.getId()))
                    .thenReturn(Optional.of(compteAgent));
            when(compteBancaireRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(banqueRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("✅ Débite montantActif et crédite le compte agent")
        void approvisionner_success_mouvementsFinanciers() {
            // GIVEN
            BigDecimal montant = new BigDecimal("300000");
            BigDecimal montantActifAvant = banque.getMontantActif(); // 2000000
            BigDecimal soldeAgentAvant = compteAgent.getSolde();     // 100000

            when(compteBancaireRepository.calculerMontantLigne(banque.getId()))
                    .thenReturn(new BigDecimal("400000"));
            Operation savedOp = Operation.builder()
                    .id(1L).type(TypeOperation.APPROVISIONNEMENT_AGENT)
                    .montant(montant).frais(BigDecimal.ZERO).banque(banque).build();
            when(operationRepository.save(any())).thenReturn(savedOp);
            when(mapper.toOperationResponse(any())).thenReturn(
                    OperationResponse.builder()
                            .type(TypeOperation.APPROVISIONNEMENT_AGENT)
                            .montant(montant).frais(BigDecimal.ZERO).build());

            // WHEN
            OperationResponse response = superviseurService.approvisionnerAgent(
                    superviseur, agentUser.getId(),
                    new ApprovisionnerAgentRequest(montant, "Approvisionnement test"));

            // THEN
            assertThat(banque.getMontantActif())
                    .as("montantActif débité du montant")
                    .isEqualByComparingTo(montantActifAvant.subtract(montant));

            assertThat(compteAgent.getSolde())
                    .as("Compte agent crédité du montant")
                    .isEqualByComparingTo(soldeAgentAvant.add(montant));

            assertThat(response.getType())
                    .isEqualTo(TypeOperation.APPROVISIONNEMENT_AGENT);
            assertThat(response.getFrais()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("✅ montantLigne recalculé après approvisionnement (Skill 13)")
        void approvisionner_recalculeMontantLigne() {
            // GIVEN
            BigDecimal montant = new BigDecimal("200000");
            BigDecimal montantLigneAttendu = new BigDecimal("300000");

            when(compteBancaireRepository.calculerMontantLigne(banque.getId()))
                    .thenReturn(montantLigneAttendu);
            when(operationRepository.save(any())).thenReturn(
                    Operation.builder().id(1L).type(TypeOperation.APPROVISIONNEMENT_AGENT)
                            .montant(montant).frais(BigDecimal.ZERO).banque(banque).build());
            when(mapper.toOperationResponse(any())).thenReturn(new OperationResponse());

            // WHEN
            superviseurService.approvisionnerAgent(
                    superviseur, agentUser.getId(),
                    new ApprovisionnerAgentRequest(montant, null));

            // THEN
            assertThat(banque.getMontantLigne())
                    .isEqualByComparingTo(montantLigneAttendu);
            verify(compteBancaireRepository).calculerMontantLigne(banque.getId());
        }

        @Test
        @DisplayName("❌ Lève BankingException si montantActif insuffisant")
        void approvisionner_montantActifInsuffisant_throwsException() {
            // GIVEN
            BigDecimal montant = new BigDecimal("3000000"); // > montantActif (2000000)

            // WHEN / THEN
            assertThatThrownBy(() -> superviseurService.approvisionnerAgent(
                    superviseur, agentUser.getId(),
                    new ApprovisionnerAgentRequest(montant, null)))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("montant actif de la banque insuffisant");
        }

        @Test
        @DisplayName("❌ Bloque si le compte de l'agent est suspendu (Skill 15)")
        void approvisionner_compteAgentSuspendu_throwsException() {
            // GIVEN
            compteAgent.setStatut(StatutCompte.SUSPENDU);

            // WHEN / THEN
            assertThatThrownBy(() -> superviseurService.approvisionnerAgent(
                    superviseur, agentUser.getId(),
                    new ApprovisionnerAgentRequest(new BigDecimal("10000"), null)))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("suspendu");
        }

        @Test
        @DisplayName("❌ Bloque si la banque est suspendue (Skill 15)")
        void approvisionner_banqueSuspendue_throwsException() {
            // GIVEN
            banque.setStatut(StatutBanque.SUSPENDUE);

            // WHEN / THEN
            assertThatThrownBy(() -> superviseurService.approvisionnerAgent(
                    superviseur, agentUser.getId(),
                    new ApprovisionnerAgentRequest(new BigDecimal("10000"), null)))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("suspendue");
        }
    }

    // =========================================================================
    //  Skill 5 — Suspension / Réactivation de compte
    // =========================================================================

    @Nested
    @DisplayName("Skill 5 — Suspension & Réactivation de compte utilisateur")
    class SuspensionCompteTests {

        @Test
        @DisplayName("✅ Suspend un compte actif")
        void suspendreCompte_success() {
            // GIVEN
            when(compteBancaireRepository.findByUtilisateurId(agentUser.getId()))
                    .thenReturn(Optional.of(compteAgent));
            when(compteBancaireRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.toCompteBancaireResponse(any())).thenReturn(
                    CompteBancaireResponse.builder().statut(StatutCompte.SUSPENDU).build());

            // WHEN
            CompteBancaireResponse response = superviseurService.suspendreCompte(
                    superviseur, agentUser.getId());

            // THEN
            assertThat(compteAgent.getStatut()).isEqualTo(StatutCompte.SUSPENDU);
        }

        @Test
        @DisplayName("❌ Lève BankingException si compte déjà suspendu")
        void suspendreCompte_dejasSuspendu_throwsException() {
            // GIVEN
            compteAgent.setStatut(StatutCompte.SUSPENDU);
            when(compteBancaireRepository.findByUtilisateurId(agentUser.getId()))
                    .thenReturn(Optional.of(compteAgent));

            // WHEN / THEN
            assertThatThrownBy(() -> superviseurService.suspendreCompte(
                    superviseur, agentUser.getId()))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("déjà suspendu");
        }

        @Test
        @DisplayName("✅ Réactive un compte suspendu")
        void reactiverCompte_success() {
            // GIVEN
            compteAgent.setStatut(StatutCompte.SUSPENDU);
            when(compteBancaireRepository.findByUtilisateurId(agentUser.getId()))
                    .thenReturn(Optional.of(compteAgent));
            when(compteBancaireRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.toCompteBancaireResponse(any())).thenReturn(
                    CompteBancaireResponse.builder().statut(StatutCompte.ACTIF).build());

            // WHEN
            superviseurService.reactiverCompte(superviseur, agentUser.getId());

            // THEN
            assertThat(compteAgent.getStatut()).isEqualTo(StatutCompte.ACTIF);
        }

        @Test
        @DisplayName("❌ Bloque si l'utilisateur n'appartient pas à la banque du superviseur")
        void suspendreCompte_horsPerimetre_throwsBankingException() {
            // GIVEN — compte appartenant à une autre banque
            Banque autreBanque = Banque.builder().id(99L).nom("Autre Banque").build();
            CompteBancaire compteBanqueEtrangere = CompteBancaire.builder()
                    .id(999L).banque(autreBanque).utilisateur(agentUser).build();

            when(compteBancaireRepository.findByUtilisateurId(agentUser.getId()))
                    .thenReturn(Optional.of(compteBanqueEtrangere));

            // WHEN / THEN
            assertThatThrownBy(() -> superviseurService.suspendreCompte(
                    superviseur, agentUser.getId()))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("n'appartient pas à votre banque");
        }
    }
}
