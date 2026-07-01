package cm.edu.banking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import cm.edu.banking.model.enums.StatutRetrait;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entité JPA représentant une demande de retrait initiée par un agent
 * et en attente de confirmation par le client via code OTP et mot de passe.
 *
 * <p>Cycle de vie :</p>
 * <ol>
 *   <li>L'agent appelle {@code POST /api/agent/retrait/initier} → une instance
 *       de {@code RetraitEnAttente} est créée avec un code OTP à 6 chiffres
 *       et un statut {@link StatutRetrait#EN_ATTENTE}.</li>
 *   <li>Le code OTP est transmis au client (log console / SMS / email selon
 *       la configuration de {@link cm.edu.banking.service.NotificationService}).</li>
 *   <li>Le client communique son OTP à l'agent qui appelle
 *       {@code POST /api/agent/retrait/confirmer} avec le code OTP et le
 *       mot de passe du client.</li>
 *   <li>Si les deux facteurs sont valides et que l'OTP n'est pas expiré
 *       (fenêtre de 5 minutes), le retrait est exécuté et le statut passe
 *       à {@link StatutRetrait#CONFIRME}.</li>
 *   <li>En cas d'échec ou d'expiration, le statut passe à
 *       {@link StatutRetrait#EXPIRE} ou {@link StatutRetrait#ANNULE}.</li>
 * </ol>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Entity
@Table(name = "retraits_en_attente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"compteClient", "compteAgent"})
public class RetraitEnAttente {

    /** Durée de validité d'un OTP en minutes. */
    public static final int OTP_VALIDITY_MINUTES = 5;

    // ── Clé primaire ─────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Parties impliquées ────────────────────────────────────────

    /**
     * Compte bancaire du client qui effectue le retrait.
     * C'est ce compte qui sera débité (montant + frais 2 %) lors de
     * la confirmation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_client_id", nullable = false)
    private CompteBancaire compteClient;

    /**
     * Compte bancaire (caisse) de l'agent qui traite le retrait.
     * Ce compte sera crédité du montant (sans les frais) lors de la
     * confirmation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_agent_id", nullable = false)
    private CompteBancaire compteAgent;

    // ── Montants ──────────────────────────────────────────────────

    /** Montant souhaité par le client (hors frais), en FCFA. */
    @Column(name = "montant", nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    /**
     * Frais calculés au moment de l'initiation (2 % du montant).
     * Stockés ici pour garantir la cohérence lors de la confirmation
     * même si des paramètres venaient à changer entre les deux appels.
     */
    @Column(name = "frais", nullable = false, precision = 19, scale = 2)
    private BigDecimal frais;

    /** Montant total qui sera débité du compte client (montant + frais). */
    @Column(name = "montant_debite", nullable = false, precision = 19, scale = 2)
    private BigDecimal montantDebite;

    // ── OTP ───────────────────────────────────────────────────────

    /**
     * Code OTP (One-Time Password) à 6 chiffres généré aléatoirement lors
     * de l'initiation. Transmis au client via
     * {@link cm.edu.banking.service.NotificationService}.
     */
    @Column(name = "code_otp", nullable = false, length = 6)
    private String codeOtp;

    /**
     * Date et heure d'expiration de l'OTP.
     * Calculée lors de la création :
     * {@code dateCreation + OTP_VALIDITY_MINUTES minutes}.
     */
    @Column(name = "expiration_otp", nullable = false)
    private LocalDateTime expirationOtp;

    // ── Statut ────────────────────────────────────────────────────

    /**
     * Statut courant de la demande de retrait.
     * Valeur initiale : {@link StatutRetrait#EN_ATTENTE}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private StatutRetrait statut = StatutRetrait.EN_ATTENTE;

    /** Description ou motif optionnel saisi par l'agent. */
    @Column(name = "description", length = 255)
    private String description;

    /** Nombre de tentatives de confirmation échouées (max 3). */
    @Column(name = "tentatives", nullable = false)
    @Builder.Default
    private int tentatives = 0;

    // ── Audit ─────────────────────────────────────────────────────

    /** Date et heure de création de la demande. */
    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /** Date et heure de la dernière mise à jour du statut. */
    @Column(name = "date_mise_a_jour")
    private LocalDateTime dateMiseAJour;

    // ── Méthodes utilitaires ──────────────────────────────────────

    /**
     * Vérifie si l'OTP est encore dans sa fenêtre de validité.
     *
     * @return {@code true} si l'OTP n'est pas encore expiré
     */
    public boolean isOtpValide() {
        return LocalDateTime.now().isBefore(expirationOtp);
    }

}