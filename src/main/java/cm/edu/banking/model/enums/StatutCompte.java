package cm.edu.banking.model.enums;

/**
 * Énumération représentant le statut opérationnel d'un {@link
 * cm.edu.banking.model.CompteBancaire}.
 *
 * <p>Conformément à la règle de suspension (Skill 15), un compte dont le
 * statut est {@link #SUSPENDU} ne peut :</p>
 * <ul>
 *   <li>effectuer un dépôt ;</li>
 *   <li>effectuer un retrait ;</li>
 *   <li>effectuer un transfert (en tant qu'émetteur) ;</li>
 *   <li>recevoir un transfert (en tant que destinataire).</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
public enum StatutCompte {

    /**
     * Le compte bancaire est actif et peut participer normalement à toutes
     * les opérations financières prévues par le système (dépôts, retraits,
     * transferts internes et interbancaires), sous réserve que la banque
     * associée ne soit pas elle-même suspendue.
     */
    ACTIF,

    /**
     * Le compte bancaire est suspendu. Toute tentative de dépôt, de
     * retrait, ou de transfert (émission ou réception) impliquant ce
     * compte doit être rejetée par le système.
     */
    SUSPENDU
}
