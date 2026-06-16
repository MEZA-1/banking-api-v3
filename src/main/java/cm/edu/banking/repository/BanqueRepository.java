package cm.edu.banking.repository;

import cm.edu.banking.model.Banque;
import cm.edu.banking.model.enums.StatutBanque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Spring Data JPA pour l'entité {@link Banque}.
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Repository
public interface BanqueRepository extends JpaRepository<Banque, Long> {

    /**
     * Vérifie l'existence d'une banque portant un nom donné.
     *
     * @param nom le nom à vérifier
     * @return {@code true} si une banque avec ce nom existe déjà
     */
    boolean existsByNom(String nom);

    /**
     * Récupère toutes les banques ayant un statut donné.
     *
     * @param statut le statut à filtrer ({@link StatutBanque#ACTIVE} ou
     *               {@link StatutBanque#SUSPENDUE})
     * @return la liste des banques correspondantes
     */
    List<Banque> findByStatut(StatutBanque statut);
}
