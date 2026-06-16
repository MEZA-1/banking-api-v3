package cm.edu.banking.repository;

import cm.edu.banking.model.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository Spring Data JPA pour l'entité {@link Operation}.
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Repository
public interface OperationRepository extends JpaRepository<Operation, Long> {

    /**
     * Récupère de façon paginée toutes les opérations impliquant un
     * compte bancaire donné (en tant qu'émetteur ou destinataire).
     *
     * <p>Utilisé pour exposer l'historique paginé des opérations d'un
     * agent ou d'un client (Skill 14).</p>
     *
     * @param compteId l'identifiant du compte bancaire
     * @param pageable paramètres de pagination et de tri
     * @return une page d'opérations impliquant le compte
     */
    @Query("""
            SELECT o FROM Operation o
            WHERE o.compteEmetteur.id = :compteId
               OR o.compteDestinataire.id = :compteId
            ORDER BY o.dateOperation DESC
            """)
    Page<Operation> findByCompteId(@Param("compteId") Long compteId, Pageable pageable);

    /**
     * Récupère de façon paginée toutes les opérations d'une banque donnée,
     * triées de la plus récente à la plus ancienne.
     *
     * <p>Utilisé par les administrateurs et superviseurs pour consulter
     * l'historique global d'une banque.</p>
     *
     * @param banqueId l'identifiant de la banque
     * @param pageable paramètres de pagination et de tri
     * @return une page d'opérations liées à la banque
     */
    Page<Operation> findByBanqueIdOrderByDateOperationDesc(Long banqueId, Pageable pageable);
}
