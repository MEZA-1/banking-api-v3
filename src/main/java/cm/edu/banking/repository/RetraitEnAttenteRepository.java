package cm.edu.banking.repository;

import cm.edu.banking.model.RetraitEnAttente;
import cm.edu.banking.model.enums.StatutRetrait;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité {@link RetraitEnAttente}.
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Repository
public interface RetraitEnAttenteRepository extends JpaRepository<RetraitEnAttente, Long> {

    /**
     * Recherche une demande de retrait active (statut EN_ATTENTE) pour un
     * compte client donné.
     *
     * <p>Utilisé pour détecter une demande déjà en cours et empêcher
     * un agent de créer un second retrait simultané pour le même client.</p>
     *
     * @param compteClientId identifiant du compte client
     * @param statut         statut à filtrer (EN_ATTENTE)
     * @return la demande active si elle existe
     */
    Optional<RetraitEnAttente> findByCompteClientIdAndStatut(
            Long compteClientId, StatutRetrait statut);

    /**
     * Expire automatiquement toutes les demandes dont l'OTP est dépassé
     * et dont le statut est encore EN_ATTENTE.
     *
     * <p>Cette méthode est appelée par le scheduler
     * {@link cm.edu.banking.service.RetraitCleanupScheduler} toutes les
     * minutes.</p>
     *
     * @param now date/heure courante pour comparaison avec {@code expirationOtp}
     * @return le nombre de demandes expirées
     */
    @Modifying
    @Query("""
            UPDATE RetraitEnAttente r
            SET r.statut = cm.edu.banking.model.enums.StatutRetrait.EXPIRE,
                r.dateMiseAJour = :now
            WHERE r.statut = cm.edu.banking.model.enums.StatutRetrait.EN_ATTENTE
              AND r.expirationOtp < :now
            """)
    int expireOldRequests(@Param("now") LocalDateTime now);
}