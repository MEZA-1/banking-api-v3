package cm.edu.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requête — Étape 2 : l'agent soumet la confirmation du retrait
 * avec le code OTP et le mot de passe du client.
 *
 * <p>Les deux facteurs sont vérifiés côté backend :</p>
 * <ol>
 *   <li>Le {@code codeOtp} doit correspondre à celui généré lors de
 *       l'initiation et ne pas être expiré (fenêtre de 5 minutes).</li>
 *   <li>Le {@code motDePasseClient} est vérifié par BCrypt contre
 *       le hash stocké en base pour l'utilisateur propriétaire du compte.</li>
 * </ol>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmerRetraitRequest {

    /**
     * Identifiant de la demande de retrait en attente, retourné lors de
     * l'initiation dans {@link cm.edu.banking.dto.response.RetraitEnAttenteResponse}.
     */
    @NotNull(message = "L'identifiant de la demande est obligatoire")
    private Long demandeId;

    /**
     * Code OTP à 6 chiffres reçu par le client et communiqué à l'agent.
     */
    @NotBlank(message = "Le code OTP est obligatoire")
    @Pattern(regexp = "\\d{6}", message = "Le code OTP doit contenir exactement 6 chiffres")
    private String codeOtp;

    /**
     * Mot de passe en clair du client, vérifié par BCrypt.
     * Permet de confirmer que le client présent est bien le titulaire
     * du compte sans nécessiter de session client active.
     */
    @NotBlank(message = "Le mot de passe du client est obligatoire")
    private String motDePasseClient;
}