package cm.edu.banking.model.enums;

/**
 * Énumération représentant le statut opérationnel d'une {@link
 * cm.edu.banking.model.Banque}.
 *
 * <p>Le statut d'une banque a un impact direct sur l'ensemble des
 * utilisateurs qui en dépendent : lorsqu'une banque est suspendue, toutes
 * les opérations financières (dépôts, retraits, transferts internes et
 * interbancaires) initiées par les utilisateurs de cette banque sont
 * automatiquement bloquées.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
public enum StatutBanque {

    /**
     * La banque est active : toutes les opérations financières de ses
     * utilisateurs (agents et clients) sont autorisées, sous réserve du
     * respect des autres règles métier (solde suffisant, compte non
     * suspendu, etc.).
     */
    ACTIVE,

    /**
     * La banque est suspendue : toutes les opérations financières
     * (dépôts, retraits, transferts internes et interbancaires) des
     * utilisateurs rattachés à cette banque sont bloquées, qu'il
     * s'agisse d'agents ou de clients.
     */
    SUSPENDUE
}
