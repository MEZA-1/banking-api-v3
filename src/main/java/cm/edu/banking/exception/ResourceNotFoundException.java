package cm.edu.banking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée lorsqu'une ressource demandée est introuvable en base
 * de données (par exemple un utilisateur, un compte ou une banque avec
 * un identifiant inexistant).
 *
 * <p>Retourne automatiquement un HTTP {@code 404 Not Found} au client.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Construit une exception de ressource introuvable.
     *
     * @param resourceName nom de la ressource (ex. : "Utilisateur", "Banque")
     * @param fieldName    nom du champ de recherche (ex. : "id", "email")
     * @param fieldValue   valeur du champ utilisée dans la recherche
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s introuvable avec %s : '%s'", resourceName, fieldName, fieldValue));
    }
}
