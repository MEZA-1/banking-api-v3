package cm.edu.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de requête pour la création d'une nouvelle banque par un
 * administrateur (Skill 3 / Skill 4).
 *
 * <p>Le {@code montantActif} est initialisé automatiquement à
 * {@code montantInitial} côté service. Le {@code montantLigne}
 * démarre à zéro. Ces deux champs ne doivent jamais être fournis
 * dans la requête.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBanqueRequest {

    /** Nom unique de la banque à créer. */
    @NotBlank(message = "Le nom de la banque est obligatoire")
    private String nom;

    /**
     * Montant initial de capital injecté dans la banque.
     * Doit être supérieur ou égal à 2 000 000 FCFA (Skill 3).
     */
    @NotNull(message = "Le montant initial est obligatoire")
    @DecimalMin(value = "2000000.00",
            message = "Le montant initial doit être supérieur ou égal à 2 000 000 FCFA")
    private BigDecimal montantInitial;
}
