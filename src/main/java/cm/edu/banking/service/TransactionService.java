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
import cm.edu.banking.model.enums.Role;
import cm.edu.banking.model.enums.StatutBanque;
import cm.edu.banking.model.enums.StatutCompte;
import cm.edu.banking.model.enums.TypeOperation;
import cm.edu.banking.repository.BanqueRepository;
import cm.edu.banking.repository.CompteBancaireRepository;
import cm.edu.banking.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Moteur central de transactions financières de la plateforme bancaire
 * multi-banques CEMAC.
 *
 * <p>Ce service implémente la totalité du cycle de vie des opérations
 * financières défini par les Skills 8 à 15 :</p>
 * <ul>
 *   <li><strong>Skill 8</strong> : création de compte bancaire (solde = 0,
 *       numéro unique généré).</li>
 *   <li><strong>Skill 9</strong> : dépôt agent → client, sans frais.</li>
 *   <li><strong>Skill 10</strong> : retrait client via agent, frais 2 %.</li>
 *   <li><strong>Skill 11</strong> : transfert interne (même banque), frais 2 %.</li>
 *   <li><strong>Skill 12</strong> : transfert interbancaire, frais 4 %.</li>
 *   <li><strong>Skill 13</strong> : recalcul automatique du
 *       {@code montantLigne} après chaque opération.</li>
 *   <li><strong>Skill 14</strong> : enregistrement systématique de chaque
 *       opération dans l'historique ({@link Operation}).</li>
 *   <li><strong>Skill 15</strong> : blocage de toute opération sur un
 *       compte ou une banque suspendus.</li>
 * </ul>
 *
 * <p><strong>Principe de garantie d'atomicité :</strong> chaque méthode
 * publique est annotée {@link Transactional}. Si une étape échoue
 * (validation, débit, crédit, enregistrement historique), la transaction
 * entière est annulée ({@code ROLLBACK}) et aucun effet partiel n'est
 * persisté.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    /** Taux de frais pour le retrait et le transfert interne (2 %). */
    private static final BigDecimal TAUX_FRAIS_STANDARD =
            new BigDecimal("0.02");

    /** Taux de frais pour le transfert interbancaire (4 %). */
    private static final BigDecimal TAUX_FRAIS_INTERBANCAIRE =
            new BigDecimal("0.04");
    private static final BigDecimal MONTANT_MIN_DEPOSIT = new BigDecimal("1000");
    private static final BigDecimal MONTANT_MAX_DEPOSIT = new BigDecimal("1000000");

    private final CompteBancaireRepository compteBancaireRepository;
    private final BanqueRepository banqueRepository;
    private final OperationRepository operationRepository;
    private final BankingMapper mapper;

    // =========================================================================
    //  Skill 8 — Création de compte bancaire
    // =========================================================================

    /**
     * Crée un compte bancaire pour un utilisateur (client ou agent).
     *
     * <p>Règles métier :</p>
     * <ul>
     *   <li>L'utilisateur ne doit pas déjà posséder un compte bancaire.</li>
     *   <li>La banque cible doit être {@link StatutBanque#ACTIVE}.</li>
     *   <li>Le solde initial est obligatoirement {@link BigDecimal#ZERO}.</li>
     *   <li>Le numéro de compte est généré automatiquement.</li>
     * </ul>
     *
     * @param utilisateur l'utilisateur qui crée son compte
     * @param request     l'identifiant de la banque choisie
     * @return le DTO représentant le compte créé
     * @throws BankingException          si l'utilisateur possède déjà un
     *                                   compte ou si la banque est suspendue
     * @throws ResourceNotFoundException si la banque est introuvable
     */
    @Transactional
    public CompteBancaireResponse creerCompteBancaire(
            User utilisateur, CreateCompteBancaireRequest request) {

        // Unicité : un utilisateur = un seul compte
        if (compteBancaireRepository.existsByUtilisateurId(utilisateur.getId())) {
            throw new BankingException(
                    "Vous possédez déjà un compte bancaire. "
                            + "Un seul compte est autorisé par utilisateur.");
        }

        Banque banque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Banque", "id", request.getBanqueId()));

        verifierBanqueActive(banque);

        CompteBancaire compte = CompteBancaire.builder()
                .numeroCompte(genererNumeroCompte(banque))
                .solde(BigDecimal.ZERO)
                .statut(StatutCompte.ACTIF)
                .utilisateur(utilisateur)
                .banque(banque)
                .build();

        CompteBancaire saved = compteBancaireRepository.save(compte);
        log.info("Compte bancaire créé : {} pour {} (banque {})",
                saved.getNumeroCompte(), utilisateur.getEmail(), banque.getNom());

        return mapper.toCompteBancaireResponse(saved);
    }

    // =========================================================================
    //  Skill 9 — Dépôt (agent → client, sans frais)
    // =========================================================================

    /**
     * Effectue un dépôt du compte de l'agent vers le compte du client.
     *
     * <p>Flux financier :</p>
     * <pre>
     *   compteAgent  -= montant
     *   compteClient += montant
     *   frais         = 0
     * </pre>
     *
     * <p>Validations :</p>
     * <ol>
     *   <li>La banque de l'agent n'est pas suspendue.</li>
     *   <li>Le compte de l'agent n'est pas suspendu.</li>
     *   <li>Le compte du client n'est pas suspendu.</li>
     *   <li>La banque du client n'est pas suspendue.</li>
     *   <li>{@code soldeAgent >= montant}.</li>
     * </ol>
     *
     * <p>Après l'opération, le {@code montantLigne} des banques impliquées
     * est recalculé (Skill 13).</p>
     *
     * @param agent   l'agent connecté qui effectue le dépôt
     * @param request les paramètres du dépôt (clientId, montant,
     *                description)
     * @return le DTO de l'opération enregistrée dans l'historique
     * @throws BankingException          si une règle métier est violée
     * @throws ResourceNotFoundException si le client ou son compte est
     *                                   introuvable
     */
    @Transactional
    public OperationResponse effectuerDepot(User agent, DepotRequest request) {

        // --- Chargement et validations ---
        CompteBancaire compteAgent = getCompteOuErreur(agent.getId(), "Agent");
        verifierBanqueActive(compteAgent.getBanque());
        verifierCompteActif(compteAgent, "Agent");

        CompteBancaire compteClient = getCompteParUtilisateurId(
                request.getClientId(), "Client");
        verifierBanqueActive(compteClient.getBanque());
        verifierCompteActif(compteClient, "Client");

        // Validation du rôle : le destinataire doit être un CLIENT
        if (compteClient.getUtilisateur().getRole() != Role.CLIENT) {
            throw new BankingException(
                    "Le compte destinataire ne correspond pas à un client.");
        }

        BigDecimal montant = request.getMontant();
        
         if (montant.compareTo(BigDecimal.ZERO) <= 0 && montant.compareTo(MONTANT_MIN_DEPOSIT) < 0) {
			throw new BankingException(
					"Le montant du depot doit être supérieur à"+MONTANT_MIN_DEPOSIT+"  FCFA.");
		}
         //verifier que le montant ne depasse pas le maximum
         
         if(montant.compareTo(MONTANT_MAX_DEPOSIT) > 0) {
        	 throw new BankingException(
					 "Le montant du depot ne doit pas dépasser "+MONTANT_MAX_DEPOSIT+"  FCFA.");
         }
         //verifier que le client et l'gent sont de la meme banque
         if(!compteClient.getBanque().getId().equals(compteAgent.getBanque().getId())) {
			 throw new BankingException("Le client et l'agent doivent appartenir à la même banque pour effectuer un dépôt.");
			 		 }
         
        // Validation du solde agent
        if (compteAgent.getSolde().compareTo(montant) < 0) {
            throw new BankingException(
                    "Dépôt impossible : solde agent insuffisant. "
                            + "Solde disponible : " + compteAgent.getSolde()
                            + " FCFA, montant demandé : " + montant + " FCFA.");
        }

        // --- Mouvements financiers (atomiques) ---
        compteAgent.setSolde(compteAgent.getSolde().subtract(montant));
        compteClient.setSolde(compteClient.getSolde().add(montant));

        compteBancaireRepository.save(compteAgent);
        compteBancaireRepository.save(compteClient);

        // Recalcul du montantLigne pour la banque de l'agent
        recalculerMontantLigne(compteAgent.getBanque());
        // Si le client est dans une banque différente, recalculer aussi
        if (!compteClient.getBanque().getId()
                .equals(compteAgent.getBanque().getId())) {
            recalculerMontantLigne(compteClient.getBanque());
        }

        // --- Historique (Skill 14) ---
        Operation operation = Operation.builder()
                .type(TypeOperation.DEPOT)
                .montant(montant)
                .frais(BigDecimal.ZERO)
                .compteEmetteur(compteAgent)
                .compteDestinataire(compteClient)
                .banque(compteAgent.getBanque())
                .description(request.getDescription())
                .build();

        Operation saved = operationRepository.save(operation);
        log.info("DEPOT {} FCFA : agent {} → client {} (banque {})",
                montant, compteAgent.getNumeroCompte(),
                compteClient.getNumeroCompte(), compteAgent.getBanque().getNom());

        return mapper.toOperationResponse(saved);
    }
/*
    // =========================================================================
    //  Skill 10 — Retrait (client via agent, frais 2 %)
    // =========================================================================

    /**
     * Effectue un retrait du compte client via le compte de l'agent,
     * avec application de frais de 2 %.
     *
     * <p>Flux financier :</p>
     * <pre>
     *   frais               = montant × 2 %
     *   montantDébitéClient = montant + frais
     *   compteClient       -= montantDébitéClient
     *   compteAgent        += montant
     *   banque.montantActif += frais
     * </pre>
     *
     * <p>Validations :</p>
     * <ol>
     *   <li>La banque de l'agent n'est pas suspendue.</li>
     *   <li>Le compte de l'agent n'est pas suspendu.</li>
     *   <li>Le compte du client n'est pas suspendu.</li>
     *   <li>La banque du client n'est pas suspendue.</li>
     *   <li>{@code soldeClient >= montant + frais}.</li>
     * </ol>
     *
     * @param agent   l'agent connecté qui traite le retrait
     * @param request les paramètres du retrait (clientId, montant,
     *                description)
     * @return le DTO de l'opération enregistrée dans l'historique
     * @throws BankingException          si une règle métier est violée
     * @throws ResourceNotFoundException si le client ou son compte est
     *                                   introuvable
     */
   /* @Transactional
    public OperationResponse effectuerRetrait(User agent, RetraitRequest request) {

        // --- Chargement et validations ---
        CompteBancaire compteAgent = getCompteOuErreur(agent.getId(), "Agent");
        verifierBanqueActive(compteAgent.getBanque());
        verifierCompteActif(compteAgent, "Agent");

        CompteBancaire compteClient = getCompteParUtilisateurId(
                request.getClientId(), "Client");
        verifierBanqueActive(compteClient.getBanque());
        verifierCompteActif(compteClient, "Client");

        if (compteClient.getUtilisateur().getRole() != Role.CLIENT) {
            throw new BankingException(
                    "Le compte concerné ne correspond pas à un client.");
        }

        BigDecimal montant = request.getMontant();
        BigDecimal frais = calculerFrais(montant, TAUX_FRAIS_STANDARD);
        BigDecimal montantDebiteClient = montant.add(frais);

        // Validation du solde client (montant + frais)
        if (compteClient.getSolde().compareTo(montantDebiteClient) < 0) {
            throw new BankingException(
                    "Solde insuffisant. "
                            + "Montant requis (retrait + frais 2 %) : "
                            + montantDebiteClient + " FCFA, "
                            + "solde disponible : " + compteClient.getSolde()
                            + " FCFA.");
        }

        // --- Mouvements financiers (atomiques) ---
        // 1. Débit client (montant + frais)
        compteClient.setSolde(compteClient.getSolde().subtract(montantDebiteClient));

        // 2. Crédit agent (montant uniquement — sans les frais)
        compteAgent.setSolde(compteAgent.getSolde().add(montant));

        compteBancaireRepository.save(compteClient);
        compteBancaireRepository.save(compteAgent);

        // 3. Frais → montantActif de la banque de l'agent
        Banque banque = compteAgent.getBanque();
        banque.setMontantActif(banque.getMontantActif().add(frais));
        banqueRepository.save(banque);

        // 4. Recalcul du montantLigne (Skill 13)
        recalculerMontantLigne(banque);
        if (!compteClient.getBanque().getId().equals(banque.getId())) {
            recalculerMontantLigne(compteClient.getBanque());
        }

        // --- Historique (Skill 14) ---
        Operation operation = Operation.builder()
                .type(TypeOperation.RETRAIT)
                .montant(montant)
                .frais(frais)
                .compteEmetteur(compteClient)
                .compteDestinataire(compteAgent)
                .banque(banque)
                .description(request.getDescription())
                .build();

        Operation saved = operationRepository.save(operation);
        log.info("RETRAIT {} FCFA (frais={}) : client {} → agent {} (banque {})",
                montant, frais, compteClient.getNumeroCompte(),
                compteAgent.getNumeroCompte(), banque.getNom());

        return mapper.toOperationResponse(saved);
    }*/

    // =========================================================================
    //  Skills 11 & 12 — Transfert interne (2 %) et interbancaire (4 %)
    // =========================================================================

    /**
     * Effectue un transfert de fonds depuis le compte du client émetteur
     * vers le compte identifié par le numéro fourni.
     *
     * <p>Le type de transfert (interne ou interbancaire) est déterminé
     * automatiquement en comparant les identifiants de banque des deux
     * comptes :</p>
     * <ul>
     *   <li>Même banque → {@link TypeOperation#TRANSFERT_INTERNE},
     *       frais 2 %.</li>
     *   <li>Banques différentes → {@link TypeOperation#TRANSFERT_INTERBANCAIRE},
     *       frais 4 %.</li>
     * </ul>
     *
     * <p>Flux financier (commun aux deux types) :</p>
     * <pre>
     *   frais                  = montant × taux
     *   compteEmetteur        -= montant + frais
     *   compteDestinataire    += montant
     *   banqueEmettrice.montantActif += frais
     * </pre>
     *
     * <p>Validations :</p>
     * <ol>
     *   <li>L'émetteur ne peut pas transférer vers son propre compte.</li>
     *   <li>La banque de l'émetteur n'est pas suspendue.</li>
     *   <li>Le compte émetteur n'est pas suspendu.</li>
     *   <li>La banque du destinataire n'est pas suspendue.</li>
     *   <li>Le compte destinataire n'est pas suspendu.</li>
     *   <li>{@code soldeEmetteur >= montant + frais}.</li>
     * </ol>
     *
     * @param client  le client connecté qui initie le transfert
     * @param request le numéro de compte destinataire, le montant et la
     *                description optionnelle
     * @return le DTO de l'opération enregistrée dans l'historique
     * @throws BankingException          si une règle métier est violée
     * @throws ResourceNotFoundException si le compte destinataire est
     *                                   introuvable
     */
    @Transactional
    public OperationResponse effectuerTransfert(User client, TransfertRequest request) {

        // --- Chargement du compte émetteur ---
        CompteBancaire compteEmetteur = getCompteOuErreur(client.getId(), "Client");
        verifierBanqueActive(compteEmetteur.getBanque());
        verifierCompteActif(compteEmetteur, "Émetteur");

        // --- Chargement du compte destinataire par numéro de compte ---
        CompteBancaire compteDestinataire = compteBancaireRepository
                .findByNumeroCompte(request.getNumeroCompteDestinataire())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CompteBancaire", "numeroCompte",
                        request.getNumeroCompteDestinataire()));

        // Blocage auto-transfert
        if (compteEmetteur.getId().equals(compteDestinataire.getId())) {
            throw new BankingException(
                    "Transfert impossible : l'émetteur et le destinataire "
                            + "sont le même compte.");
        }

        verifierBanqueActive(compteDestinataire.getBanque());
        verifierCompteActif(compteDestinataire, "Destinataire");

        // --- Détermination du type et du taux de frais ---
        boolean mêmeBanque = compteEmetteur.getBanque().getId()
                .equals(compteDestinataire.getBanque().getId());

        TypeOperation typeOperation = mêmeBanque
                ? TypeOperation.TRANSFERT_INTERNE
                : TypeOperation.TRANSFERT_INTERBANCAIRE;

        BigDecimal taux = mêmeBanque
                ? TAUX_FRAIS_STANDARD
                : TAUX_FRAIS_INTERBANCAIRE;
        
        //verifications prealables sur le montant du transfert
        BigDecimal montant = request.getMontant();
        
        //verifier que le montant est positif et superieur à 1000 et inferieur à 1000000
        if (montant.compareTo(BigDecimal.ZERO) <= 0 && montant.compareTo(MONTANT_MIN_DEPOSIT) < 0) {
			throw new BankingException(
					"Le montant du transfert doit être supérieur à "+MONTANT_MIN_DEPOSIT+"  FCFA.");
		}
        if(montant.compareTo(MONTANT_MAX_DEPOSIT) > 0) {
			throw new BankingException(
					"Le montant du transfert doit être inferieur à "+MONTANT_MIN_DEPOSIT+"  FCFA.");
		}
        
        
        
        BigDecimal frais = calculerFrais(montant, taux);
        BigDecimal montantDebiteEmetteur = montant.add(frais);

        // --- Validation du solde émetteur ---
        if (compteEmetteur.getSolde().compareTo(montantDebiteEmetteur) < 0) {
            String tauxPct = mêmeBanque ? "2" : "4";
            throw new BankingException(
                    "Solde insuffisant pour ce transfert "
                            + (mêmeBanque ? "interne" : "interbancaire") + ". "
                            + "Montant requis (transfert + frais " + tauxPct + " %) : "
                            + montantDebiteEmetteur + " FCFA, "
                            + "solde disponible : " + compteEmetteur.getSolde()
                            + " FCFA.");
        }

        // --- Mouvements financiers (atomiques) ---
        // 1. Débit émetteur (montant + frais)
        compteEmetteur.setSolde(
                compteEmetteur.getSolde().subtract(montantDebiteEmetteur));

        // 2. Crédit destinataire (montant uniquement)
        compteDestinataire.setSolde(
                compteDestinataire.getSolde().add(montant));

        compteBancaireRepository.save(compteEmetteur);
        compteBancaireRepository.save(compteDestinataire);

        // 3. Frais → montantActif de la banque émettrice
        Banque banqueEmettrice = compteEmetteur.getBanque();
        banqueEmettrice.setMontantActif(
                banqueEmettrice.getMontantActif().add(frais));
        banqueRepository.save(banqueEmettrice);

        // 4. Recalcul du montantLigne (Skill 13)
        recalculerMontantLigne(banqueEmettrice);
        if (!mêmeBanque) {
            recalculerMontantLigne(compteDestinataire.getBanque());
        }

        // --- Historique (Skill 14) ---
        Operation operation = Operation.builder()
                .type(typeOperation)
                .montant(montant)
                .frais(frais)
                .compteEmetteur(compteEmetteur)
                .compteDestinataire(compteDestinataire)
                .banque(banqueEmettrice)
                .description(request.getDescription())
                .build();

        Operation saved = operationRepository.save(operation);
        log.info("{} {} FCFA (frais={}) : {} → {} [{} → {}]",
                typeOperation, montant, frais,
                compteEmetteur.getNumeroCompte(),
                compteDestinataire.getNumeroCompte(),
                banqueEmettrice.getNom(),
                compteDestinataire.getBanque().getNom());

        return mapper.toOperationResponse(saved);
    }

    // =========================================================================
    //  Consultation (Skill 7)
    // =========================================================================

    /**
     * Retourne le compte bancaire de l'utilisateur connecté.
     *
     * @param utilisateur l'utilisateur connecté
     * @return le DTO du compte bancaire
     * @throws ResourceNotFoundException si l'utilisateur n'a pas encore
     *                                   de compte bancaire
     */
    @Transactional(readOnly = true)
    public CompteBancaireResponse getMonCompte(User utilisateur) {
        CompteBancaire compte = getCompteOuErreur(utilisateur.getId(), "Utilisateur");
        return mapper.toCompteBancaireResponse(compte);
    }

    /**
     * Retourne l'historique paginé des opérations du compte de
     * l'utilisateur connecté (Skill 7 & Skill 14).
     *
     * @param utilisateur l'utilisateur connecté
     * @param pageable    paramètres de pagination et de tri
     * @return une page d'opérations triées de la plus récente à la plus
     *         ancienne
     * @throws ResourceNotFoundException si l'utilisateur n'a pas de compte
     */
    @Transactional(readOnly = true)
    public Page<OperationResponse> getMonHistorique(
            User utilisateur, Pageable pageable) {
        CompteBancaire compte = getCompteOuErreur(utilisateur.getId(), "Utilisateur");
        return operationRepository.findByCompteId(compte.getId(), pageable)
                .map(mapper::toOperationResponse);
    }

    // =========================================================================
    //  Méthodes utilitaires privées
    // =========================================================================

    /**
     * Calcule les frais d'une opération en appliquant le taux donné au
     * montant, arrondi à 2 décimales (HALF_UP).
     *
     * @param montant le montant de base
     * @param taux    le taux de frais (ex. {@code 0.02} pour 2 %)
     * @return le montant des frais arrondi à 2 décimales
     */
    private BigDecimal calculerFrais(BigDecimal montant, BigDecimal taux) {
        return montant.multiply(taux)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Charge le compte bancaire d'un utilisateur par son identifiant, ou
     * lève une {@link ResourceNotFoundException} avec un message clair.
     *
     * @param utilisateurId l'identifiant de l'utilisateur
     * @param role          libellé du rôle pour le message d'erreur
     * @return le compte bancaire trouvé
     */
    private CompteBancaire getCompteOuErreur(Long utilisateurId, String role) {
        return compteBancaireRepository
                .findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CompteBancaire", "utilisateur (" + role + ") id",
                        utilisateurId));
    }

    /**
     * Charge le compte bancaire d'un utilisateur destinataire/client par
     * son identifiant utilisateur.
     *
     * @param utilisateurId l'identifiant de l'utilisateur cible
     * @param role          libellé du rôle pour le message d'erreur
     * @return le compte bancaire trouvé
     */
    private CompteBancaire getCompteParUtilisateurId(
            Long utilisateurId, String role) {
        return compteBancaireRepository
                .findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CompteBancaire", "utilisateur (" + role + ") id",
                        utilisateurId));
    }

    /**
     * Vérifie que la banque est active, sinon lève une
     * {@link BankingException} (Skill 15).
     *
     * @param banque la banque à vérifier
     */
    private void verifierBanqueActive(Banque banque) {
        if (banque.getStatut() == StatutBanque.SUSPENDUE) {
            throw new BankingException(
                    "Opération bloquée : la banque '"
                            + banque.getNom()
                            + "' est suspendue (Skill 15).");
        }
    }

    /**
     * Vérifie que le compte bancaire est actif, sinon lève une
     * {@link BankingException} (Skill 15).
     *
     * @param compte le compte à vérifier
     * @param role   libellé du rôle pour le message d'erreur
     */
    private void verifierCompteActif(CompteBancaire compte, String role) {
        if (compte.getStatut() == StatutCompte.SUSPENDU) {
            throw new BankingException(
                    "Opération bloquée : le compte " + role.toLowerCase()
                            + " (" + compte.getNumeroCompte()
                            + ") est suspendu (Skill 15).");
        }
    }

    /**
     * Recalcule et persiste le {@code montantLigne} de la banque donnée
     * (Skill 13) : somme de tous les soldes des comptes clients et agents.
     *
     * <p>Cette méthode est appelée à la fin de chaque transaction
     * financière pour maintenir l'indicateur à jour de façon atomique
     * dans la même transaction JPA.</p>
     *
     * @param banque la banque dont le {@code montantLigne} doit être mis
     *               à jour
     */
    private void recalculerMontantLigne(Banque banque) {
        BigDecimal montantLigne = compteBancaireRepository
                .calculerMontantLigne(banque.getId());
        banque.setMontantLigne(montantLigne);
        banqueRepository.save(banque);
        log.debug("montantLigne recalculé pour {} : {} FCFA",
                banque.getNom(), montantLigne);
    }

    /**
     * Génère un numéro de compte bancaire unique.
     *
     * <p>Format : {@code BK<banqueId_5d>-<yyyyMMddHHmmss>-<ms3d>}
     * Exemple : {@code BK00003-20240315102233-412}</p>
     *
     * @param banque la banque de rattachement du compte
     * @return un numéro de compte garanti unique (unicité renforcée par
     *         la contrainte {@code UNIQUE} de la colonne {@code numero_compte})
     */
    private String genererNumeroCompte(Banque banque) {
        String codeBanque = String.format("BK%05d", banque.getId());
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long ms = System.currentTimeMillis() % 1000;
        return codeBanque + "-" + timestamp + "-" + String.format("%03d", ms);
    }
}
