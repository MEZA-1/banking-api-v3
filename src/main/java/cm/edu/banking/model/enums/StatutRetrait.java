/**
 * 
 */
package cm.edu.banking.model.enums;

/**
 * Description de la classe Statusretrait
 *@author meza
 *@since Jun 29, 2026
 */

/**
 * Statuts possibles d'une demande de retrait en attente.
 */
public enum StatutRetrait {
    /** Demande créée, OTP envoyé, en attente de confirmation client. */
    EN_ATTENTE,
    /** OTP et mot de passe validés, retrait exécuté avec succès. */
    CONFIRME,
    /** OTP expiré (fenêtre de 5 minutes dépassée). */
    EXPIRE,
    /** Annulé manuellement par l'agent ou après 3 tentatives échouées. */
    ANNULE
}
