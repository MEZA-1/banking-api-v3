package cm.edu.banking.controller;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cm.edu.banking.dto.request.ApprovisionnerAgentRequest;
import cm.edu.banking.dto.request.CreateAgentRequest;
import cm.edu.banking.dto.response.CompteBancaireResponse;
import cm.edu.banking.dto.response.OperationResponse;
import cm.edu.banking.dto.response.UserResponse;
import cm.edu.banking.model.User;
import cm.edu.banking.security.SecurityUtils;
import cm.edu.banking.service.SuperviseurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur REST exposant les endpoints réservés au rôle
 * {@link cm.edu.banking.model.enums.Role#SUPERVISEUR}.
 *
 * <p>Toutes les routes sont protégées par
 * {@code @PreAuthorize("hasRole('SUPERVISEUR')")}. Le superviseur opère
 * exclusivement dans le périmètre de sa propre banque : chaque méthode
 * récupère l'utilisateur connecté via {@link SecurityUtils} pour en
 * déduire la banque de rattachement.</p>
 *
 * <p>Base URL : {@code /api/superviseur}</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@RestController
@Slf4j
@RequestMapping("/api/superviseur")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERVISEUR')")
@Tag(name = "Superviseur", description = "Opérations réservées au superviseur de banque")
@SecurityRequirement(name = "bearerAuth")
public class SuperviseurController {

    private final SuperviseurService superviseurService;
    private final SecurityUtils securityUtils;

    // =========================================================================
    //  Gestion des agents (Skill 5)
    // =========================================================================

    /**
     * Crée un agent rattaché à la banque du superviseur connecté.
     *
     * <p>Un compte bancaire (solde initial = 0) est automatiquement créé
     * pour l'agent (Skill 6).</p>
     *
     * @param request les informations de l'agent à créer
     * @return {@code 201 Created} avec le DTO de l'agent créé
     */
    @PostMapping("/agents")
    @Operation(
            summary = "Créer un agent",
            description = "Crée un compte agent dans la banque du superviseur connecté. "
                    + "Un compte bancaire (solde 0) est généré automatiquement."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Agent créé",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides ou banque suspendue",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Email ou téléphone déjà utilisé",
                    content = @Content)
    })
    public ResponseEntity<UserResponse> creerAgent(
            @Valid @RequestBody CreateAgentRequest request) {
        User superviseur = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(superviseurService.creerAgent(superviseur, request));
    }

    /**
     * Retourne la liste des agents de la banque du superviseur connecté.
     *
     * @return {@code 200 OK} avec la liste des agents
     */
    @GetMapping("/agents")
    @Operation(summary = "Lister les agents de ma banque")
    @ApiResponse(responseCode = "200", description = "Liste des agents")
    public ResponseEntity<List<UserResponse>> listerAgents() {
        User superviseur = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.ok(superviseurService.listerAgents(superviseur));
    }

    // =========================================================================
    //  Gestion des clients (Skill 5)
    // =========================================================================

    /**
     * Retourne la liste des clients rattachés à la banque du superviseur
     * connecté (c'est-à-dire les clients dont le compte bancaire est lié
     * à cette banque).
     *
     * @return {@code 200 OK} avec la liste des clients
     */
    @GetMapping("/clients")
    @Operation(summary = "Lister les clients de ma banque")
    @ApiResponse(responseCode = "200", description = "Liste des clients")
    public ResponseEntity<List<UserResponse>> listerClients() {
        User superviseur = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.ok(superviseurService.listerClients(superviseur));
    }

    // =========================================================================
    //  Suspension / Réactivation de comptes (Skill 5)
    // =========================================================================

    /**
     * Suspend le compte bancaire d'un utilisateur (agent ou client)
     * appartenant à la banque du superviseur connecté.
     *
     * <p>Un compte suspendu ne peut plus émettre ni recevoir aucune
     * opération financière (Skill 15).</p>
     *
     * @param utilisateurId l'identifiant de l'utilisateur dont le compte
     *                      doit être suspendu
     * @return {@code 200 OK} avec le DTO du compte mis à jour
     */
    @PatchMapping("/comptes/{utilisateurId}/suspendre")
    @Operation(
            summary = "Suspendre le compte d'un utilisateur",
            description = "Suspend le compte bancaire d'un agent ou d'un client de la banque "
                    + "(Skill 5 & 15)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte suspendu",
                    content = @Content(schema = @Schema(implementation = CompteBancaireResponse.class))),
            @ApiResponse(responseCode = "400", description = "Compte déjà suspendu", content = @Content),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable", content = @Content)
    })
    public ResponseEntity<CompteBancaireResponse> suspendreCompte(
            @Parameter(description = "Identifiant de l'utilisateur")
            @PathVariable Long utilisateurId) {
        User superviseur = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.ok(superviseurService.suspendreCompte(superviseur, utilisateurId));
    }

    /**
     * Réactive le compte bancaire d'un utilisateur suspendu appartenant
     * à la banque du superviseur connecté.
     *
     * @param utilisateurId l'identifiant de l'utilisateur dont le compte
     *                      doit être réactivé
     * @return {@code 200 OK} avec le DTO du compte mis à jour
     */
    @PatchMapping("/comptes/{utilisateurId}/reactiver")
    @Operation(
            summary = "Réactiver le compte d'un utilisateur suspendu",
            description = "Réactive le compte bancaire d'un agent ou d'un client de la banque."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte réactivé",
                    content = @Content(schema = @Schema(implementation = CompteBancaireResponse.class))),
            @ApiResponse(responseCode = "400", description = "Compte déjà actif", content = @Content),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable", content = @Content)
    })
    public ResponseEntity<CompteBancaireResponse> reactiverCompte(
            @Parameter(description = "Identifiant de l'utilisateur")
            @PathVariable Long utilisateurId) {
        User superviseur = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.ok(superviseurService.reactiverCompte(superviseur, utilisateurId));
    }

    // =========================================================================
    //  Approvisionnement agent (Skill 5)
    // =========================================================================

    /**
     * Approvisionne le compte d'un agent en prélevant le montant sur le
     * {@code montantActif} de la banque.
     *
     * <p>Après l'opération, le {@code montantLigne} de la banque est
     * automatiquement recalculé (Skill 13) et l'opération est enregistrée
     * dans l'historique (Skill 14).</p>
     *
     * @param agentId l'identifiant de l'agent à approvisionner
     * @param request le montant et la description optionnelle
     * @return {@code 200 OK} avec le DTO de l'opération enregistrée
     */
    @PostMapping("/agents/{agentId}/approvisionner")
    @Operation(
            summary = "Approvisionner un agent",
            description = "Transfère un montant du montantActif de la banque vers "
                    + "le compte de l'agent. Recalcule le montantLigne (Skill 5 & 13)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approvisionnement effectué",
                    content = @Content(schema = @Schema(implementation = OperationResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Montantactif insuffisant, banque ou compte suspendu",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Agent introuvable", content = @Content)
    })
    public ResponseEntity<OperationResponse> approvisionnerAgent(
            @Parameter(description = "Identifiant de l'agent") @PathVariable Long agentId,
            @Valid @RequestBody ApprovisionnerAgentRequest request) {
        User superviseur = securityUtils.getUtilisateurConnecte();
        //superviseur.setBanque(securityUtils.getBanqueConnectee());
        //afficher le supperviseur dans les logs pour verifier les donners
        
        log.info("superviseur controler"+superviseur);
        
        return ResponseEntity.ok(
                superviseurService.approvisionnerAgent(superviseur, agentId, request));
    }

    // =========================================================================
    //  Historique (Skill 14)
    // =========================================================================

    /**
     * Retourne l'historique paginé des opérations de la banque du
     * superviseur connecté.
     *
     * @param pageable paramètres de pagination (page, size, sort)
     * @return {@code 200 OK} avec la page d'opérations
     */
    @GetMapping("/historique")
    @Operation(
            summary = "Historique paginé des opérations de ma banque",
            description = "Retourne toutes les opérations de la banque du superviseur "
                    + "connecté, triées de la plus récente à la plus ancienne."
    )
    @ApiResponse(responseCode = "200", description = "Page d'opérations")
    public ResponseEntity<Page<OperationResponse>> getHistorique(
            @PageableDefault(size = 20, sort = "dateOperation",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        User superviseur = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.ok(superviseurService.getHistoriqueBanque(superviseur, pageable));
    }
}
