package cm.edu.banking.security;

import cm.edu.banking.exception.ResourceNotFoundException;
import cm.edu.banking.model.User;
import cm.edu.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Composant utilitaire permettant de résoudre l'entité {@link User}
 * de l'utilisateur actuellement authentifié à partir du
 * {@link SecurityContextHolder}.
 *
 * <p>Utilisé dans les contrôleurs pour récupérer le principal complet
 * (entité JPA) à partir du simple email stocké dans le token JWT, sans
 * avoir à répéter cette logique dans chaque contrôleur.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Retourne l'entité {@link User} de l'utilisateur actuellement
     * authentifié.
     *
     * <p>Le nom d'utilisateur (email) est extrait du contexte de sécurité
     * Spring, peuplé lors de la validation du token JWT par
     * {@link cm.edu.banking.security.filter.JwtAuthenticationFilter}.</p>
     *
     * @return l'entité {@link User} correspondant à l'utilisateur connecté
     * @throws ResourceNotFoundException si l'email du token ne correspond
     *                                   à aucun utilisateur en base de
     *                                   données (incohérence anormale)
     */
    public User getUtilisateurConnecte() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur", "email", email));
    }
}
