package cm.edu.banking.dto.response;

import cm.edu.banking.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse retourné par les endpoints d'authentification
 * ({@code POST /api/auth/register} et {@code POST /api/auth/login}).
 *
 * <p>Contient le token JWT à inclure dans l'en-tête
 * {@code Authorization: Bearer <accessToken>} pour toutes les requêtes
 * ultérieures vers les endpoints protégés, ainsi que les informations
 * minimales nécessaires pour que le client adapte son comportement selon
 * le rôle de l'utilisateur connecté.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * Token JWT signé, valide pour la durée configurée dans
     * {@code app.jwt.expiration} (24 h par défaut).
     *
     * <p>Doit être envoyé dans l'en-tête HTTP :
     * {@code Authorization: Bearer <accessToken>}</p>
     */
    private String accessToken;

    /** Type du token, toujours {@code "Bearer"} pour JWT. */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Identifiant technique de l'utilisateur authentifié. */
    private Long userId;

    /** Nom complet de l'utilisateur authentifié ({@code prenom nom}). */
    private String nomComplet;

    /** Adresse e-mail de l'utilisateur authentifié. */
    private String email;

    /**
     * Rôle de l'utilisateur authentifié, permettant au client de connaître
     * les droits associés et d'adapter l'interface utilisateur en
     * conséquence.
     */
    private Role role;
}
