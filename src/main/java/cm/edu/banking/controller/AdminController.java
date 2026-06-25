package cm.edu.banking.controller;

import cm.edu.banking.dto.request.CreateBanqueRequest;
import cm.edu.banking.dto.request.CreateSuperviseurRequest;
import cm.edu.banking.dto.request.RetraitAdminRequest;
import cm.edu.banking.dto.response.BanqueResponse;
import cm.edu.banking.dto.response.OperationResponse;
import cm.edu.banking.dto.response.UserResponse;
import cm.edu.banking.service.AdminService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST exposant les endpoints réservés au rôle
 * {@link cm.edu.banking.model.enums.Role#ADMIN}.
 *
 * <p>Toutes les routes de ce contrôleur sont protégées par
 * {@code @PreAuthorize("hasRole('ADMIN')")} : Spring Security rejette
 * toute requête d'un utilisateur non-administrateur avec un
 * {@code 403 Forbidden}.</p>
 *
 * <p>Base URL : {@code /api/admin}</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISEUR')")
@Tag(name = "Administration", description = "Opérations réservées à l'administrateur global")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    // =========================================================================
    //  Gestion des banques (Skills 3 & 4)
    // =========================================================================

    /**
     * Crée une nouvelle banque sur la plateforme.
     *
     * <p>Le {@code montantActif} est initialisé à {@code montantInitial}
     * et le {@code montantLigne} à zéro automatiquement (Skill 3).</p>
     *
     * @param request les données de la banque (nom, montantInitial ≥ 2 000 000 FCFA)
     * @return {@code 201 Created} avec le DTO de la banque créée
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/banques")
    @Operation(
            summary = "Créer une banque",
            description = "Crée une nouvelle banque avec un montant initial ≥ 2 000 000 FCFA."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Banque créée",
                    content = @Content(schema = @Schema(implementation = BanqueResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
            @ApiResponse(responseCode = "409", description = "Nom de banque déjà utilisé", content = @Content)
    })
    public ResponseEntity<BanqueResponse> creerBanque(
            @Valid @RequestBody CreateBanqueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.creerBanque(request));
    }

    /**
     * Retourne la liste complète de toutes les banques de la plateforme.
     *
     * @return {@code 200 OK} avec la liste des banques
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/banques")
    @Operation(summary = "Lister toutes les banques")
    @ApiResponse(responseCode = "200", description = "Liste des banques")
    public ResponseEntity<List<BanqueResponse>> listerBanques() {
        return ResponseEntity.ok(adminService.listerBanques());
    }

    /**
     * Retourne le détail d'une banque par son identifiant.
     *
     * @param banqueId l'identifiant de la banque
     * @return {@code 200 OK} avec le DTO de la banque
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISEUR')")
    @GetMapping("/banques/{banqueId}")
    @Operation(summary = "Consulter une banque")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Banque trouvée",
                    content = @Content(schema = @Schema(implementation = BanqueResponse.class))),
            @ApiResponse(responseCode = "404", description = "Banque introuvable", content = @Content)
    })
    public ResponseEntity<BanqueResponse> getBanque(
            @Parameter(description = "Identifiant de la banque") @PathVariable Long banqueId) {
        return ResponseEntity.ok(adminService.getBanque(banqueId));
    }

    /**
     * Suspend une banque active.
     *
     * <p>Toutes les opérations des utilisateurs rattachés à cette banque
     * seront bloquées (Skill 15).</p>
     *
     * @param banqueId l'identifiant de la banque à suspendre
     * @return {@code 200 OK} avec le DTO de la banque mise à jour
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/banques/{banqueId}/suspendre")
    @Operation(summary = "Suspendre une banque")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Banque suspendue"),
            @ApiResponse(responseCode = "400", description = "Banque déjà suspendue", content = @Content),
            @ApiResponse(responseCode = "404", description = "Banque introuvable", content = @Content)
    })
    public ResponseEntity<BanqueResponse> suspendrebanque(
            @Parameter(description = "Identifiant de la banque") @PathVariable Long banqueId) {
        return ResponseEntity.ok(adminService.suspendrebanque(banqueId));
    }

    /**
     * Réactive une banque suspendue.
     *
     * @param banqueId l'identifiant de la banque à réactiver
     * @return {@code 200 OK} avec le DTO de la banque mise à jour
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/banques/{banqueId}/reactiver")
    @Operation(summary = "Réactiver une banque suspendue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Banque réactivée"),
            @ApiResponse(responseCode = "400", description = "Banque déjà active", content = @Content),
            @ApiResponse(responseCode = "404", description = "Banque introuvable", content = @Content)
    })
    public ResponseEntity<BanqueResponse> reactiverBanque(
            @Parameter(description = "Identifiant de la banque") @PathVariable Long banqueId) {
        return ResponseEntity.ok(adminService.reactiverBanque(banqueId));
    }

    /**
     * Effectue un retrait sur le {@code montantActif} d'une banque.
     *
     * <p>Plafond : 1 000 000 FCFA par opération (Skill 4). L'opération
     * est enregistrée dans l'historique.</p>
     *
     * @param banqueId l'identifiant de la banque concernée
     * @param request  le montant à retirer et une description optionnelle
     * @return {@code 200 OK} avec le DTO de l'opération enregistrée
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/banques/{banqueId}/retrait")
    @Operation(
            summary = "Retrait administrateur sur une banque",
            description = "Débite le montantActif de la banque. "
                    + "Plafonné à 1 000 000 FCFA par opération (Skill 4)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retrait effectué",
                    content = @Content(schema = @Schema(implementation = OperationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Montant invalide ou montantActif insuffisant",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Banque introuvable", content = @Content)
    })
    public ResponseEntity<OperationResponse> retraitAdmin(
            @Parameter(description = "Identifiant de la banque") @PathVariable Long banqueId,
            @Valid @RequestBody RetraitAdminRequest request) {
        return ResponseEntity.ok(adminService.retraitAdmin(banqueId, request));
    }

    /**
     * Consulte l'historique paginé des opérations d'une banque donnée.
     *
     * @param banqueId l'identifiant de la banque
     * @param pageable paramètres de pagination (page, size, sort)
     * @return {@code 200 OK} avec la page d'opérations
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/banques/{banqueId}/historique")
    @Operation(summary = "Historique paginé des opérations d'une banque")
    public ResponseEntity<Page<OperationResponse>> getHistoriqueBanque(
            @Parameter(description = "Identifiant de la banque") @PathVariable Long banqueId,
            @PageableDefault(size = 20, sort = "dateOperation",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getHistoriqueBanque(banqueId, pageable));
    }

    // =========================================================================
    //  Gestion des utilisateurs (Skill 4)
    // =========================================================================

    /**
     * Retourne la liste de tous les utilisateurs de la plateforme.
     *
     * @return {@code 200 OK} avec la liste des utilisateurs
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/utilisateurs")
    @Operation(summary = "Lister tous les utilisateurs de la plateforme")
    public ResponseEntity<List<UserResponse>> listerUtilisateurs() {
        return ResponseEntity.ok(adminService.listerUtilisateurs());
    }

    /**
     * Crée un superviseur rattaché à une banque (Skill 4).
     *
     * <p>Seuls les administrateurs peuvent créer des superviseurs.
     * Le rôle {@link cm.edu.banking.model.enums.Role#SUPERVISEUR} est
     * attribué automatiquement.</p>
     *
     * @param request les informations du superviseur et l'identifiant de
     *                sa banque de rattachement
     * @return {@code 201 Created} avec le DTO du superviseur créé
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/superviseurs")
    @Operation(
            summary = "Créer un superviseur",
            description = "Crée un compte superviseur rattaché à une banque existante (Skill 4)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Superviseur créé",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
            @ApiResponse(responseCode = "404", description = "Banque introuvable", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email ou téléphone déjà utilisé",
                    content = @Content)
    })
    public ResponseEntity<UserResponse> creerSuperviseur(
            @Valid @RequestBody CreateSuperviseurRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.creerSuperviseur(request));
    }
}
