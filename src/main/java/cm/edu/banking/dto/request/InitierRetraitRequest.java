package cm.edu.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de requête — Étape 1 : l'agent initie un retrait pour un client.
 *
 * <p>Cette requête déclenche la génération d'un OTP à 6 chiffres qui est
 * transmis au client. Le retrait n'est pas encore exécuté à cette étape.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitierRetraitRequest {

    /** Identifiant de l'utilisateur client qui effectue le retrait. */
    @NotNull(message = "L'identifiant du client est obligatoire")
    private Long clientId;

    /**
     * Montant souhaité par le client en FCFA (hors frais de 2 %).
     * Doit être strictement positif.
     */
    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "1.00", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    /** Description ou motif optionnel du retrait. */
    private String description;
}