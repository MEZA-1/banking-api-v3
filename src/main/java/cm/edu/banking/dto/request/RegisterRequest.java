package cm.edu.banking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requête pour l'inscription d'un nouvel utilisateur via
 * {@code POST /api/auth/register} (Skill 2.2).
 *
 * <p>À la réception de cette requête, le système crée automatiquement
 * un utilisateur avec le rôle {@link cm.edu.banking.model.enums.Role#CLIENT}.
 * L'email et le téléphone doivent être uniques sur l'ensemble de la
 * plateforme.</p>
 *
 * <p>Le champ {@code motDePasse} est hashé en BCrypt avant persistance ;
 * il ne doit jamais être stocké ni retourné en clair.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /** Nom de famille de l'utilisateur. */
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    /** Prénom de l'utilisateur. */
    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    /** Adresse e-mail, utilisée comme identifiant de connexion. */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Le format de l'email est invalide")
    private String email;

    /** Numéro de téléphone de l'utilisateur. */
    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    private String telephone;

    /** Mot de passe en clair (sera hashé en BCrypt côté service). */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String motDePasse;
}
