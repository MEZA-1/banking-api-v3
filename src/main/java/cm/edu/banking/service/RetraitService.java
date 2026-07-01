package cm.edu.banking.service;

import cm.edu.banking.dto.request.ConfirmerRetraitRequest;
import cm.edu.banking.dto.request.InitierRetraitRequest;
import cm.edu.banking.dto.response.OperationResponse;
import cm.edu.banking.dto.response.RetraitEnAttenteResponse;
import cm.edu.banking.exception.BankingException;
import cm.edu.banking.exception.ResourceNotFoundException;
import cm.edu.banking.model.*;
import cm.edu.banking.model.enums.StatutRetrait;
import cm.edu.banking.model.enums.Role;
import cm.edu.banking.model.enums.StatutBanque;
import cm.edu.banking.model.enums.StatutCompte;
import cm.edu.banking.model.enums.TypeOperation;
import cm.edu.banking.repository.BanqueRepository;
import cm.edu.banking.repository.CompteBancaireRepository;
import cm.edu.banking.repository.OperationRepository;
import cm.edu.banking.repository.RetraitEnAttenteRepository;
import cm.edu.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Service applicatif gérant le cycle de vie complet d'un retrait client
 * en deux étapes sécurisées (Skill 10 enrichi).
 *
 * <h2>Étape 1 — Initiation ({@link #initierRetrait})</h2>
 * <ol>
 *   <li>Valider les comptes agent et client (banque active, comptes actifs,
 *       rôle CLIENT, solde suffisant).</li>
 *   <li>Générer un OTP à 6 chiffres via {@link SecureRandom}.</li>
 *   <li>Persister une instance de {@link RetraitEnAttente} avec statut
 *       {@link StatutRetrait#EN_ATTENTE} et expiration dans 5 minutes.</li>
 *   <li>Envoyer le code OTP au client via
 *       {@link NotificationService#envoyerOtpRetrait}.</li>
 * </ol>
 *
 * <h2>Étape 2 — Confirmation ({@link #confirmerRetrait})</h2>
 * <ol>
 *   <li>Charger la demande par {@code demandeId} et vérifier son statut
 *       ({@link StatutRetrait#EN_ATTENTE}) et l'expiration de l'OTP.</li>
 *   <li>Comparer le {@code codeOtp} fourni avec celui stocké.</li>
 *   <li>Vérifier le {@code motDePasseClient} via BCrypt.</li>
 *   <li>Si les deux facteurs sont valides : exécuter le retrait (mêmes
 *       règles financières que {@link TransactionService#effectuerRetrait}).</li>
 *   <li>Mettre à jour le statut de la demande à
 *       {@link StatutRetrait#CONFIRME}.</li>
 * </ol>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetraitService {

    private static final BigDecimal TAUX_FRAIS = new BigDecimal("0.02");
    private static final int MAX_TENTATIVES = 3;
    private static final BigDecimal MONTANT_MIN = new BigDecimal("1000");
    private static final BigDecimal MONTANT_MAX = new BigDecimal("1000000");

    private final RetraitEnAttenteRepository retraitEnAttenteRepository;
    private final CompteBancaireRepository   compteBancaireRepository;
    private final BanqueRepository           banqueRepository;
    private final OperationRepository        operationRepository;
    private final UserRepository             userRepository;
    private final PasswordEncoder            passwordEncoder;
    private final NotificationService        notificationService;
    private final BankingMapper              mapper;

    // =========================================================================
    //  ÉTAPE 1 — Initiation
    // =========================================================================

    /**
     * Initie un retrait : valide les règles métier, génère un OTP et
     * notifie le client.
     *
     * @param agent   l'agent connecté qui traite l'opération
     * @param request les paramètres du retrait (clientId, montant, description)
     * @return un DTO contenant l'identifiant de la demande, le récapitulatif
     *         financier et l'OTP (à des fins de démonstration)
     * @throws BankingException          si une règle métier est violée
     * @throws ResourceNotFoundException si le client ou son compte est introuvable
     */
    @Transactional
    public RetraitEnAttenteResponse initierRetrait(User agent, InitierRetraitRequest request) {

        // ── 1. Chargement et validation du compte agent ────────────────
        CompteBancaire compteAgent = getCompteOuErreur(agent.getId(), "Agent");
        verifierBanqueActive(compteAgent.getBanque());
        verifierCompteActif(compteAgent, "Agent");

        // ── 2. Chargement et validation du compte client ───────────────
        CompteBancaire compteClient = compteBancaireRepository
                .findByUtilisateurId(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CompteBancaire", "utilisateur (Client) id", request.getClientId()));

        verifierBanqueActive(compteClient.getBanque());
        verifierCompteActif(compteClient, "Client");

        User client = compteClient.getUtilisateur();
        if (client.getRole() != Role.CLIENT) {
            throw new BankingException(
                    "Le compte destinataire ne correspond pas à un client.");
        }

        //verifier que le montant est dans les limites autorisées
        BigDecimal montant      = request.getMontant();
        
        //verifier que le montant est positif et supérieur à un minimum (ex: 1000 FCFA)
        if (montant.compareTo(BigDecimal.ZERO) <= 0 && montant.compareTo(MONTANT_MIN) < 0) {
			throw new BankingException(
					"Le montant du retrait doit être supérieur à "+MONTANT_MIN+" FCFA.");
		}
        
        //verifier que le montant ne dépasse pas un maximum (ex: 1 000 000 FCFA)
       
       if (montant.compareTo(MONTANT_MAX) > 0) {
    	   			throw new BankingException(
					"Le montant du retrait ne peut pas dépasser "+ MONTANT_MAX+" FCFA.");
       }
       
       //verifier que le client et l'agent ne sont pas les mêmes utilisateurs
        if (agent.getId().equals(client.getId())) {
        				throw new BankingException(
					"L'agent ne peut pas initier un retrait pour lui-même.");
        }
        
        //verifier que le client et l'agent sont dans la même banque
        if (!compteClient.getBanque().getId().equals(compteAgent.getBanque().getId())) {
						throw new BankingException(
					"L'agent et le client doivent appartenir à la même banque.");
		}
        
        // ── 3. Calcul des montants ─────────────────────────────────────
        BigDecimal frais        = montant.multiply(TAUX_FRAIS).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montantTotal = montant.add(frais);

        // ── 4. Vérification du solde client ────────────────────────────
        if (compteClient.getSolde().compareTo(montantTotal) < 0) {
            throw new BankingException(
                    String.format("Solde insuffisant. Requis : %s FCFA (retrait + frais 2%%), " +
                                  "solde disponible : %s FCFA.",
                            montantTotal, compteClient.getSolde()));
        }
       
        
        
        // ── 5. Vérifier qu'aucune demande EN_ATTENTE n'existe déjà ────
        retraitEnAttenteRepository
                .findByCompteClientIdAndStatut(compteClient.getId(), StatutRetrait.EN_ATTENTE)
                .ifPresent(existante -> {
                    throw new BankingException(
                            "Une demande de retrait est déjà en cours pour ce client " +
                            "(demande #" + existante.getId() + "). " +
                            "Attendez sa confirmation ou son expiration.");
                });

        // ── 6. Génération de l'OTP ─────────────────────────────────────
        String codeOtp = genererOtp();
        LocalDateTime expiration = LocalDateTime.now()
                .plusMinutes(RetraitEnAttente.OTP_VALIDITY_MINUTES);

        // ── 7. Persistance de la demande ──────────────────────────────
        RetraitEnAttente demande = RetraitEnAttente.builder()
                .compteClient(compteClient)
                .compteAgent(compteAgent)
                .montant(montant)
                .frais(frais)
                .montantDebite(montantTotal)
                .codeOtp(codeOtp)
                .expirationOtp(expiration)
                .statut(StatutRetrait.EN_ATTENTE)
                .description(request.getDescription())
                .build();

        RetraitEnAttente saved = retraitEnAttenteRepository.save(demande);

        // ── 8. Notification au client ─────────────────────────────────
        notificationService.envoyerOtpRetrait(
                client, compteClient, montant, frais, montantTotal, codeOtp);

        log.info("Retrait initié : demande#{} | client:{} | montant:{} | OTP:{} | expire:{}",
                saved.getId(), client.getEmail(), montant, codeOtp, expiration);

        // ── 9. Construction de la réponse ─────────────────────────────
        return RetraitEnAttenteResponse.builder()
                .demandeId(saved.getId())
                .clientId(client.getId())
                .nomClient(client.getPrenom() + " " + client.getNom())
                .emailClient(client.getEmail())
                .numeroCompteClient(compteClient.getNumeroCompte())
                .soldeClientAvant(compteClient.getSolde())
                .nomAgent(agent.getPrenom() + " " + agent.getNom())
                .numeroCompteAgent(compteAgent.getNumeroCompte())
                .montant(montant)
                .frais(frais)
                .montantDebite(montantTotal)
                .codeOtpPreview(codeOtp)   // ⚠ À supprimer en production
                .expirationOtp(expiration)
                .statut(StatutRetrait.EN_ATTENTE)
                .message(String.format(
                        "Demande créée. Un code OTP a été envoyé à %s (%s). " +
                        "Le client a %d minutes pour confirmer.",
                        client.getEmail(), masquerTel(client.getTelephone()),
                        RetraitEnAttente.OTP_VALIDITY_MINUTES))
                .dateCreation(saved.getDateCreation())
                .build();
    }

    // =========================================================================
    //  ÉTAPE 2 — Confirmation
    // =========================================================================

    /**
     * Confirme un retrait en attente après vérification du code OTP et
     * du mot de passe du client.
     *
     * @param agent   l'agent connecté (doit être le même que lors de l'initiation)
     * @param request la confirmation (demandeId, codeOtp, motDePasseClient)
     * @return le DTO de l'opération financière exécutée
     * @throws BankingException          si l'OTP est invalide/expiré, le mot de
     *                                   passe incorrect ou le statut invalide
     * @throws ResourceNotFoundException si la demande est introuvable
     */
    @Transactional
    public OperationResponse confirmerRetrait(User agent, ConfirmerRetraitRequest request) {

        // ── 1. Charger la demande ─────────────────────────────────────
        RetraitEnAttente demande = retraitEnAttenteRepository
                .findById(request.getDemandeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RetraitEnAttente", "id", request.getDemandeId()));

        // ── 2. Vérifier le statut ─────────────────────────────────────
        if (demande.getStatut() != StatutRetrait.EN_ATTENTE) {
            throw new BankingException(
                    "Cette demande de retrait ne peut plus être confirmée. " +
                    "Statut actuel : " + demande.getStatut().name() + ".");
        }

        // ── 3. Vérifier l'expiration de l'OTP ────────────────────────
        if (!demande.isOtpValide()) {
            demande.setStatut(StatutRetrait.EXPIRE);
            demande.setDateMiseAJour(LocalDateTime.now());
            retraitEnAttenteRepository.save(demande);
            throw new BankingException(
                    "Le code OTP a expiré (validité : " +
                    RetraitEnAttente.OTP_VALIDITY_MINUTES + " minutes). " +
                    "Veuillez initier un nouveau retrait.");
        }

        // ── 4. Limiter les tentatives (anti-brute-force) ───────────────
        if (demande.getTentatives() >= MAX_TENTATIVES) {
            demande.setStatut(StatutRetrait.ANNULE);
            demande.setDateMiseAJour(LocalDateTime.now());
            retraitEnAttenteRepository.save(demande);
            throw new BankingException(
                    "Demande annulée : nombre maximum de tentatives atteint (" +
                    MAX_TENTATIVES + ").");
        }

        // ── 5. Vérifier le code OTP ───────────────────────────────────
        if (!demande.getCodeOtp().equals(request.getCodeOtp())) {
            int tentativesRestantes = MAX_TENTATIVES - demande.getTentatives() - 1;
            demande.setTentatives(demande.getTentatives() + 1);
            demande.setDateMiseAJour(LocalDateTime.now());
            retraitEnAttenteRepository.save(demande);
            throw new BankingException(
                    "Code OTP incorrect. " +
                    (tentativesRestantes > 0
                            ? tentativesRestantes + " tentative(s) restante(s)."
                            : "Dernière tentative utilisée — demande annulée."));
        }

        // ── 6. Vérifier le mot de passe du client ─────────────────────
        User client = demande.getCompteClient().getUtilisateur();
        if (!passwordEncoder.matches(request.getMotDePasseClient(), client.getMotDePasse())) {
            int tentativesRestantes = MAX_TENTATIVES - demande.getTentatives() - 1;
            demande.setTentatives(demande.getTentatives() + 1);
            demande.setDateMiseAJour(LocalDateTime.now());
            retraitEnAttenteRepository.save(demande);
            throw new BankingException(
                    "Mot de passe client incorrect. " +
                    (tentativesRestantes > 0
                            ? tentativesRestantes + " tentative(s) restante(s)."
                            : "Dernière tentative utilisée — demande annulée."));
        }

        // ── 7. Re-valider les comptes et le solde (état temps réel) ───
        CompteBancaire compteClient = demande.getCompteClient();
        CompteBancaire compteAgent  = demande.getCompteAgent();
        Banque banque = compteAgent.getBanque();

        verifierBanqueActive(banque);
        verifierCompteActif(compteClient, "Client");
        verifierCompteActif(compteAgent,  "Agent");

        if (compteClient.getSolde().compareTo(demande.getMontantDebite()) < 0) {
            throw new BankingException(
                    "Solde client insuffisant au moment de la confirmation. " +
                    "Solde actuel : " + compteClient.getSolde() + " FCFA.");
        }

        // ── 8. Exécution des mouvements financiers (atomique) ─────────
        // Débit client (montant + frais)
        compteClient.setSolde(compteClient.getSolde().subtract(demande.getMontantDebite()));
        // Crédit agent (montant seul, sans les frais)
        compteAgent.setSolde(compteAgent.getSolde().add(demande.getMontant()));

        compteBancaireRepository.save(compteClient);
        compteBancaireRepository.save(compteAgent);

        // Frais → montantActif de la banque
        banque.setMontantActif(banque.getMontantActif().add(demande.getFrais()));
        banqueRepository.save(banque);

        // Recalcul montantLigne (Skill 13)
        BigDecimal montantLigne = compteBancaireRepository.calculerMontantLigne(banque.getId());
        banque.setMontantLigne(montantLigne);
        banqueRepository.save(banque);

        // ── 9. Historique des opérations (Skill 14) ───────────────────
        Operation operation = Operation.builder()
                .type(TypeOperation.RETRAIT)
                .montant(demande.getMontant())
                .frais(demande.getFrais())
                .compteEmetteur(compteClient)
                .compteDestinataire(compteAgent)
                .banque(banque)
                .description(demande.getDescription())
                .build();
        Operation savedOp = operationRepository.save(operation);

        // ── 10. Marquer la demande comme confirmée ────────────────────
        demande.setStatut(StatutRetrait.CONFIRME);
        demande.setDateMiseAJour(LocalDateTime.now());
        retraitEnAttenteRepository.save(demande);

        log.info("Retrait confirmé : demande#{} | client:{} | montant:{} | frais:{}",
                demande.getId(), client.getEmail(), demande.getMontant(), demande.getFrais());

        return mapper.toOperationResponse(savedOp);
    }

    // =========================================================================
    //  Méthodes utilitaires privées
    // =========================================================================

    /**
     * Génère un code OTP aléatoire à 6 chiffres via {@link SecureRandom}.
     *
     * @return une chaîne de 6 chiffres (avec zéros de remplissage si nécessaire)
     */
    private String genererOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100_000 + random.nextInt(900_000); // [100000, 999999]
        return String.valueOf(otp);
    }

    /**
     * Masque partiellement un numéro de téléphone pour l'affichage
     * dans les messages. Ex : {@code 6XXXXXXXX} → {@code 6XXXXX789}.
     */
    private String masquerTel(String tel) {
        if (tel == null || tel.length() < 4) return "****";
        return tel.substring(0, tel.length() - 3).replaceAll("\\d", "*")
               + tel.substring(tel.length() - 3);
    }

    /** Charge le compte d'un utilisateur ou lève une ResourceNotFoundException. */
    private CompteBancaire getCompteOuErreur(Long utilisateurId, String role) {
        return compteBancaireRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CompteBancaire", "utilisateur (" + role + ") id", utilisateurId));
    }

    /** Vérifie que la banque est active (Skill 15). */
    private void verifierBanqueActive(Banque banque) {
        if (banque.getStatut() == StatutBanque.SUSPENDUE) {
            throw new BankingException(
                    "Opération bloquée : la banque '" + banque.getNom() +
                    "' est suspendue (Skill 15).");
        }
    }

    /** Vérifie que le compte est actif (Skill 15). */
    private void verifierCompteActif(CompteBancaire compte, String role) {
        if (compte.getStatut() == StatutCompte.SUSPENDU) {
            throw new BankingException(
                    "Opération bloquée : le compte " + role.toLowerCase() +
                    " (" + compte.getNumeroCompte() + ") est suspendu (Skill 15).");
        }
    }
}