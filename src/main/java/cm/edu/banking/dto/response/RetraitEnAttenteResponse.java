package cm.edu.banking.dto.response;

import cm.edu.banking.model.enums.StatutRetrait;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse retourné après l'initiation d'un retrait
 * ({@code POST /api/agent/retrait/initier}).
 *
 * <p>Contient toutes les informations nécessaires pour que l'agent
 * puisse présenter le récapitulatif au client et collecter son OTP
 * et son mot de passe en vue de la confirmation.</p>
 *
 * <p><strong>Note de sécurité :</strong> le champ {@code codeOtpPreview}
 * est inclus uniquement à des fins de démonstration académique. En
 * production, l'OTP ne doit jamais être retourné dans la réponse API ;
 * il doit uniquement être transmis au client par un canal sécurisé
 * indépendant (SMS, email, etc.).</p>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetraitEnAttenteResponse {

    /** Identifiant unique de la demande de retrait. */
    private Long demandeId;

    // ── Informations client ───────────────────────────────────────
    private Long clientId;
    private String nomClient;
    private String emailClient;
    private String numeroCompteClient;
    private BigDecimal soldeClientAvant;

    // ── Informations agent ────────────────────────────────────────
    private String nomAgent;
    private String numeroCompteAgent;

    // ── Montants ──────────────────────────────────────────────────
    private BigDecimal montant;
    private BigDecimal frais;
    private BigDecimal montantDebite;

    // ── OTP ───────────────────────────────────────────────────────
    /**
     * Code OTP affiché UNIQUEMENT à des fins de démonstration.
     * En production, remplacer par un vrai canal de notification
     * (SMS via Twilio, email via SendGrid, etc.) et supprimer ce champ.
     */
    private String codeOtpPreview;

    /** Date/heure d'expiration de l'OTP (5 minutes après la création). */
    private LocalDateTime expirationOtp;

    /** Statut de la demande : EN_ATTENTE après initiation. */
    private StatutRetrait statut;

    /** Message d'information à afficher à l'agent. */
    private String message;

    private LocalDateTime dateCreation;
}
