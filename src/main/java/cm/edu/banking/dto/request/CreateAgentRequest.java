package cm.edu.banking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requête pour la création d'un agent par un superviseur
 * (Skill 5).
 *
 * <p>L'agent créé est automatiquement rattaché à la banque du superviseur
 * appelant. Son rôle est défini à
 * {@link cm.edu.banking.model.enums.Role#AGENT} côté service, et un
 * compte bancaire (solde initial = 0) lui est créé en même temps.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAgentRequest {

    /** Nom de famille de l'agent. */
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    /** Prénom de l'agent. */
    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    /** Adresse e-mail unique de l'agent (identifiant de connexion). */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Le format de l'email est invalide")
    private String email;

    /** Numéro de téléphone unique de l'agent. */
    @NotBlank(message = "Le téléphone est obligatoire")
    private String telephone;

    /** Mot de passe initial en clair (sera hashé en BCrypt côté service). */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String motDePasse;
}
