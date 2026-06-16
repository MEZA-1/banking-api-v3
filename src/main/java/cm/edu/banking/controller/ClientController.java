package cm.edu.banking.controller;

import cm.edu.banking.dto.request.CreateCompteBancaireRequest;
import cm.edu.banking.dto.request.TransfertRequest;
import cm.edu.banking.dto.response.CompteBancaireResponse;
import cm.edu.banking.dto.response.OperationResponse;
import cm.edu.banking.model.User;
import cm.edu.banking.security.SecurityUtils;
import cm.edu.banking.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST exposant les endpoints réservés au rôle
 * {@link cm.edu.banking.model.enums.Role#CLIENT}.
 *
 * <p>Un client peut :</p>
 * <ul>
 *   <li>créer son compte bancaire dans une banque de son choix (Skill 7 &
 *       8) ;</li>
 *   <li>consulter son solde et les détails de son compte (Skill 7) ;</li>
 *   <li>effectuer un transfert vers un autre compte — interne (Skill 11)
 *       ou interbancaire (Skill 12), le service déterminant automatiquement
 *       le type ;</li>
 *   <li>consulter l'historique paginé de ses opérations (Skill 7).</li>
 * </ul>
 *
 * <p>Base URL : {@code /api/client}</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Client", description = "Opérations réservées au client bancaire")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final TransactionService transactionService;
    private final SecurityUtils securityUtils;

    /**
     * Crée le compte bancaire du client connecté dans la banque spécifiée
     * (Skill 7 & 8).
     *
     * <p>Règles :</p>
     * <ul>
     *   <li>Un client ne peut posséder qu'un seul compte bancaire.</li>
     *   <li>Le solde initial est obligatoirement nul.</li>
     *   <li>La banque choisie doit être active.</li>
     * </ul>
     *
     * @param request l'identifiant de la banque choisie
     * @return {@code 201 Created} avec le DTO du compte créé
     */
    @PostMapping("/compte")
    @Operation(
            summary = "Créer mon compte bancaire",
            description = "Ouvre un compte bancaire (solde = 0) dans la banque choisie. "
                    + "Un seul compte par client (Skills 7 & 8)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé",
                    content = @Content(schema = @Schema(implementation = CompteBancaireResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Client possède déjà un compte, ou banque suspendue",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Banque introuvable",
                    content = @Content)
    })
    public ResponseEntity<CompteBancaireResponse> creerCompte(
            @Valid @RequestBody CreateCompteBancaireRequest request) {
        User client = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.creerCompteBancaire(client, request));
    }

    /**
     * Retourne le compte bancaire et le solde courant du client connecté
     * (Skill 7).
     *
     * @return {@code 200 OK} avec le DTO du compte
     */
    @GetMapping("/compte")
    @Operation(
            summary = "Consulter mon compte et mon solde",
            description = "Retourne les détails du compte bancaire du client connecté, "
                    + "incluant le solde courant (Skill 7)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte trouvé",
                    content = @Content(schema = @Schema(implementation = CompteBancaireResponse.class))),
            @ApiResponse(responseCode = "404", description = "Aucun compte pour ce client",
                    content = @Content)
    })
    public ResponseEntity<CompteBancaireResponse> getMonCompte() {
        User client = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.ok(transactionService.getMonCompte(client));
    }

    /**
     * Effectue un transfert de fonds depuis le compte du client connecté
     * vers un compte identifié par son numéro.
     *
     * <p>Le service détermine automatiquement le type de transfert :</p>
     * <ul>
     *   <li>Même banque → <strong>transfert interne</strong>, frais 2 %
     *       (Skill 11).</li>
     *   <li>Banques différentes → <strong>transfert interbancaire</strong>,
     *       frais 4 % (Skill 12).</li>
     * </ul>
     *
     * <p>Dans les deux cas, l'émetteur est débité de
     * {@code montant + frais} et le destinataire est crédité du
     * {@code montant}.</p>
     *
     * @param request le numéro de compte destinataire, le montant et la
     *                description optionnelle
     * @return {@code 200 OK} avec le DTO de l'opération enregistrée,
     *         incluant le type de transfert déterminé automatiquement
     */
    @PostMapping("/transfert")
    @Operation(
            summary = "Effectuer un transfert",
            description = "Transfère des fonds vers un autre compte. "
                    + "Le type (interne 2 % / interbancaire 4 %) est déterminé "
                    + "automatiquement selon les banques des deux comptes (Skills 11 & 12)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfert effectué",
                    content = @Content(schema = @Schema(implementation = OperationResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Solde insuffisant, auto-transfert, compte ou banque suspendu",
                    content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "Compte destinataire introuvable", content = @Content)
    })
    public ResponseEntity<OperationResponse> effectuerTransfert(
            @Valid @RequestBody TransfertRequest request) {
        User client = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.ok(
                transactionService.effectuerTransfert(client, request));
    }

    /**
     * Retourne l'historique paginé des opérations du client connecté
     * (dépôts reçus, retraits effectués, transferts émis et reçus)
     * (Skill 7 & 14).
     *
     * @param pageable paramètres de pagination (page, size, sort)
     * @return {@code 200 OK} avec la page d'opérations
     */
    @GetMapping("/historique")
    @Operation(
            summary = "Consulter mon historique d'opérations",
            description = "Retourne toutes les opérations impliquant le compte du client "
                    + "(émetteur ou destinataire), triées de la plus récente à la plus ancienne "
                    + "(Skills 7 & 14)."
    )
    @ApiResponse(responseCode = "200", description = "Page d'opérations")
    public ResponseEntity<Page<OperationResponse>> getHistorique(
            @PageableDefault(size = 20, sort = "dateOperation",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        User client = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.ok(
                transactionService.getMonHistorique(client, pageable));
    }
}
