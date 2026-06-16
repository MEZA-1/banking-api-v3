package cm.edu.banking.model.enums;

/**
 * Énumération représentant les différents rôles applicatifs disponibles
 * au sein de la plateforme bancaire multi-banques.
 *
 * <p>Chaque utilisateur de l'application possède un et un seul rôle, qui
 * détermine l'ensemble des opérations qu'il est autorisé à effectuer.</p>
 *
 * <ul>
 *   <li>{@link #ADMIN} : Administrateur global de la plateforme. Gère les
 *       banques, crée les superviseurs et peut effectuer des retraits
 *       directement sur le {@code montantActif} d'une banque.</li>
 *   <li>{@link #SUPERVISEUR} : Responsable d'une banque. Crée et gère les
 *       agents, supervise les clients de sa banque et approvisionne les
 *       agents en liquidités.</li>
 *   <li>{@link #AGENT} : Agent d'une banque, point de contact physique pour
 *       les opérations de dépôt et de retrait des clients.</li>
 *   <li>{@link #CLIENT} : Utilisateur final détenteur d'un compte bancaire,
 *       pouvant consulter son solde, son historique et effectuer des
 *       transferts.</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
public enum Role {

    /**
     * Administrateur global de la plateforme. Possède les droits les plus
     * élevés : gestion des banques (création, suspension, réactivation),
     * création des superviseurs, consultation globale des utilisateurs et
     * des banques, et retraits limités sur le {@code montantActif} d'une
     * banque.
     */
    ADMIN,

    /**
     * Superviseur rattaché à une banque précise. Peut créer des agents,
     * consulter les agents et clients de sa banque, suspendre des comptes
     * (agents ou clients) et approvisionner les agents en liquidités.
     */
    SUPERVISEUR,

    /**
     * Agent bancaire rattaché à une banque précise. Dispose d'un compte
     * bancaire utilisé comme caisse pour effectuer les dépôts et les
     * retraits au profit des clients.
     */
    AGENT,

    /**
     * Client final de la plateforme. Peut créer son propre compte bancaire,
     * consulter son solde et son historique, et effectuer des transferts
     * internes ou interbancaires.
     */
    CLIENT
}
