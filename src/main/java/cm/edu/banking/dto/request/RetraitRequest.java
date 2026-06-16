package cm.edu.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de requête pour un retrait effectué par un client auprès d'un
 * agent (Skill 10).
 *
 * <p>Calcul des frais (2 %) :</p>
 * <pre>
 *   frais               = montant × 2 %
 *   montantDébitéClient = montant + frais
 *   compteClient       -= montantDébitéClient
 *   compteAgent        += montant          (l'agent récupère le montant sans frais)
 *   banque.montantActif += frais
 * </pre>
 *
 * <p>Validation : {@code soldeClient >= montant + frais}.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetraitRequest {

    /**
     * Identifiant de l'utilisateur client qui effectue le retrait.
     */
    @NotNull(message = "L'identifiant du client est obligatoire")
    private Long clientId;

    /**
     * Montant souhaité par le client en FCFA (hors frais).
     * Doit être strictement positif.
     */
    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "1.00", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    /** Description ou motif optionnel du retrait. */
    private String description;
}
