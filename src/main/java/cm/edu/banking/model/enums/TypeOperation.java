package cm.edu.banking.model.enums;

/**
 * Énumération représentant la nature d'une {@link
 * cm.edu.banking.model.Operation} enregistrée dans l'historique du
 * système.
 *
 * <p>Chaque type d'opération correspond à une règle de calcul de frais et
 * à un impact spécifique sur les soldes des comptes concernés et sur les
 * montants agrégés (<code>montantActif</code> et <code>montantLigne</code>)
 * de la ou des banques impliquées.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
public enum TypeOperation {

    /**
     * Dépôt effectué par un agent au profit d'un client. Aucun frais
     * n'est appliqué : le montant est débité du compte de l'agent et
     * crédité sur le compte du client (Skill 9).
     */
    DEPOT,

    /**
     * Retrait effectué par un client auprès d'un agent. Des frais de 2 %
     * sont appliqués sur le montant retiré : le compte client est débité
     * du montant et des frais, le compte agent est crédité du montant
     * (hors frais), et les frais sont ajoutés au {@code montantActif} de
     * la banque (Skill 10).
     */
    RETRAIT,

    /**
     * Transfert entre deux clients appartenant à la même banque. Des
     * frais de 2 % sont appliqués sur le montant transféré. Le compte
     * émetteur est débité du montant et des frais, le compte
     * destinataire est crédité du montant, et les frais sont ajoutés au
     * {@code montantActif} de la banque (Skill 11).
     */
    TRANSFERT_INTERNE,

    /**
     * Transfert entre deux clients appartenant à des banques différentes.
     * Des frais de 4 % sont appliqués sur le montant transféré. Le compte
     * émetteur est débité du montant et des frais, le compte
     * destinataire est crédité du montant, et les frais sont ajoutés au
     * {@code montantActif} de la banque émettrice (Skill 12).
     */
    TRANSFERT_INTERBANCAIRE,

    /**
     * Approvisionnement d'un agent par son superviseur. Le montant est
     * débité du {@code montantActif} de la banque et crédité sur le
     * compte de l'agent (Skill 5).
     */
    APPROVISIONNEMENT_AGENT,

    /**
     * Retrait effectué directement par un administrateur sur le
     * {@code montantActif} d'une banque, plafonné à 1 000 000 FCFA par
     * opération (Skill 4).
     */
    RETRAIT_ADMIN
}
