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
 * DTO de requête pour un transfert de fonds initié par un client,
 * qu'il soit interne (même banque, Skill 11) ou interbancaire
 * (banques différentes, Skill 12).
 *
 * <p>Le type de transfert (interne vs interbancaire) est déterminé
 * automatiquement par le service en comparant les banques de rattachement
 * des comptes émetteur et destinataire.</p>
 *
 * <p>Frais applicables :</p>
 * <ul>
 *   <li><strong>Transfert interne</strong> : 2 % → frais vont au
 *       {@code montantActif} de la banque commune.</li>
 *   <li><strong>Transfert interbancaire</strong> : 4 % → frais vont au
 *       {@code montantActif} de la banque émettrice.</li>
 * </ul>
 *
 * <p>Dans les deux cas :</p>
 * <pre>
 *   frais            = montant × taux
 *   compteEmetteur  -= montant + frais
 *   compteDestinataire += montant
 *   banqueEmettrice.montantActif += frais
 * </pre>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransfertRequest {

    /**
     * Numéro de compte du destinataire. Utilisé pour identifier le
     * compte cible indépendamment de la banque, ce qui permet au service
     * de détecter automatiquement s'il s'agit d'un transfert interne
     * ou interbancaire.
     */
    @NotBlank(message = "Le numéro de compte destinataire est obligatoire")
    private String numeroCompteDestinataire;

    /**
     * Montant à transférer en FCFA (hors frais).
     * Doit être strictement positif.
     */
    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "1.00", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    /** Description ou motif optionnel du transfert. */
    private String description;
}
