package cm.edu.banking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception métier générique de la plateforme bancaire.
 *
 * <p>Utilisée pour signaler une violation d'une règle métier
 * (solde insuffisant, plafond dépassé, opération interdite sur compte
 * ou banque suspendu·e, etc.) et retourner automatiquement un
 * HTTP {@code 400 Bad Request} au client.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BankingException extends RuntimeException {

    /**
     * Construit une exception métier avec un message descriptif.
     *
     * @param message description claire de la règle métier violée
     */
    public BankingException(String message) {
        super(message);
    }

    /**
     * Construit une exception métier avec un message et une cause racine.
     *
     * @param message description claire de la règle métier violée
     * @param cause   exception d'origine ayant provoqué l'erreur
     */
    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
