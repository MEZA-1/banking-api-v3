package cm.edu.banking.service;

import cm.edu.banking.repository.RetraitEnAttenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Composant Spring chargé d'expirer automatiquement les demandes de
 * retrait dont le code OTP a dépassé sa fenêtre de validité (5 minutes).
 *
 * <p>La tâche planifiée s'exécute toutes les 60 secondes via
 * {@link Scheduled}, garantissant qu'aucune demande expirée ne reste en
 * statut {@link cm.edu.banking.model.RetraitEnAttente.StatutRetrait#EN_ATTENTE}.</p>
 *
 * <p>Pour activer le scheduling, la classe principale
 * {@link cm.edu.banking.BankingApplication} doit être annotée avec
 * {@link org.springframework.scheduling.annotation.EnableScheduling}.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetraitCleanupScheduler {

    private final RetraitEnAttenteRepository retraitEnAttenteRepository;

    /**
     * Tâche planifiée : expire toutes les demandes de retrait dont l'OTP
     * est dépassé. S'exécute toutes les 60 secondes.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireOtpsExpires() {
        int count = retraitEnAttenteRepository.expireOldRequests(LocalDateTime.now());
        if (count > 0) {
            log.info("[SCHEDULER] {} demande(s) de retrait expirée(s) automatiquement.", count);
        }
    }
}