package cm.edu.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de requête pour un dépôt effectué par un agent au profit d'un
 * client (Skill 9).
 *
 * <p>Règles :</p>
 * <ul>
 *   <li>Aucun frais n'est appliqué.</li>
 *   <li>Le montant est débité du compte de l'agent et crédité sur le
 *       compte du client.</li>
 *   <li>Validation : {@code soldeAgent >= montant}.</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepotRequest {

    /**
     * Identifiant de l'utilisateur client qui reçoit le dépôt.
     */
    @NotNull(message = "L'identifiant du client est obligatoire")
    private Long clientId;

    /**
     * Montant à déposer en FCFA. Doit être strictement positif.
     */
    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "1.00", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    /** Description ou motif optionnel du dépôt. */
    private String description;
}
