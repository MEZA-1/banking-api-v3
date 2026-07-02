package cm.edu.banking.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import cm.edu.banking.exception.ResourceNotFoundException;
import cm.edu.banking.model.Banque;
import cm.edu.banking.model.User;
import cm.edu.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional; // IMPORTANT pour le FetchType.LAZY

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
        return userRepository.findByEmailWithBanque(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur", "email", email));
    }
    /*
    /**
     * Retourne la {@link Banque} de l'utilisateur actuellement authentifié
     * en fonction de son rôle (Directement ou via son compte bancaire).
     *
     * @return l'entité {@link Banque}
     * @throws ResourceNotFoundException si l'utilisateur n'a pas ou n'est lié à aucune banque (ex: ADMIN)
     */
    /*@Transactional(readOnly = true) // Permet de charger les relations LAZY (banque et compteBancaire)
    public Banque getBanqueConnectee() {
        User uer = getUtilisateurConnecte();
        
        User user = userRepository.findById(uer.getId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Utilisateur", "email", uer.getEmail()));
        
        // Cas 1 : Rôle SUPERVISEUR ou AGENT (Rattaché directement à l'entité User)
        if (user.getBanque() != null) {
            return user.getBanque();
        }

        // Cas 2 : Rôle CLIENT (La banque est déterminée via son CompteBancaire)
        // Note : On suppose ici que ton entité CompteBancaire possède une méthode getBanque()
        if (user.getCompteBancaire() != null && user.getCompteBancaire().getBanque() != null) {
            return user.getCompteBancaire().getBanque();
        }

        // Cas 3 : Rôle ADMIN (ou utilisateur sans compte/banque configuré)
        throw new ResourceNotFoundException("Banque", "utilisateur (Rôle: " + user.getRole() + ")", user.getEmail());
    }*/
}
