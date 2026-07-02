package cm.edu.banking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cm.edu.banking.dto.response.BanqueActiveProjection;
import cm.edu.banking.model.Banque;
import cm.edu.banking.model.enums.StatutBanque;

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
    
    @Query("SELECT b.id as id, b.nom as nom FROM Banque b WHERE b.statut = cm.edu.banking.model.enums.StatutBanque.ACTIVE ")
     List<BanqueActiveProjection> findAllActiveBanques();


}
