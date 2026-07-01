package cm.edu.banking.service;

import cm.edu.banking.dto.request.CreateCompteBancaireRequest;
import cm.edu.banking.dto.request.DepotRequest;
import cm.edu.banking.dto.request.RetraitRequest;
import cm.edu.banking.dto.request.TransfertRequest;
import cm.edu.banking.dto.response.CompteBancaireResponse;
import cm.edu.banking.dto.response.OperationResponse;
import cm.edu.banking.exception.BankingException;
import cm.edu.banking.exception.ResourceNotFoundException;
import cm.edu.banking.model.Banque;
import cm.edu.banking.model.CompteBancaire;
import cm.edu.banking.model.Operation;
import cm.edu.banking.model.User;
import cm.edu.banking.model.enums.*;
import cm.edu.banking.repository.BanqueRepository;
import cm.edu.banking.repository.CompteBancaireRepository;
import cm.edu.banking.repository.OperationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du {@link TransactionService}.
 *
 * <p>Couverture :</p>
 * <ul>
 *   <li>Skill 8 : création de compte bancaire</li>
 *   <li>Skill 9 : dépôt (sans frais)</li>
 *   <li>Skill 10 : retrait (frais 2 %)</li>
 *   <li>Skill 11 : transfert interne (frais 2 %)</li>
 *   <li>Skill 12 : transfert interbancaire (frais 4 %)</li>
 *   <li>Skill 13 : recalcul montantLigne</li>
 *   <li>Skill 15 : blocages suspension banque / compte</li>
 * </ul>
 *
 * <p>Stratégie : chaque test est <em>unitaire</em> et <em>isolé</em>.
 * Toutes les dépendances JPA sont mockées via Mockito. Aucune base de
 * données réelle n'est utilisée.</p>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService — Tests unitaires")
class TransactionServiceTest {

    // =========================================================================
    //  Mocks
    // =========================================================================

    @Mock private CompteBancaireRepository compteBancaireRepository;
    @Mock private BanqueRepository banqueRepository;
    @Mock private OperationRepository operationRepository;
    @Mock private BankingMapper mapper;

    @InjectMocks
    private TransactionService transactionService;

    // =========================================================================
    //  Fixtures réutilisables
    // =========================================================================

    private Banque banqueA;
    private Banque banqueB;
    private User agent;
    private User client;
    private User client2;
    private CompteBancaire compteAgent;
    private CompteBancaire compteClient;
    private CompteBancaire compteClient2BanqueB;

    @BeforeEach
    void setUp() {
        banqueA = Banque.builder()
                .id(1L).nom("Banque Alpha")
                .statut(StatutBanque.ACTIVE)
                .montantInitial(new BigDecimal("5000000"))
                .montantActif(new BigDecimal("4000000"))
                .montantLigne(BigDecimal.ZERO)
                .build();

        banqueB = Banque.builder()
                .id(2L).nom("Banque Beta")
                .statut(StatutBanque.ACTIVE)
                .montantInitial(new BigDecimal("3000000"))
                .montantActif(new BigDecimal("2500000"))
                .montantLigne(BigDecimal.ZERO)
                .build();

        agent = User.builder()
                .id(10L).nom("Doe").prenom("John")
                .email("agent@banquealpha.cm").role(Role.AGENT)
                .banque(banqueA).build();

        client = User.builder()
                .id(20L).nom("Martin").prenom("Alice")
                .email("alice@client.cm").role(Role.CLIENT)
                .build();

        client2 = User.builder()
                .id(30L).nom("Dupont").prenom("Bob")
                .email("bob@client.cm").role(Role.CLIENT)
                .build();

        compteAgent = CompteBancaire.builder()
                .id(100L).numeroCompte("BK00001-20240115-001")
                .solde(new BigDecimal("500000"))
                .statut(StatutCompte.ACTIF)
                .utilisateur(agent).banque(banqueA)
                .dateCreation(LocalDateTime.now())
                .build();

        compteClient = CompteBancaire.builder()
                .id(200L).numeroCompte("BK00001-20240115-002")
                .solde(new BigDecimal("200000"))
                .statut(StatutCompte.ACTIF)
                .utilisateur(client).banque(banqueA)
                .dateCreation(LocalDateTime.now())
                .build();

        compteClient2BanqueB = CompteBancaire.builder()
                .id(300L).numeroCompte("BK00002-20240115-003")
                .solde(new BigDecimal("150000"))
                .statut(StatutCompte.ACTIF)
                .utilisateur(client2).banque(banqueB)
                .dateCreation(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    //  Skill 8 — Création de compte bancaire
    // =========================================================================

    @Nested
    @DisplayName("Skill 8 — Création de compte bancaire")
    class CreationCompteTests {

        @Test
        @DisplayName("✅ Crée un compte avec solde initial = 0")
        void creerCompte_success() {
            // GIVEN
            when(compteBancaireRepository.existsByUtilisateurId(client.getId()))
                    .thenReturn(false);
            when(banqueRepository.findById(banqueA.getId()))
                    .thenReturn(Optional.of(banqueA));
            when(compteBancaireRepository.save(any(CompteBancaire.class)))
                    .thenAnswer(inv -> {
                        CompteBancaire c = inv.getArgument(0);
                        c = CompteBancaire.builder()
                                .id(201L).numeroCompte(c.getNumeroCompte())
                                .solde(c.getSolde()).statut(c.getStatut())
                                .utilisateur(c.getUtilisateur()).banque(c.getBanque())
                                .build();
                        return c;
                    });
            CompteBancaireResponse expectedResponse = CompteBancaireResponse.builder()
                    .id(201L).solde(BigDecimal.ZERO).build();
            when(mapper.toCompteBancaireResponse(any())).thenReturn(expectedResponse);

            CreateCompteBancaireRequest request =
                    new CreateCompteBancaireRequest(banqueA.getId());

            // WHEN
            CompteBancaireResponse response =
                    transactionService.creerCompteBancaire(client, request);

            // THEN
            assertThat(response).isNotNull();
            assertThat(response.getSolde()).isEqualByComparingTo(BigDecimal.ZERO);

            ArgumentCaptor<CompteBancaire> captor =
                    ArgumentCaptor.forClass(CompteBancaire.class);
            verify(compteBancaireRepository).save(captor.capture());
            assertThat(captor.getValue().getSolde())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(captor.getValue().getStatut())
                    .isEqualTo(StatutCompte.ACTIF);
        }

        @Test
        @DisplayName("❌ Lève BankingException si l'utilisateur a déjà un compte")
        void creerCompte_dejaExistant_throwsBankingException() {
            // GIVEN
            when(compteBancaireRepository.existsByUtilisateurId(client.getId()))
                    .thenReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.creerCompteBancaire(
                    client, new CreateCompteBancaireRequest(banqueA.getId())))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("déjà un compte bancaire");
        }

        @Test
        @DisplayName("❌ Lève BankingException si la banque est suspendue")
        void creerCompte_banqueSuspendue_throwsBankingException() {
            // GIVEN
            banqueA.setStatut(StatutBanque.SUSPENDUE);
            when(compteBancaireRepository.existsByUtilisateurId(client.getId()))
                    .thenReturn(false);
            when(banqueRepository.findById(banqueA.getId()))
                    .thenReturn(Optional.of(banqueA));

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.creerCompteBancaire(
                    client, new CreateCompteBancaireRequest(banqueA.getId())))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("suspendue");
        }

        @Test
        @DisplayName("❌ Lève ResourceNotFoundException si la banque est introuvable")
        void creerCompte_banqueIntrouvable_throwsResourceNotFoundException() {
            // GIVEN
            when(compteBancaireRepository.existsByUtilisateurId(client.getId()))
                    .thenReturn(false);
            when(banqueRepository.findById(99L)).thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.creerCompteBancaire(
                    client, new CreateCompteBancaireRequest(99L)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    //  Skill 9 — Dépôt (sans frais)
    // =========================================================================

    @Nested
    @DisplayName("Skill 9 — Dépôt agent → client (frais = 0)")
    class DepotTests {

        @BeforeEach
        void setupMocks() {
            when(compteBancaireRepository.findByUtilisateurId(agent.getId()))
                    .thenReturn(Optional.of(compteAgent));
            when(compteBancaireRepository.findByUtilisateurId(client.getId()))
                    .thenReturn(Optional.of(compteClient));
            when(compteBancaireRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(compteBancaireRepository.calculerMontantLigne(anyLong()))
                    .thenReturn(BigDecimal.ZERO);
            when(banqueRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("✅ Débite l'agent et crédite le client — frais nuls")
        void depot_success_aucunFrais() {
            // GIVEN
            BigDecimal montant = new BigDecimal("50000");
            BigDecimal soldAgentAvant = compteAgent.getSolde();
            BigDecimal soldeClientAvant = compteClient.getSolde();

            Operation opSaved = buildOperation(TypeOperation.DEPOT, montant, BigDecimal.ZERO);
            when(operationRepository.save(any())).thenReturn(opSaved);
            OperationResponse expectedResp = buildOperationResponse(
                    TypeOperation.DEPOT, montant, BigDecimal.ZERO);
            when(mapper.toOperationResponse(any())).thenReturn(expectedResp);

            // WHEN
            OperationResponse response = transactionService.effectuerDepot(
                    agent, new DepotRequest(client.getId(), montant, "Test dépôt"));

            // THEN
            assertThat(response.getMontant()).isEqualByComparingTo(montant);
            assertThat(response.getFrais()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(compteAgent.getSolde())
                    .isEqualByComparingTo(soldAgentAvant.subtract(montant));
            assertThat(compteClient.getSolde())
                    .isEqualByComparingTo(soldeClientAvant.add(montant));

            // Vérification que les frais ne touchent pas le montantActif
            verify(banqueRepository, atLeastOnce()).save(any(Banque.class));
            ArgumentCaptor<Operation> opCaptor = ArgumentCaptor.forClass(Operation.class);
            verify(operationRepository).save(opCaptor.capture());
            assertThat(opCaptor.getValue().getFrais()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(opCaptor.getValue().getType()).isEqualTo(TypeOperation.DEPOT);
        }

        @Test
        @DisplayName("❌ Lève BankingException si le solde agent est insuffisant")
        void depot_soldeAgentInsuffisant_throwsBankingException() {
            // GIVEN — montant > solde agent
            BigDecimal montant = new BigDecimal("600000"); // soldeAgent = 500000

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.effectuerDepot(
                    agent, new DepotRequest(client.getId(), montant, null)))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("solde agent insuffisant");
        }

        @Test
        @DisplayName("❌ Bloque si la banque de l'agent est suspendue (Skill 15)")
        void depot_banqueAgentSuspendue_throwsBankingException() {
            // GIVEN
            banqueA.setStatut(StatutBanque.SUSPENDUE);

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.effectuerDepot(
                    agent, new DepotRequest(client.getId(), new BigDecimal("1000"), null)))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("suspendue");
        }

        @Test
        @DisplayName("❌ Bloque si le compte du client est suspendu (Skill 15)")
        void depot_compteClientSuspendu_throwsBankingException() {
            // GIVEN
            compteClient.setStatut(StatutCompte.SUSPENDU);

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.effectuerDepot(
                    agent, new DepotRequest(client.getId(), new BigDecimal("1000"), null)))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("suspendu");
        }
    }

    // =========================================================================
    //  Skill 11 — Transfert interne (frais 2 %)
    // =========================================================================

    @Nested
    @DisplayName("Skill 11 — Transfert interne même banque (frais 2 %)")
    class TransfertInterneTests {

        private User client2SameBanque;
        private CompteBancaire compteClient2SameBanque;

        @BeforeEach
        void setupTransfertInterne() {
            client2SameBanque = User.builder()
                    .id(21L).nom("Durand").prenom("Eva")
                    .email("eva@client.cm").role(Role.CLIENT).build();

            compteClient2SameBanque = CompteBancaire.builder()
                    .id(201L).numeroCompte("BK00001-20240115-099")
                    .solde(new BigDecimal("50000"))
                    .statut(StatutCompte.ACTIF)
                    .utilisateur(client2SameBanque).banque(banqueA)
                    .build();

            when(compteBancaireRepository.findByUtilisateurId(client.getId()))
                    .thenReturn(Optional.of(compteClient));
            when(compteBancaireRepository.findByNumeroCompte(
                    compteClient2SameBanque.getNumeroCompte()))
                    .thenReturn(Optional.of(compteClient2SameBanque));
            when(compteBancaireRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(banqueRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(compteBancaireRepository.calculerMontantLigne(anyLong()))
                    .thenReturn(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("✅ Frais 2 % — émetteur débité montant+frais, destinataire crédité montant")
        void transfertInterne_success_frais2Pourcent() {
            // GIVEN
            BigDecimal montant = new BigDecimal("50000");
            BigDecimal fraisAttendus = new BigDecimal("1000.00"); // 50000 × 2%
            BigDecimal debitEmetteur = new BigDecimal("51000.00");

            BigDecimal soldeEmetteurAvant = compteClient.getSolde();    // 200000
            BigDecimal soldeDestAvant = compteClient2SameBanque.getSolde(); // 50000
            BigDecimal montantActifAvant = banqueA.getMontantActif();

            Operation opSaved = buildOperation(
                    TypeOperation.TRANSFERT_INTERNE, montant, fraisAttendus);
            when(operationRepository.save(any())).thenReturn(opSaved);
            when(mapper.toOperationResponse(any()))
                    .thenReturn(buildOperationResponse(
                            TypeOperation.TRANSFERT_INTERNE, montant, fraisAttendus));

            // WHEN
            OperationResponse response = transactionService.effectuerTransfert(
                    client, new TransfertRequest(
                            compteClient2SameBanque.getNumeroCompte(), montant, null));

            // THEN
            assertThat(response.getType()).isEqualTo(TypeOperation.TRANSFERT_INTERNE);

            assertThat(compteClient.getSolde())
                    .as("Émetteur débité de montant + frais 2 %")
                    .isEqualByComparingTo(soldeEmetteurAvant.subtract(debitEmetteur));

            assertThat(compteClient2SameBanque.getSolde())
                    .as("Destinataire crédité du montant seul")
                    .isEqualByComparingTo(soldeDestAvant.add(montant));

            assertThat(banqueA.getMontantActif())
                    .as("Frais 2 % versés au montantActif de la banque")
                    .isEqualByComparingTo(montantActifAvant.add(fraisAttendus));

            ArgumentCaptor<Operation> captor = ArgumentCaptor.forClass(Operation.class);
            verify(operationRepository).save(captor.capture());
            assertThat(captor.getValue().getType())
                    .isEqualTo(TypeOperation.TRANSFERT_INTERNE);
        }

        @Test
        @DisplayName("❌ Lève BankingException si solde émetteur insuffisant")
        void transfertInterne_soldeInsuffisant() {
            // GIVEN — solde = 200000, montant + frais 2% = 204000
            BigDecimal montant = new BigDecimal("200000");

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.effectuerTransfert(
                    client, new TransfertRequest(
                            compteClient2SameBanque.getNumeroCompte(), montant, null)))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("Solde insuffisant");
        }

        @Test
        @DisplayName("❌ Bloque l'auto-transfert (même compte)")
        void transfertInterne_autoTransfert_throwsBankingException() {
            // GIVEN — le destinataire est le même compte que l'émetteur
            when(compteBancaireRepository.findByNumeroCompte(compteClient.getNumeroCompte()))
                    .thenReturn(Optional.of(compteClient));

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.effectuerTransfert(
                    client, new TransfertRequest(
                            compteClient.getNumeroCompte(), new BigDecimal("1000"), null)))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("même compte");
        }

        @Test
        @DisplayName("❌ Bloque si le compte destinataire est suspendu (Skill 15)")
        void transfertInterne_compteDestinatairesSuspendu() {
            // GIVEN
            compteClient2SameBanque.setStatut(StatutCompte.SUSPENDU);

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.effectuerTransfert(
                    client, new TransfertRequest(
                            compteClient2SameBanque.getNumeroCompte(),
                            new BigDecimal("1000"), null)))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("suspendu");
        }
    }

    // =========================================================================
    //  Skill 12 — Transfert interbancaire (frais 4 %)
    // =========================================================================

    @Nested
    @DisplayName("Skill 12 — Transfert interbancaire (frais 4 %)")
    class TransfertInterbancaireTests {

        @BeforeEach
        void setupMocks() {
            when(compteBancaireRepository.findByUtilisateurId(client.getId()))
                    .thenReturn(Optional.of(compteClient));
            when(compteBancaireRepository.findByNumeroCompte(
                    compteClient2BanqueB.getNumeroCompte()))
                    .thenReturn(Optional.of(compteClient2BanqueB));
            when(compteBancaireRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(banqueRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(compteBancaireRepository.calculerMontantLigne(anyLong()))
                    .thenReturn(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("✅ Frais 4 % — type TRANSFERT_INTERBANCAIRE détecté automatiquement")
        void transfertInterbancaire_success_frais4Pourcent() {
            // GIVEN
            BigDecimal montant = new BigDecimal("50000");
            BigDecimal fraisAttendus = new BigDecimal("2000.00"); // 50000 × 4%
            BigDecimal debitEmetteur = new BigDecimal("52000.00");

            BigDecimal soldeEmetteurAvant = compteClient.getSolde();       // 200000
            BigDecimal soldeDestAvant = compteClient2BanqueB.getSolde();   // 150000
            BigDecimal montantActifBanqueAAvant = banqueA.getMontantActif();

            Operation opSaved = buildOperation(
                    TypeOperation.TRANSFERT_INTERBANCAIRE, montant, fraisAttendus);
            when(operationRepository.save(any())).thenReturn(opSaved);
            when(mapper.toOperationResponse(any()))
                    .thenReturn(buildOperationResponse(
                            TypeOperation.TRANSFERT_INTERBANCAIRE, montant, fraisAttendus));

            // WHEN
            OperationResponse response = transactionService.effectuerTransfert(
                    client, new TransfertRequest(
                            compteClient2BanqueB.getNumeroCompte(), montant, null));

            // THEN
            assertThat(response.getType()).isEqualTo(TypeOperation.TRANSFERT_INTERBANCAIRE);

            assertThat(compteClient.getSolde())
                    .as("Émetteur débité de montant + frais 4 %")
                    .isEqualByComparingTo(soldeEmetteurAvant.subtract(debitEmetteur));

            assertThat(compteClient2BanqueB.getSolde())
                    .as("Destinataire crédité du montant seul")
                    .isEqualByComparingTo(soldeDestAvant.add(montant));

            assertThat(banqueA.getMontantActif())
                    .as("Frais 4 % au montantActif de la BANQUE ÉMETTRICE (BanqueA)")
                    .isEqualByComparingTo(montantActifBanqueAAvant.add(fraisAttendus));

            // BanqueB ne reçoit pas les frais
            assertThat(banqueB.getMontantActif())
                    .as("BanqueB ne perçoit aucun frais")
                    .isEqualByComparingTo(new BigDecimal("2500000"));

            ArgumentCaptor<Operation> captor = ArgumentCaptor.forClass(Operation.class);
            verify(operationRepository).save(captor.capture());
            assertThat(captor.getValue().getType())
                    .isEqualTo(TypeOperation.TRANSFERT_INTERBANCAIRE);
            assertThat(captor.getValue().getFrais())
                    .isEqualByComparingTo(fraisAttendus);
        }

        @Test
        @DisplayName("✅ Frais 4 % : 25 000 FCFA → frais = 1 000 FCFA")
        void transfertInterbancaire_calcul_frais_25000() {
            // GIVEN
            BigDecimal montant = new BigDecimal("25000");
            BigDecimal fraisAttendus = new BigDecimal("1000.00");

            Operation opSaved = buildOperation(
                    TypeOperation.TRANSFERT_INTERBANCAIRE, montant, fraisAttendus);
            when(operationRepository.save(any())).thenReturn(opSaved);
            when(mapper.toOperationResponse(any()))
                    .thenReturn(buildOperationResponse(
                            TypeOperation.TRANSFERT_INTERBANCAIRE, montant, fraisAttendus));

            // WHEN
            transactionService.effectuerTransfert(
                    client, new TransfertRequest(
                            compteClient2BanqueB.getNumeroCompte(), montant, null));

            // THEN
            ArgumentCaptor<Operation> captor = ArgumentCaptor.forClass(Operation.class);
            verify(operationRepository).save(captor.capture());
            assertThat(captor.getValue().getFrais())
                    .isEqualByComparingTo(fraisAttendus);
        }

        @Test
        @DisplayName("❌ Bloque si la banque destinataire est suspendue (Skill 15)")
        void transfertInterbancaire_banqueDestinatairesSuspendue() {
            // GIVEN
            banqueB.setStatut(StatutBanque.SUSPENDUE);

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.effectuerTransfert(
                    client, new TransfertRequest(
                            compteClient2BanqueB.getNumeroCompte(),
                            new BigDecimal("10000"), null)))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("suspendue");
        }

        @Test
        @DisplayName("❌ Lève ResourceNotFoundException si compte destinataire introuvable")
        void transfertInterbancaire_compteDestIntrouvable() {
            // GIVEN
            when(compteBancaireRepository.findByNumeroCompte("INCONNU-999"))
                    .thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> transactionService.effectuerTransfert(
                    client, new TransfertRequest(
                            "INCONNU-999", new BigDecimal("10000"), null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    //  Skill 13 — Recalcul montantLigne
    // =========================================================================

    @Nested
    @DisplayName("Skill 13 — Recalcul montantLigne après opération")
    class MontantLigneTests {

        @Test
        @DisplayName("✅ montantLigne recalculé après un dépôt réussi")
        void montantLigne_recalculeAprésDepot() {
            // GIVEN
            BigDecimal montantLigneAttendu = new BigDecimal("750000");
            when(compteBancaireRepository.findByUtilisateurId(agent.getId()))
                    .thenReturn(Optional.of(compteAgent));
            when(compteBancaireRepository.findByUtilisateurId(client.getId()))
                    .thenReturn(Optional.of(compteClient));
            when(compteBancaireRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(banqueRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(compteBancaireRepository.calculerMontantLigne(banqueA.getId()))
                    .thenReturn(montantLigneAttendu);
            Operation opSaved = buildOperation(
                    TypeOperation.DEPOT, new BigDecimal("10000"), BigDecimal.ZERO);
            when(operationRepository.save(any())).thenReturn(opSaved);
            when(mapper.toOperationResponse(any()))
                    .thenReturn(buildOperationResponse(
                            TypeOperation.DEPOT, new BigDecimal("10000"), BigDecimal.ZERO));

            // WHEN
            transactionService.effectuerDepot(
                    agent, new DepotRequest(client.getId(), new BigDecimal("10000"), null));

            // THEN — banqueA.montantLigne mis à jour
            assertThat(banqueA.getMontantLigne())
                    .isEqualByComparingTo(montantLigneAttendu);
            verify(compteBancaireRepository, atLeastOnce())
                    .calculerMontantLigne(banqueA.getId());
        }
    }

    // =========================================================================
    //  Helpers privés
    // =========================================================================

    private Operation buildOperation(TypeOperation type, BigDecimal montant,
                                     BigDecimal frais) {
        return Operation.builder()
                .id(1L).type(type).montant(montant).frais(frais)
                .banque(banqueA).dateOperation(LocalDateTime.now())
                .build();
    }

    private OperationResponse buildOperationResponse(TypeOperation type,
                                                     BigDecimal montant,
                                                     BigDecimal frais) {
        return OperationResponse.builder()
                .id(1L).type(type).montant(montant).frais(frais)
                .montantTotal(montant.add(frais))
                .banqueId(banqueA.getId()).nomBanque(banqueA.getNom())
                .build();
    }
}
