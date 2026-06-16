package cm.edu.banking.repository;

import cm.edu.banking.model.CompteBancaire;
import cm.edu.banking.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité {@link CompteBancaire}.
 *
 * <p>Fournit en particulier la requête de calcul du
 * {@code montantLigne} utilisée après chaque opération financière
 * (Skill 13).</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Repository
public interface CompteBancaireRepository extends JpaRepository<CompteBancaire, Long> {

    /**
     * Recherche le compte bancaire d'un utilisateur par son identifiant.
     *
     * @param utilisateurId l'identifiant de l'utilisateur
     * @return un {@link Optional} contenant le compte trouvé
     */
    Optional<CompteBancaire> findByUtilisateurId(Long utilisateurId);

    /**
     * Vérifie si un utilisateur possède déjà un compte bancaire.
     *
     * @param utilisateurId l'identifiant de l'utilisateur
     * @return {@code true} si un compte existe pour cet utilisateur
     */
    boolean existsByUtilisateurId(Long utilisateurId);

    /**
     * Vérifie l'existence d'un compte portant un numéro donné.
     *
     * @param numeroCompte le numéro de compte à vérifier
     * @return {@code true} si un compte avec ce numéro existe déjà
     */
    boolean existsByNumeroCompte(String numeroCompte);

    /**
     * Calcule la somme des soldes de tous les comptes bancaires
     * appartenant aux clients ({@link Role#CLIENT}) et agents
     * ({@link Role#AGENT}) rattachés à une banque donnée.
     *
     * <p>Ce calcul correspond à la définition du {@code montantLigne}
     * (Skill 13) et est exécuté après chaque opération financière afin
     * de maintenir cet indicateur à jour de façon atomique dans la même
     * transaction.</p>
     *
     * @param banqueId l'identifiant de la banque concernée
     * @return la somme des soldes des comptes clients et agents de la
     *         banque, ou {@link BigDecimal#ZERO} si aucun compte n'existe
     */
    @Query("""
            SELECT COALESCE(SUM(c.solde), 0)
            FROM CompteBancaire c
            WHERE c.banque.id = :banqueId
              AND c.utilisateur.role IN (
                  cm.edu.banking.model.enums.Role.CLIENT,
                  cm.edu.banking.model.enums.Role.AGENT
              )
            """)
    BigDecimal calculerMontantLigne(@Param("banqueId") Long banqueId);

    /**
     * Récupère tous les comptes bancaires associés à une banque donnée
     * dont les propriétaires ont un rôle spécifique.
     *
     * @param banqueId l'identifiant de la banque
     * @param role     le rôle à filtrer
     * @return la liste des comptes correspondants
     */
    List<CompteBancaire> findByBanqueIdAndUtilisateurRole(Long banqueId, Role role);
}
