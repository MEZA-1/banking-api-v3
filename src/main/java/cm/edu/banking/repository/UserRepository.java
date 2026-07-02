package cm.edu.banking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cm.edu.banking.model.User;
import cm.edu.banking.model.enums.Role;

/**
 * Repository Spring Data JPA pour l'entité {@link User}.
 *
 * <p>Fournit les requêtes de lecture nécessaires à la couche service,
 * en particulier celles utilisées par la chaîne de sécurité JWT
 * ({@link cm.edu.banking.security.CustomUserDetailsService}) et les
 * vérifications d'unicité lors de l'inscription.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Recherche un utilisateur par son adresse e-mail.
     *
     * <p>Utilisé par {@link cm.edu.banking.security.CustomUserDetailsService}
     * pour charger le principal de sécurité lors de l'authentification JWT,
     * et par le service d'inscription pour vérifier l'unicité de l'email
     * (Skill 2.2).</p>
     *
     * @param email l'adresse e-mail à rechercher (sensible à la casse selon
     *              la collation PostgreSQL configurée)
     * @return un {@link Optional} contenant l'utilisateur trouvé, ou vide
     *         si aucun utilisateur ne possède cet email
     */
    Optional<User> findByEmail(String email);
    
    
 // Cette requête force le chargement de la banque (JOIN FETCH) même si elle est configurée en LAZY
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.banque WHERE u.email = :email")
    Optional<User> findByEmailWithBanque(@Param("email") String email);

    /**
     * Vérifie l'existence d'un utilisateur portant un email donné.
     *
     * @param email l'adresse e-mail à vérifier
     * @return {@code true} si un utilisateur avec cet email existe déjà
     */
    boolean existsByEmail(String email);

    /**
     * Vérifie l'existence d'un utilisateur portant un numéro de téléphone
     * donné.
     *
     * @param telephone le numéro de téléphone à vérifier
     * @return {@code true} si un utilisateur avec ce téléphone existe déjà
     */
    boolean existsByTelephone(String telephone);

    /**
     * Vérifie l'existence d'au moins un utilisateur possédant le rôle
     * {@link Role#ADMIN}.
     *
     * <p>Utilisé lors de l'initialisation de l'application (Skill 2.1)
     * pour déterminer si le Super Admin doit être créé automatiquement.</p>
     *
     * @param role le rôle à vérifier
     * @return {@code true} si au moins un utilisateur avec ce rôle existe
     */
    boolean existsByRole(Role role);

    /**
     * Récupère la liste de tous les utilisateurs d'une banque donnée ayant
     * un rôle spécifique.
     *
     * <p>Utilisé par les superviseurs pour consulter la liste de leurs
     * agents ({@link Role#AGENT}) et par les administrateurs pour lister
     * les superviseurs ({@link Role#SUPERVISEUR}) (Skills 4 et 5).</p>
     *
     * @param banqueId l'identifiant de la banque
     * @param role     le rôle à filtrer
     * @return la liste (éventuellement vide) des utilisateurs correspondants
     */
    List<User> findByBanqueIdAndRole(Long banqueId, Role role);
}
