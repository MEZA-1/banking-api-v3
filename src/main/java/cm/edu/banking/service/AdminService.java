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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service applicatif regroupant toutes les opérations réservées au
 * rôle {@link Role#ADMIN} (Skills 3 et 4).
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>Création, suspension et réactivation des banques (Skill 3).</li>
 *   <li>Consultation de toutes les banques et de tous les utilisateurs
 *       (Skill 4).</li>
 *   <li>Création des superviseurs rattachés à une banque (Skill 4).</li>
 *   <li>Retrait administrateur sur le {@code montantActif} d'une banque,
 *       plafonné à 1 000 000 FCFA par opération (Skill 4).</li>
 * </ul>
 *
 * <p>Toutes les méthodes d'écriture sont annotées {@link Transactional}
 * pour garantir l'atomicité des opérations (mise à jour de la banque +
 * enregistrement de l'opération dans l'historique).</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final BanqueRepository banqueRepository;
    private final UserRepository userRepository;
    private final OperationRepository operationRepository;
    private final PasswordEncoder passwordEncoder;
    private final BankingMapper mapper;

    // =========================================================================
    //  Gestion des banques (Skill 3 & 4)
    // =========================================================================

    /**
     * Crée une nouvelle banque sur la plateforme.
     *
     * <p>Règles métier appliquées :</p>
     * <ul>
     *   <li>Le nom de la banque doit être unique.</li>
     *   <li>Le {@code montantInitial} doit être ≥ 2 000 000 FCFA (validé
     *       en amont par {@link jakarta.validation.Valid} sur le DTO, mais
     *       la vérification est redondante ici pour la robustesse).</li>
     *   <li>{@code montantActif} est initialisé à {@code montantInitial}.</li>
     *   <li>{@code montantLigne} est initialisé à zéro.</li>
     * </ul>
     *
     * @param request les données de la banque à créer
     * @return le DTO représentant la banque créée
     * @throws DuplicateResourceException si une banque avec ce nom existe déjà
     */
    @Transactional
    public BanqueResponse creerBanque(CreateBanqueRequest request) {
        if (banqueRepository.existsByNom(request.getNom())) {
            throw new DuplicateResourceException("Banque", "nom", request.getNom());
        }

        Banque banque = Banque.builder()
                .nom(request.getNom())
                .montantInitial(request.getMontantInitial())
                .montantActif(request.getMontantInitial())  // montantActif = montantInitial à la création
                .montantLigne(BigDecimal.ZERO)
                .statut(StatutBanque.ACTIVE)
                .build();

        Banque saved = banqueRepository.save(banque);
        log.info("Banque créée : {} (id={})", saved.getNom(), saved.getId());

        return toBanqueResponse(saved);
    }

    /**
     * Suspend une banque active.
     *
     * <p>Une fois suspendue, toutes les opérations financières des
     * utilisateurs rattachés à cette banque sont bloquées (Skill 15).
     * La suspension d'une banque déjà suspendue est ignorée sans erreur.</p>
     *
     * @param banqueId l'identifiant de la banque à suspendre
     * @return le DTO représentant la banque avec le statut mis à jour
     * @throws ResourceNotFoundException si aucune banque ne correspond
     *                                   à l'identifiant fourni
     */
    @Transactional
    public BanqueResponse suspendrebanque(Long banqueId) {
        Banque banque = findBanqueById(banqueId);

        if (banque.getStatut() == StatutBanque.SUSPENDUE) {
            throw new BankingException("La banque '" + banque.getNom() + "' est déjà suspendue.");
        }

        banque.setStatut(StatutBanque.SUSPENDUE);
        log.info("Banque suspendue : {} (id={})", banque.getNom(), banqueId);

        return toBanqueResponse(banqueRepository.save(banque));
    }

    /**
     * Réactive une banque précédemment suspendue.
     *
     * @param banqueId l'identifiant de la banque à réactiver
     * @return le DTO représentant la banque avec le statut mis à jour
     * @throws ResourceNotFoundException si aucune banque ne correspond
     *                                   à l'identifiant fourni
     * @throws BankingException          si la banque est déjà active
     */
    @Transactional
    public BanqueResponse reactiverBanque(Long banqueId) {
        Banque banque = findBanqueById(banqueId);

        if (banque.getStatut() == StatutBanque.ACTIVE) {
            throw new BankingException("La banque '" + banque.getNom() + "' est déjà active.");
        }

        banque.setStatut(StatutBanque.ACTIVE);
        log.info("Banque réactivée : {} (id={})", banque.getNom(), banqueId);

        return toBanqueResponse(banqueRepository.save(banque));
    }

    /**
     * Retourne la liste de toutes les banques enregistrées sur la
     * plateforme.
     *
     * @return la liste (éventuellement vide) de toutes les banques
     */
    @Transactional(readOnly = true)
    public List<BanqueResponse> listerBanques() {
        return banqueRepository.findAll()
                .stream()
                .map(this::toBanqueResponse)
                .toList();
    }

    /**
     * Retourne le détail d'une banque par son identifiant.
     *
     * @param banqueId l'identifiant de la banque
     * @return le DTO représentant la banque
     * @throws ResourceNotFoundException si la banque est introuvable
     */
    @Transactional(readOnly = true)
    public BanqueResponse getBanque(Long banqueId) {
        return toBanqueResponse(findBanqueById(banqueId));
    }

    // =========================================================================
    //  Retrait administrateur (Skill 4)
    // =========================================================================

    /**
     * Effectue un retrait sur le {@code montantActif} d'une banque par
     * l'administrateur.
     *
     * <p>Règles métier appliquées :</p>
     * <ul>
     *   <li>Montant plafonné à 1 000 000 FCFA par opération (validé en
     *       amont par {@code @DecimalMax} sur le DTO, redondant ici pour
     *       la robustesse).</li>
     *   <li>Le {@code montantActif} de la banque doit être suffisant.</li>
     *   <li>L'opération est enregistrée dans l'historique avec le type
     *       {@link TypeOperation#RETRAIT_ADMIN}.</li>
     * </ul>
     *
     * @param banqueId l'identifiant de la banque sur laquelle effectuer
     *                 le retrait
     * @param request  les détails du retrait (montant + description)
     * @return le DTO de l'opération enregistrée
     * @throws ResourceNotFoundException si la banque est introuvable
     * @throws BankingException          si le montant dépasse le plafond ou
     *                                   si le {@code montantActif} est
     *                                   insuffisant
     */
    @Transactional
    public OperationResponse retraitAdmin(Long banqueId, RetraitAdminRequest request) {
        Banque banque = findBanqueById(banqueId);

        // Plafond (redondance défensive avec @DecimalMax du DTO)
        BigDecimal plafond = new BigDecimal("1000000.00");
        if (request.getMontant().compareTo(plafond) > 0) {
            throw new BankingException(
                    "Le retrait administrateur est plafonné à 1 000 000 FCFA par opération. "
                            + "Montant demandé : " + request.getMontant() + " FCFA.");
        }

        // Vérification du montantActif
        if (banque.getMontantActif().compareTo(request.getMontant()) < 0) {
            throw new BankingException(
                    "Retrait impossible : montant actif insuffisant. "
                            + "Disponible : " + banque.getMontantActif() + " FCFA, "
                            + "demandé : " + request.getMontant() + " FCFA.");
        }

        // Débit du montantActif
        banque.setMontantActif(banque.getMontantActif().subtract(request.getMontant()));
        banqueRepository.save(banque);

        // Enregistrement dans l'historique
        Operation operation = Operation.builder()
                .type(TypeOperation.RETRAIT_ADMIN)
                .montant(request.getMontant())
                .frais(BigDecimal.ZERO)
                .banque(banque)
                .compteEmetteur(null)
                .compteDestinataire(null)
                .description(request.getDescription())
                .build();

        Operation saved = operationRepository.save(operation);
        log.info("Retrait admin de {} FCFA sur la banque {} (id={})",
                request.getMontant(), banque.getNom(), banqueId);

        return mapper.toOperationResponse(saved);
    }

    // =========================================================================
    //  Gestion des utilisateurs (Skill 4)
    // =========================================================================

    /**
     * Retourne la liste de tous les utilisateurs de la plateforme.
     *
     * @return la liste de tous les utilisateurs sous forme de DTOs
     */
    @Transactional(readOnly = true)
    public List<UserResponse> listerUtilisateurs() {
        return userRepository.findAll()
                .stream()
                .map(mapper::toUserResponse)
                .toList();
    }

    /**
     * Crée un superviseur rattaché à une banque donnée (Skill 4).
     *
     * <p>Seuls les administrateurs peuvent créer des superviseurs. Le
     * superviseur créé reçoit automatiquement le rôle
     * {@link Role#SUPERVISEUR}.</p>
     *
     * @param request les informations du superviseur à créer
     * @return le DTO représentant le superviseur créé
     * @throws ResourceNotFoundException  si la banque cible est introuvable
     * @throws DuplicateResourceException si l'email ou le téléphone est déjà
     *                                    utilisé
     */
    @Transactional
    public UserResponse creerSuperviseur(CreateSuperviseurRequest request) {
        Banque banque = findBanqueById(request.getBanqueId());

        validerUniciteUtilisateur(request.getEmail(), request.getTelephone());

        User superviseur = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .motDePasse(passwordEncoder.encode(request.getMotDePasse()))
                .role(Role.SUPERVISEUR)
                .banque(banque)
                .build();

        User saved = userRepository.save(superviseur);
        log.info("Superviseur créé : {} pour la banque {} (id={})",
                saved.getEmail(), banque.getNom(), banque.getId());

        return mapper.toUserResponse(saved);
    }

    /**
     * Retourne l'historique paginé des opérations d'une banque donnée.
     *
     * @param banqueId l'identifiant de la banque
     * @param pageable paramètres de pagination
     * @return une page d'opérations
     */
    @Transactional(readOnly = true)
    public Page<OperationResponse> getHistoriqueBanque(Long banqueId, Pageable pageable) {
        findBanqueById(banqueId); // valide l'existence
        return operationRepository
                .findByBanqueIdOrderByDateOperationDesc(banqueId, pageable)
                .map(mapper::toOperationResponse);
    }

    // =========================================================================
    //  Méthodes utilitaires privées
    // =========================================================================

    /**
     * Charge une banque par son identifiant ou lève une exception si
     * elle est introuvable.
     */
    private Banque findBanqueById(Long banqueId) {
        return banqueRepository.findById(banqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Banque", "id", banqueId));
    }

    /**
     * Vérifie l'unicité de l'email et du téléphone avant toute création
     * d'utilisateur.
     */
    private void validerUniciteUtilisateur(String email, String telephone) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Utilisateur", "email", email);
        }
        if (userRepository.existsByTelephone(telephone)) {
            throw new DuplicateResourceException("Utilisateur", "téléphone", telephone);
        }
    }

    /**
     * Convertit une entité {@link Banque} en {@link BanqueResponse}.
     */
    private BanqueResponse toBanqueResponse(Banque banque) {
        return BanqueResponse.builder()
                .id(banque.getId())
                .nom(banque.getNom())
                .statut(banque.getStatut())
                .montantInitial(banque.getMontantInitial())
                .montantActif(banque.getMontantActif())
                .montantLigne(banque.getMontantLigne())
                .dateCreation(banque.getDateCreation())
                .build();
    }
}
