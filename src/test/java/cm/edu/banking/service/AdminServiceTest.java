package cm.edu.banking.service;

import cm.edu.banking.dto.request.CreateBanqueRequest;
import cm.edu.banking.dto.request.CreateSuperviseurRequest;
import cm.edu.banking.dto.request.RetraitAdminRequest;
import cm.edu.banking.dto.response.BanqueResponse;
import cm.edu.banking.dto.response.OperationResponse;
import cm.edu.banking.dto.response.UserResponse;
import cm.edu.banking.exception.BankingException;
import cm.edu.banking.exception.DuplicateResourceException;
import cm.edu.banking.exception.ResourceNotFoundException;
import cm.edu.banking.model.Banque;
import cm.edu.banking.model.Operation;
import cm.edu.banking.model.User;
import cm.edu.banking.model.enums.Role;
import cm.edu.banking.model.enums.StatutBanque;
import cm.edu.banking.model.enums.TypeOperation;
import cm.edu.banking.repository.BanqueRepository;
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
 * Tests unitaires du {@link AdminService}.
 *
 * <p>Couverture :</p>
 * <ul>
 *   <li>Skill 3 : création de banque</li>
 *   <li>Skill 4 : suspension/réactivation, retrait admin, création superviseur</li>
 * </ul>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService — Tests unitaires")
class AdminServiceTest {

    @Mock private BanqueRepository banqueRepository;
    @Mock private UserRepository userRepository;
    @Mock private OperationRepository operationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private BankingMapper mapper;

    @InjectMocks
    private AdminService adminService;

    private Banque banqueActive;

    @BeforeEach
    void setUp() {
        banqueActive = Banque.builder()
                .id(1L).nom("Banque Cameroun")
                .statut(StatutBanque.ACTIVE)
                .montantInitial(new BigDecimal("5000000"))
                .montantActif(new BigDecimal("4500000"))
                .montantLigne(BigDecimal.ZERO)
                .dateCreation(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    //  Skill 3 — Création de banque
    // =========================================================================

    @Nested
    @DisplayName("Skill 3 — Création de banque")
    class CreationBanqueTests {

        @Test
        @DisplayName("✅ Crée une banque : montantActif = montantInitial, montantLigne = 0")
        void creerBanque_success() {
            // GIVEN
            CreateBanqueRequest request = new CreateBanqueRequest(
                    "Nouvelle Banque", new BigDecimal("3000000"));

            when(banqueRepository.existsByNom("Nouvelle Banque")).thenReturn(false);
            when(banqueRepository.save(any(Banque.class))).thenAnswer(inv -> {
                Banque b = inv.getArgument(0);
                b = Banque.builder()
                        .id(2L).nom(b.getNom())
                        .montantInitial(b.getMontantInitial())
                        .montantActif(b.getMontantActif())
                        .montantLigne(b.getMontantLigne())
                        .statut(b.getStatut())
                        .build();
                return b;
            });

            // WHEN
            adminService.creerBanque(request);

            // THEN
            ArgumentCaptor<Banque> captor = ArgumentCaptor.forClass(Banque.class);
            verify(banqueRepository).save(captor.capture());
            Banque saved = captor.getValue();

            assertThat(saved.getMontantActif())
                    .as("montantActif doit égaler montantInitial à la création")
                    .isEqualByComparingTo(new BigDecimal("3000000"));
            assertThat(saved.getMontantLigne())
                    .as("montantLigne doit être 0 à la création")
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getStatut()).isEqualTo(StatutBanque.ACTIVE);
        }

        @Test
        @DisplayName("❌ Lève DuplicateResourceException si le nom est déjà utilisé")
        void creerBanque_nomDuplique_throwsException() {
            // GIVEN
            when(banqueRepository.existsByNom("Banque Cameroun")).thenReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> adminService.creerBanque(
                    new CreateBanqueRequest("Banque Cameroun", new BigDecimal("3000000"))))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Banque Cameroun");
        }
    }

    // =========================================================================
    //  Skill 4 — Suspension / Réactivation
    // =========================================================================

    @Nested
    @DisplayName("Skill 4 — Suspension & Réactivation de banque")
    class StatutBanqueTests {

        @Test
        @DisplayName("✅ Suspend une banque active")
        void suspendre_banqueActive_success() {
            // GIVEN
            when(banqueRepository.findById(1L)).thenReturn(Optional.of(banqueActive));
            when(banqueRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // WHEN
            adminService.suspendrebanque(1L);

            // THEN
            assertThat(banqueActive.getStatut()).isEqualTo(StatutBanque.SUSPENDUE);
        }

        @Test
        @DisplayName("❌ Lève BankingException si la banque est déjà suspendue")
        void suspendre_banqueDejaSuspendue_throwsException() {
            // GIVEN
            banqueActive.setStatut(StatutBanque.SUSPENDUE);
            when(banqueRepository.findById(1L)).thenReturn(Optional.of(banqueActive));

            // WHEN / THEN
            assertThatThrownBy(() -> adminService.suspendrebanque(1L))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("déjà suspendue");
        }

        @Test
        @DisplayName("✅ Réactive une banque suspendue")
        void reactiver_banqueSuspendue_success() {
            // GIVEN
            banqueActive.setStatut(StatutBanque.SUSPENDUE);
            when(banqueRepository.findById(1L)).thenReturn(Optional.of(banqueActive));
            when(banqueRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // WHEN
            adminService.reactiverBanque(1L);

            // THEN
            assertThat(banqueActive.getStatut()).isEqualTo(StatutBanque.ACTIVE);
        }

        @Test
        @DisplayName("❌ Lève BankingException si la banque est déjà active")
        void reactiver_banqueDejaActive_throwsException() {
            // GIVEN
            when(banqueRepository.findById(1L)).thenReturn(Optional.of(banqueActive));

            // WHEN / THEN
            assertThatThrownBy(() -> adminService.reactiverBanque(1L))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("déjà active");
        }

        @Test
        @DisplayName("❌ Lève ResourceNotFoundException si banque introuvable")
        void suspendre_banqueIntrouvable_throwsException() {
            // GIVEN
            when(banqueRepository.findById(99L)).thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> adminService.suspendrebanque(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    //  Skill 4 — Retrait administrateur
    // =========================================================================

    @Nested
    @DisplayName("Skill 4 — Retrait administrateur (plafond 1 000 000 FCFA)")
    class RetraitAdminTests {

        @BeforeEach
        void setupMock() {
            when(banqueRepository.findById(1L)).thenReturn(Optional.of(banqueActive));
        }

        @Test
        @DisplayName("✅ Retrait valide débite le montantActif et persiste l'opération")
        void retraitAdmin_success() {
            // GIVEN
            BigDecimal montant = new BigDecimal("500000");
            BigDecimal montantActifAvant = banqueActive.getMontantActif(); // 4500000

            Operation savedOp = Operation.builder()
                    .id(1L).type(TypeOperation.RETRAIT_ADMIN)
                    .montant(montant).frais(BigDecimal.ZERO)
                    .banque(banqueActive).build();
            when(operationRepository.save(any())).thenReturn(savedOp);
            when(banqueRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            OperationResponse expectedResp = OperationResponse.builder()
                    .type(TypeOperation.RETRAIT_ADMIN).montant(montant)
                    .frais(BigDecimal.ZERO).build();
            when(mapper.toOperationResponse(any())).thenReturn(expectedResp);

            // WHEN
            OperationResponse response = adminService.retraitAdmin(
                    1L, new RetraitAdminRequest(montant, "Retrait test"));

            // THEN
            assertThat(banqueActive.getMontantActif())
                    .isEqualByComparingTo(montantActifAvant.subtract(montant));

            ArgumentCaptor<Operation> captor = ArgumentCaptor.forClass(Operation.class);
            verify(operationRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(TypeOperation.RETRAIT_ADMIN);
            assertThat(captor.getValue().getFrais()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("❌ Lève BankingException si montant > 1 000 000 FCFA (plafond)")
        void retraitAdmin_depassePlafond_throwsBankingException() {
            // GIVEN
            BigDecimal montantExcessif = new BigDecimal("1000001");

            // WHEN / THEN
            assertThatThrownBy(() -> adminService.retraitAdmin(
                    1L, new RetraitAdminRequest(montantExcessif, null)))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("plafonné à 1 000 000");
        }

        @Test
        @DisplayName("❌ Lève BankingException si montantActif insuffisant")
        void retraitAdmin_montantActifInsuffisant_throwsBankingException() {
            // GIVEN — montantActif = 4500000, demande 1000000 → OK
            // Forçons un montantActif très faible
            banqueActive.setMontantActif(new BigDecimal("100000"));
            BigDecimal montant = new BigDecimal("500000");

            // WHEN / THEN
            assertThatThrownBy(() -> adminService.retraitAdmin(
                    1L, new RetraitAdminRequest(montant, null)))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("montant actif insuffisant");
        }

        @Test
        @DisplayName("✅ Retrait exactement au plafond (1 000 000 FCFA) — autorisé")
        void retraitAdmin_exactementPlafond_autorise() {
            // GIVEN
            BigDecimal montant = new BigDecimal("1000000");
            Operation savedOp = Operation.builder()
                    .id(2L).type(TypeOperation.RETRAIT_ADMIN)
                    .montant(montant).frais(BigDecimal.ZERO)
                    .banque(banqueActive).build();
            when(operationRepository.save(any())).thenReturn(savedOp);
            when(banqueRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.toOperationResponse(any())).thenReturn(new OperationResponse());

            // WHEN — ne doit pas lever d'exception
            assertThatCode(() -> adminService.retraitAdmin(
                    1L, new RetraitAdminRequest(montant, null)))
                    .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    //  Skill 4 — Création de superviseur
    // =========================================================================

    @Nested
    @DisplayName("Skill 4 — Création de superviseur")
    class CreationSuperviseurTests {

        @Test
        @DisplayName("✅ Crée un superviseur avec rôle SUPERVISEUR et banque rattachée")
        void creerSuperviseur_success() {
            // GIVEN
            CreateSuperviseurRequest request = CreateSuperviseurRequest.builder()
                    .nom("Tankou").prenom("Paul")
                    .email("paul@banque.cm").telephone("677000001")
                    .motDePasse("Secret123").banqueId(1L).build();

            when(banqueRepository.findById(1L)).thenReturn(Optional.of(banqueActive));
            when(userRepository.existsByEmail("paul@banque.cm")).thenReturn(false);
            when(userRepository.existsByTelephone("677000001")).thenReturn(false);
            when(passwordEncoder.encode("Secret123")).thenReturn("$2a$10$HASH");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                return User.builder().id(5L).nom(u.getNom()).prenom(u.getPrenom())
                        .email(u.getEmail()).role(u.getRole()).banque(u.getBanque()).build();
            });
            when(mapper.toUserResponse(any())).thenReturn(UserResponse.builder()
                    .id(5L).role(Role.SUPERVISEUR).build());

            // WHEN
            UserResponse response = adminService.creerSuperviseur(request);

            // THEN
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(Role.SUPERVISEUR);
            assertThat(captor.getValue().getBanque()).isEqualTo(banqueActive);
            assertThat(captor.getValue().getMotDePasse()).isEqualTo("$2a$10$HASH");
        }

        @Test
        @DisplayName("❌ Lève DuplicateResourceException si email déjà utilisé")
        void creerSuperviseur_emailDuplique_throwsException() {
            // GIVEN
            when(banqueRepository.findById(1L)).thenReturn(Optional.of(banqueActive));
            when(userRepository.existsByEmail("paul@banque.cm")).thenReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> adminService.creerSuperviseur(
                    CreateSuperviseurRequest.builder()
                            .email("paul@banque.cm").telephone("677000001")
                            .banqueId(1L).motDePasse("pass").build()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");
        }
    }
}
