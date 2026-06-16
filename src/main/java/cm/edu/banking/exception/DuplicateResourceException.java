package cm.edu.banking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée lors d'une tentative de création d'une ressource dont
 * un champ unique (email, téléphone, numéro de compte, nom de banque)
 * existe déjà en base de données.
 *
 * <p>Retourne automatiquement un HTTP {@code 409 Conflict} au client.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    /**
     * Construit une exception de doublon.
     *
     * @param resourceName nom de la ressource (ex. : "Utilisateur", "Banque")
     * @param fieldName    nom du champ dupliqué (ex. : "email", "telephone")
     * @param fieldValue   valeur dupliquée
     */
    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s avec %s '%s' existe déjà", resourceName, fieldName, fieldValue));
    }
}
