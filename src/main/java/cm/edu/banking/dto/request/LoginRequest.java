package cm.edu.banking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requête pour l'authentification d'un utilisateur existant via
 * {@code POST /api/auth/login} (Skill 2.2).
 *
 * <p>En cas de succès, le service d'authentification retourne un token JWT
 * contenu dans un {@link cm.edu.banking.dto.response.AuthResponse}.</p>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /** Adresse e-mail de l'utilisateur (identifiant de connexion). */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Le format de l'email est invalide")
    private String email;

    /** Mot de passe en clair de l'utilisateur. */
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String motDePasse;
}
