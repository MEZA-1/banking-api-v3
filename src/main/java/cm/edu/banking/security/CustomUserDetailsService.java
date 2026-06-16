package cm.edu.banking.security;

import cm.edu.banking.model.User;
import cm.edu.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implémentation de {@link UserDetailsService} chargée par Spring Security
 * pour résoudre le principal de sécurité à partir d'une adresse e-mail.
 *
 * <p>Cette classe fait le pont entre la couche JPA ({@link UserRepository})
 * et la chaîne de filtres Spring Security. Elle est invoquée :</p>
 * <ul>
 *   <li>lors de l'authentification par login/password ;</li>
 *   <li>lors de la validation d'un token JWT entrant, au sein du filtre
 *       {@link cm.edu.banking.security.filter.JwtAuthenticationFilter}.</li>
 * </ul>
 *
 * <p>Le rôle de l'utilisateur est exposé sous la forme d'une autorité
 * Spring préfixée {@code ROLE_} (convention Spring Security), permettant
 * l'usage de {@code hasRole("ADMIN")} dans les expressions de sécurité.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Charge un {@link UserDetails} à partir de l'adresse e-mail fournie
     * comme nom d'utilisateur.
     *
     * <p>La méthode est annotée {@link Transactional} en lecture seule
     * pour s'assurer que la session Hibernate reste ouverte pendant la
     * résolution de l'entité {@link User}, évitant ainsi toute
     * {@code LazyInitializationException} potentielle.</p>
     *
     * @param email l'adresse e-mail de l'utilisateur (utilisée comme
     *              identifiant de connexion)
     * @return un {@link UserDetails} contenant les informations de sécurité
     *         de l'utilisateur
     * @throws UsernameNotFoundException si aucun utilisateur ne possède
     *                                   l'email fourni
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Aucun utilisateur trouvé avec l'email : " + email));

        // Le rôle est préfixé ROLE_ selon la convention Spring Security
        // (permet d'utiliser hasRole("ADMIN") dans les expressions SpEL)
        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getMotDePasse(),
                List.of(authority)
        );
    }
}
