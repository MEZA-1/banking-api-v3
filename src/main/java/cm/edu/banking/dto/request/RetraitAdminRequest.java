package cm.edu.banking.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de requête pour le retrait effectué par un administrateur
 * directement sur le {@code montantActif} d'une banque (Skill 4).
 *
 * <p>Règles métier :</p>
 * <ul>
 *   <li>Le montant est plafonné à 1 000 000 FCFA par opération.</li>
 *   <li>Le retrait est impossible si le {@code montantActif} de la banque
 *       est insuffisant.</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetraitAdminRequest {

    /**
     * Montant à retirer du {@code montantActif} de la banque.
     * Doit être compris entre 1 et 1 000 000 FCFA inclus.
     */
    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "1.00", message = "Le montant doit être supérieur à 0")
    @DecimalMax(value = "1000000.00",
            message = "Le retrait administrateur est plafonné à 1 000 000 FCFA par opération")
    private BigDecimal montant;

    /** Motif du retrait administrateur (optionnel). */
    private String description;
}
