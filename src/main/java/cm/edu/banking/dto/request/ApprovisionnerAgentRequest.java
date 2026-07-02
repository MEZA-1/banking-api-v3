package cm.edu.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de requête pour l'approvisionnement du compte d'un agent par son
 * superviseur (Skill 5).
 *
 * <p>L'opération débite le {@code montantActif} de la banque et crédite
 * le solde du compte de l'agent du même montant. Le {@code montantLigne}
 * de la banque est recalculé automatiquement après l'opération (Skill 13).</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovisionnerAgentRequest {

    /**
     * Montant à transférer du {@code montantActif} de la banque vers le
     * compte de l'agent. Doit être strictement positif.
     */
    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "1.00", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    /** Description optionnelle de l'approvisionnement. */
    private String description;
    
    //id de la banque
    
    //private long idBanque;
}
