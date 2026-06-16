package cm.edu.banking.controller;

import cm.edu.banking.dto.request.DepotRequest;
import cm.edu.banking.dto.request.RetraitRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST exposant les endpoints réservés au rôle
 * {@link cm.edu.banking.model.enums.Role#AGENT}.
 *
 * <p>Un agent peut :</p>
 * <ul>
 *   <li>consulter son propre compte bancaire (caisse) ;</li>
 *   <li>effectuer un dépôt pour un client (Skill 9) ;</li>
 *   <li>traiter un retrait d'un client (Skill 10) ;</li>
 *   <li>consulter l'historique paginé de ses propres opérations (Skill 6).</li>
 * </ul>
 *
 * <p>Base URL : {@code /api/agent}</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENT')")
@Tag(name = "Agent", description = "Opérations réservées à l'agent bancaire")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final TransactionService transactionService;
    private final SecurityUtils securityUtils;

    /**
     * Retourne le compte bancaire (caisse) de l'agent connecté.
     *
     * @return {@code 200 OK} avec le DTO du compte de l'agent
     */
    @GetMapping("/mon-compte")
    @Operation(summary = "Consulter mon compte (caisse)")
    @ApiResponse(responseCode = "200", description = "Compte de l'agent",
            content = @Content(schema = @Schema(implementation = CompteBancaireResponse.class)))
    public ResponseEntity<CompteBancaireResponse> getMonCompte() {
        User agent = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.ok(transactionService.getMonCompte(agent));
    }

    /**
     * Effectue un dépôt depuis le compte de l'agent vers le compte d'un
     * client (Skill 9 — sans frais).
     *
     * <p>Le montant est prélevé sur la caisse de l'agent et crédité sur
     * le compte du client. La règle de solde agent est vérifiée :
     * {@code soldeAgent >= montant}.</p>
     *
     * @param request l'identifiant du client, le montant et la description
     * @return {@code 200 OK} avec le DTO de l'opération enregistrée
     */
    @PostMapping("/depot")
    @Operation(
            summary = "Effectuer un dépôt pour un client",
            description = "Débite la caisse de l'agent et crédite le compte du client. "
                    + "Aucun frais (Skill 9)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dépôt effectué",
                    content = @Content(schema = @Schema(implementation = OperationResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Solde agent insuffisant, compte ou banque suspendu",
                    content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "Client ou compte introuvable", content = @Content)
    })
    public ResponseEntity<OperationResponse> effectuerDepot(
            @Valid @RequestBody DepotRequest request) {
        User agent = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.ok(transactionService.effectuerDepot(agent, request));
    }

    /**
     * Traite le retrait d'un client (Skill 10 — frais 2 %).
     *
     * <p>Flux :</p>
     * <ul>
     *   <li>Le compte client est débité de {@code montant + frais (2 %)}.</li>
     *   <li>La caisse de l'agent est créditée du {@code montant} (sans les frais).</li>
     *   <li>Les frais sont versés au {@code montantActif} de la banque.</li>
     * </ul>
     *
     * @param request l'identifiant du client, le montant et la description
     * @return {@code 200 OK} avec le DTO de l'opération enregistrée
     */
    @PostMapping("/retrait")
    @Operation(
            summary = "Traiter le retrait d'un client",
            description = "Débite le client (montant + 2 % de frais), crédite la caisse "
                    + "de l'agent et reverse les frais au montantActif de la banque (Skill 10)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retrait effectué",
                    content = @Content(schema = @Schema(implementation = OperationResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Solde client insuffisant, compte ou banque suspendu",
                    content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "Client ou compte introuvable", content = @Content)
    })
    public ResponseEntity<OperationResponse> effectuerRetrait(
            @Valid @RequestBody RetraitRequest request) {
        User agent = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.ok(transactionService.effectuerRetrait(agent, request));
    }

    /**
     * Retourne l'historique paginé des opérations de l'agent connecté
     * (dépôts traités, retraits traités, approvisionnements reçus).
     *
     * @param pageable paramètres de pagination (page, size, sort)
     * @return {@code 200 OK} avec la page d'opérations
     */
    @GetMapping("/historique")
    @Operation(
            summary = "Consulter mon historique d'opérations",
            description = "Retourne toutes les opérations impliquant le compte de l'agent "
                    + "(émetteur ou destinataire), triées de la plus récente à la plus ancienne."
    )
    @ApiResponse(responseCode = "200", description = "Page d'opérations")
    public ResponseEntity<Page<OperationResponse>> getHistorique(
            @PageableDefault(size = 20, sort = "dateOperation",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        User agent = securityUtils.getUtilisateurConnecte();
        return ResponseEntity.ok(transactionService.getMonHistorique(agent, pageable));
    }
}
