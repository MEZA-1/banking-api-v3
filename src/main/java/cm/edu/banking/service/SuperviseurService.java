package cm.edu.banking.service;

import cm.edu.banking.dto.request.ApprovisionnerAgentRequest;
import cm.edu.banking.dto.request.CreateAgentRequest;
import cm.edu.banking.dto.response.CompteBancaireResponse;
import cm.edu.banking.dto.response.OperationResponse;
import cm.edu.banking.dto.response.UserResponse;
import cm.edu.banking.exception.BankingException;
import cm.edu.banking.exception.DuplicateResourceException;
import cm.edu.banking.exception.ResourceNotFoundException;
import cm.edu.banking.model.Banque;
import cm.edu.banking.model.CompteBancaire;
import cm.edu.banking.model.Operation;
import cm.edu.banking.model.User;
import cm.edu.banking.model.enums.Role;
import cm.edu.banking.model.enums.StatutBanque;
import cm.edu.banking.model.enums.StatutCompte;
import cm.edu.banking.model.enums.TypeOperation;
import cm.edu.banking.repository.BanqueRepository;
import cm.edu.banking.repository.CompteBancaireRepository;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service applicatif regroupant toutes les opérations réservées au rôle
 * {@link Role#SUPERVISEUR} (Skill 5).
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>Création d'agents rattachés à la banque du superviseur.</li>
 *   <li>Consultation des agents et des clients de la banque.</li>
 *   <li>Suspension et réactivation de comptes (agents ou clients).</li>
 *   <li>Approvisionnement du compte d'un agent en liquidités depuis le
 *       {@code montantActif} de la banque, avec recalcul automatique du
 *       {@code montantLigne} (Skills 5 et 13).</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SuperviseurService {

    private final UserRepository userRepository;
    private final BanqueRepository banqueRepository;
    private final CompteBancaireRepository compteBancaireRepository;
    private final OperationRepository operationRepository;
    private final PasswordEncoder passwordEncoder;
    private final BankingMapper mapper;

    // =========================================================================
    //  Création d'agent (Skill 5)
    // =========================================================================

    /**
     * Crée un agent rattaché à la banque du superviseur appelant, et
     * génère automatiquement un compte bancaire (solde = 0) pour cet
     * agent.
     *
     * <p>Un agent doit toujours posséder un compte bancaire dès sa
     * création : ce compte lui sert de caisse pour les opérations de
     * dépôt et de retrait (Skill 6).</p>
     *
     * @param superviseur le superviseur connecté (détermine la banque)
     * @param request     les informations de l'agent à créer
     * @return le DTO représentant l'agent créé
     * @throws BankingException          si la banque du superviseur est
     *                                   suspendue
     * @throws DuplicateResourceException si l'email ou le téléphone est
     *                                   déjà utilisé
     */
    @Transactional
    public UserResponse creerAgent(User superviseur, CreateAgentRequest request) {
        Banque banque = superviseur.getBanque();
        verifierBanqueActive(banque);
        validerUniciteUtilisateur(request.getEmail(), request.getTelephone());

        // Création de l'utilisateur agent
        User agent = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .motDePasse(passwordEncoder.encode(request.getMotDePasse()))
                .role(Role.AGENT)
                .banque(banque)
                .build();

        User savedAgent = userRepository.save(agent);

        // Création automatique du compte bancaire de l'agent (solde = 0)
        CompteBancaire compteAgent = CompteBancaire.builder()
                .numeroCompte(genererNumeroCompte(banque))
                .solde(BigDecimal.ZERO)
                .statut(StatutCompte.ACTIF)
                .utilisateur(savedAgent)
                .banque(banque)
                .build();
        compteBancaireRepository.save(compteAgent);

        log.info("Agent créé : {} avec compte {} pour la banque {}",
                savedAgent.getEmail(), compteAgent.getNumeroCompte(), banque.getNom());

        return mapper.toUserResponse(savedAgent);
    }

    // =========================================================================
    //  Consultation (Skill 5)
    // =========================================================================

    /**
     * Retourne la liste des agents appartenant à la banque du superviseur.
     *
     * @param superviseur le superviseur connecté
     * @return la liste des agents de la banque
     */
    @Transactional(readOnly = true)
    public List<UserResponse> listerAgents(User superviseur) {
        Long banqueId = superviseur.getBanque().getId();
        return userRepository.findByBanqueIdAndRole(banqueId, Role.AGENT)
                .stream()
                .map(mapper::toUserResponse)
                .toList();
    }

    /**
     * Retourne la liste des clients dont le compte bancaire est rattaché
     * à la banque du superviseur.
     *
     * @param superviseur le superviseur connecté
     * @return la liste des clients de la banque
     */
    @Transactional(readOnly = true)
    public List<UserResponse> listerClients(User superviseur) {
        Long banqueId = superviseur.getBanque().getId();
        return compteBancaireRepository
                .findByBanqueIdAndUtilisateurRole(banqueId, Role.CLIENT)
                .stream()
                .map(c -> mapper.toUserResponse(c.getUtilisateur()))
                .toList();
    }

    // =========================================================================
    //  Suspension / Réactivation (Skill 5)
    // =========================================================================

    /**
     * Suspend le compte bancaire d'un utilisateur (agent ou client)
     * appartenant à la banque du superviseur.
     *
     * @param superviseur   le superviseur connecté
     * @param utilisateurId l'identifiant de l'utilisateur à suspendre
     * @return le DTO du compte mis à jour
     * @throws ResourceNotFoundException si l'utilisateur ou son compte est
     *                                   introuvable
     * @throws BankingException          si l'utilisateur n'appartient pas à
     *                                   la banque du superviseur, ou si le
     *                                   compte est déjà suspendu
     */
    @Transactional
    public CompteBancaireResponse suspendreCompte(User superviseur, Long utilisateurId) {
        CompteBancaire compte = getCompteDeUtilisateurDansBanque(
                superviseur.getBanque().getId(), utilisateurId);

        if (compte.getStatut() == StatutCompte.SUSPENDU) {
            throw new BankingException("Le compte est déjà suspendu.");
        }

        compte.setStatut(StatutCompte.SUSPENDU);
        log.info("Compte {} suspendu par le superviseur", compte.getNumeroCompte());
        return mapper.toCompteBancaireResponse(compteBancaireRepository.save(compte));
    }

    /**
     * Réactive le compte bancaire d'un utilisateur suspendu appartenant
     * à la banque du superviseur.
     *
     * @param superviseur   le superviseur connecté
     * @param utilisateurId l'identifiant de l'utilisateur à réactiver
     * @return le DTO du compte mis à jour
     * @throws ResourceNotFoundException si l'utilisateur ou son compte est
     *                                   introuvable
     * @throws BankingException          si le compte est déjà actif
     */
    @Transactional
    public CompteBancaireResponse reactiverCompte(User superviseur, Long utilisateurId) {
        CompteBancaire compte = getCompteDeUtilisateurDansBanque(
                superviseur.getBanque().getId(), utilisateurId);

        if (compte.getStatut() == StatutCompte.ACTIF) {
            throw new BankingException("Le compte est déjà actif.");
        }

        compte.setStatut(StatutCompte.ACTIF);
        log.info("Compte {} réactivé par le superviseur", compte.getNumeroCompte());
        return mapper.toCompteBancaireResponse(compteBancaireRepository.save(compte));
    }

    // =========================================================================
    //  Approvisionnement agent (Skill 5 + Skill 13)
    // =========================================================================

    /**
     * Approvisionne le compte d'un agent en prélevant le montant
     * demandé sur le {@code montantActif} de la banque.
     *
     * <p>Règles métier appliquées :</p>
     * <ul>
     *   <li>La banque ne doit pas être suspendue.</li>
     *   <li>Le compte de l'agent ne doit pas être suspendu.</li>
     *   <li>Le {@code montantActif} de la banque doit être suffisant.</li>
     *   <li>Après l'opération, le {@code montantLigne} de la banque est
     *       recalculé (Skill 13).</li>
     *   <li>L'opération est enregistrée dans l'historique (Skill 14).</li>
     * </ul>
     *
     * @param superviseur le superviseur connecté
     * @param agentId     l'identifiant de l'agent à approvisionner
     * @param request     le montant et la description optionnelle
     * @return le DTO de l'opération enregistrée
     * @throws BankingException si la banque ou le compte agent est suspendu,
     *                          ou si le {@code montantActif} est insuffisant
     */
    @Transactional
    public OperationResponse approvisionnerAgent(
            User superviseur, Long agentId, ApprovisionnerAgentRequest request) {

        Banque banque = superviseur.getBanque();
        verifierBanqueActive(banque);

        // Récupération et validation du compte de l'agent
        CompteBancaire compteAgent = getCompteDeUtilisateurDansBanque(banque.getId(), agentId);
        if (compteAgent.getStatut() == StatutCompte.SUSPENDU) {
            throw new BankingException(
                    "Approvisionnement impossible : le compte de l'agent est suspendu.");
        }

        BigDecimal montant = request.getMontant();

        // Vérification du montantActif de la banque
        if (banque.getMontantActif().compareTo(montant) < 0) {
            throw new BankingException(
                    "Approvisionnement impossible : montant actif de la banque insuffisant. "
                            + "Disponible : " + banque.getMontantActif() + " FCFA, "
                            + "demandé : " + montant + " FCFA.");
        }

        // --- Mouvements financiers (atomiques dans la transaction) ---

        // 1. Débit du montantActif de la banque
        banque.setMontantActif(banque.getMontantActif().subtract(montant));

        // 2. Crédit du compte agent
        compteAgent.setSolde(compteAgent.getSolde().add(montant));

        // 3. Recalcul du montantLigne (Skill 13)
        banqueRepository.save(banque);
        compteBancaireRepository.save(compteAgent);
        recalculerMontantLigne(banque);

        // 4. Enregistrement dans l'historique (Skill 14)
        Operation operation = Operation.builder()
                .type(TypeOperation.APPROVISIONNEMENT_AGENT)
                .montant(montant)
                .frais(BigDecimal.ZERO)
                .compteEmetteur(null)
                .compteDestinataire(compteAgent)
                .banque(banque)
                .description(request.getDescription())
                .build();

        Operation saved = operationRepository.save(operation);
        log.info("Approvisionnement de {} FCFA sur le compte agent {} (banque {})",
                montant, compteAgent.getNumeroCompte(), banque.getNom());

        return mapper.toOperationResponse(saved);
    }

    /**
     * Retourne l'historique paginé des opérations de la banque du
     * superviseur.
     *
     * @param superviseur le superviseur connecté
     * @param pageable    paramètres de pagination
     * @return une page d'opérations
     */
    @Transactional(readOnly = true)
    public Page<OperationResponse> getHistoriqueBanque(User superviseur, Pageable pageable) {
        Long banqueId = superviseur.getBanque().getId();
        return operationRepository
                .findByBanqueIdOrderByDateOperationDesc(banqueId, pageable)
                .map(mapper::toOperationResponse);
    }

    // =========================================================================
    //  Méthodes utilitaires privées
    // =========================================================================

    /**
     * Vérifie que la banque est active, sinon lève une
     * {@link BankingException}.
     */
    private void verifierBanqueActive(Banque banque) {
        if (banque.getStatut() == StatutBanque.SUSPENDUE) {
            throw new BankingException(
                    "Opération impossible : la banque '" + banque.getNom()
                            + "' est suspendue (Skill 15).");
        }
    }

    /**
     * Vérifie l'unicité de l'email et du téléphone.
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
     * Récupère le compte bancaire d'un utilisateur en vérifiant qu'il
     * appartient bien à la banque du superviseur.
     *
     * @param banqueId      identifiant de la banque du superviseur
     * @param utilisateurId identifiant de l'utilisateur cible
     * @return le compte bancaire trouvé
     * @throws ResourceNotFoundException si l'utilisateur ou son compte
     *                                   est introuvable
     * @throws BankingException          si l'utilisateur n'appartient pas
     *                                   à la bonne banque
     */
    private CompteBancaire getCompteDeUtilisateurDansBanque(Long banqueId, Long utilisateurId) {
        CompteBancaire compte = compteBancaireRepository
                .findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CompteBancaire", "utilisateurId", utilisateurId));

        if (!compte.getBanque().getId().equals(banqueId)) {
            throw new BankingException(
                    "Cet utilisateur n'appartient pas à votre banque.");
        }
        return compte;
    }

    /**
     * Recalcule et persiste le {@code montantLigne} de la banque donnée
     * conformément à la Skill 13 : somme de tous les soldes clients et
     * agents de la banque.
     *
     * @param banque la banque dont le {@code montantLigne} doit être mis
     *               à jour
     */
    private void recalculerMontantLigne(Banque banque) {
        BigDecimal montantLigne = compteBancaireRepository
                .calculerMontantLigne(banque.getId());
        banque.setMontantLigne(montantLigne);
        banqueRepository.save(banque);
        log.debug("montantLigne recalculé pour la banque {} : {} FCFA",
                banque.getNom(), montantLigne);
    }

    /**
     * Génère un numéro de compte bancaire unique basé sur un code banque
     * et un timestamp en millisecondes.
     *
     * <p>Format : {@code BK<banqueId_5digits>-<yyyyMMddHHmmss>-<ms>}
     * Exemple : {@code BK00001-20240115143022-789}</p>
     *
     * @param banque la banque à laquelle le compte sera rattaché
     * @return un numéro de compte unique (unicité garantie par la contrainte
     *         {@code @UniqueConstraint} sur la table {@code comptes_bancaires})
     */
    private String genererNumeroCompte(Banque banque) {
        String codeBanque = String.format("BK%05d", banque.getId());
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long ms = System.currentTimeMillis() % 1000;
        return codeBanque + "-" + timestamp + "-" + ms;
    }
}
