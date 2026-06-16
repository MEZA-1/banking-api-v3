package cm.edu.banking.controller;

import cm.edu.banking.dto.request.LoginRequest;
import cm.edu.banking.dto.request.RegisterRequest;
import cm.edu.banking.dto.response.AuthResponse;
import cm.edu.banking.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST gérant les endpoints publics d'authentification.
 *
 * <p>Ces endpoints sont accessibles sans token JWT (configurés comme
 * {@code permitAll} dans
 * {@link cm.edu.banking.config.SecurityConfig}) :</p>
 * <ul>
 *   <li>{@code POST /api/auth/register} : inscription d'un nouveau client
 *       (Skill 2.2).</li>
 *   <li>{@code POST /api/auth/login} : connexion et génération d'un token
 *       JWT (Skill 2.2).</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription et connexion des utilisateurs")
public class AuthController {

    private final AuthService authService;

    /**
     * Inscrit un nouvel utilisateur sur la plateforme.
     *
     * <p>Le rôle {@link cm.edu.banking.model.enums.Role#CLIENT} est attribué
     * automatiquement. En cas de succès, un token JWT valide est retourné
     * immédiatement, évitant un second appel à {@code /login}.</p>
     *
     * @param request le corps de la requête contenant les informations
     *                d'inscription (nom, prénom, email, téléphone, mot de passe)
     * @return {@code 201 Created} avec le token JWT et les informations
     *         de l'utilisateur créé
     */
    @PostMapping("/register")
    @Operation(
            summary = "Inscription d'un nouveau client",
            description = "Crée un compte utilisateur avec le rôle CLIENT par défaut "
                    + "et retourne un token JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscription réussie",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Email ou téléphone déjà utilisé",
                    content = @Content)
    })
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authentifie un utilisateur existant et génère un token JWT.
     *
     * <p>Le token JWT retourné doit être inclus dans l'en-tête
     * {@code Authorization: Bearer <token>} de toutes les requêtes
     * ultérieures vers les endpoints protégés.</p>
     *
     * @param request le corps de la requête contenant l'email et le mot
     *                de passe de l'utilisateur
     * @return {@code 200 OK} avec le token JWT et les informations de
     *         l'utilisateur authentifié
     */
    @PostMapping("/login")
    @Operation(
            summary = "Connexion et génération du token JWT",
            description = "Authentifie l'utilisateur et retourne un token JWT "
                    + "à inclure dans l'en-tête Authorization des requêtes suivantes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Identifiants incorrects",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content)
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
