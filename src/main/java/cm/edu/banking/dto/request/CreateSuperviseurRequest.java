package cm.edu.banking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requête pour la création d'un superviseur par un administrateur
 * (Skill 4).
 *
 * <p>Le superviseur créé est rattaché à la banque identifiée par
 * {@code banqueId}. Son rôle est automatiquement défini à
 * {@link cm.edu.banking.model.enums.Role#SUPERVISEUR} côté service.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSuperviseurRequest {

    /** Nom de famille du superviseur. */
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    /** Prénom du superviseur. */
    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    /** Adresse e-mail unique du superviseur (identifiant de connexion). */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Le format de l'email est invalide")
    private String email;

    /** Numéro de téléphone unique du superviseur. */
    @NotBlank(message = "Le téléphone est obligatoire")
    private String telephone;

    /** Mot de passe initial en clair (sera hashé en BCrypt côté service). */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String motDePasse;

    /** Identifiant de la banque à laquelle rattacher le superviseur. */
    @NotNull(message = "L'identifiant de la banque est obligatoire")
    private Long banqueId;
}
