package cm.edu.banking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requête pour la création d'un compte bancaire par un client
 * (Skill 7 & 8).
 *
 * <p>Règles métier :</p>
 * <ul>
 *   <li>Un client ne peut posséder qu'un seul compte bancaire.</li>
 *   <li>Le solde initial est obligatoirement nul (Skill 8) ; ce champ
 *       n'est pas accepté dans la requête.</li>
 *   <li>Le client choisit la banque dans laquelle ouvrir son compte.</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompteBancaireRequest {

    /**
     * Identifiant de la banque dans laquelle le client souhaite ouvrir
     * son compte. La banque doit être active.
     */
    @NotNull(message = "L'identifiant de la banque est obligatoire")
    private Long banqueId;
}
