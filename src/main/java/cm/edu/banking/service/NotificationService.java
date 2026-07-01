package cm.edu.banking.service;

import cm.edu.banking.model.CompteBancaire;
import cm.edu.banking.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service de notification chargé d'informer le client qu'un retrait
 * est en attente de sa confirmation via code OTP.
 *
 * <p>Implémentation actuelle : affichage dans les logs de l'application
 * (mode démonstration académique). Pour une mise en production, remplacer
 * le corps des méthodes par des appels à un fournisseur tiers :</p>
 * <ul>
 *   <li><strong>SMS :</strong> Twilio, Orange SMS API, MTN SMS API</li>
 *   <li><strong>Email :</strong> Spring Mail + SMTP, SendGrid, Mailgun</li>
 *   <li><strong>Push notification :</strong> Firebase Cloud Messaging</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Service
@Slf4j
public class NotificationService {

    /**
     * Notifie le client qu'une demande de retrait a été initiée par un
     * agent et qu'il doit fournir son code OTP + mot de passe pour la
     * confirmer.
     *
     * <p>Le message inclut le montant, les frais, le montant total débité
     * et le code OTP valable 5 minutes.</p>
     *
     * @param client       le client titulaire du compte à débiter
     * @param compteClient le compte bancaire du client
     * @param montant      le montant demandé (hors frais)
     * @param frais        les frais calculés (2 % du montant)
     * @param montantTotal le montant total débité (montant + frais)
     * @param codeOtp      le code OTP à 6 chiffres à transmettre au client
     */
    public void envoyerOtpRetrait(
            User client,
            CompteBancaire compteClient,
            BigDecimal montant,
            BigDecimal frais,
            BigDecimal montantTotal,
            String codeOtp) {

        // ── Simulation console ────────────────────────────────────────
        // En production : envoyer le SMS/email ci-dessous et supprimer les logs.
        String messageClient = buildMessage(
                client.getPrenom() + " " + client.getNom(),
                compteClient.getNumeroCompte(),
                montant, frais, montantTotal, codeOtp);

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║         NOTIFICATION RETRAIT — CODE OTP                 ║");
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info("║ Destinataire : {} <{}>", client.getPrenom() + " " + client.getNom(), client.getEmail());
        log.info("║ Téléphone    : {}", client.getTelephone());
        log.info("║ Compte       : {}", compteClient.getNumeroCompte());
        log.info("║ Montant      : {} FCFA", montant);
        log.info("║ Frais (2%)   : {} FCFA", frais);
        log.info("║ Total débité : {} FCFA", montantTotal);
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info("║ >>>  CODE OTP : {}  <<<  (valable 5 minutes)          ║", codeOtp);
        log.info("╚══════════════════════════════════════════════════════════╝");

        // ── Placeholder SMS (Twilio) ──────────────────────────────────
        // Décommenter et configurer les dépendances Twilio pour activer :
        //
        // Message message = Message.creator(
        //     new PhoneNumber(client.getTelephone()),
        //     new PhoneNumber(twilioFromNumber),
        //     messageClient
        // ).create();
        // log.info("SMS envoyé au {} : SID={}", client.getTelephone(), message.getSid());

        // ── Placeholder Email (Spring Mail) ───────────────────────────
        // SimpleMailMessage mail = new SimpleMailMessage();
        // mail.setTo(client.getEmail());
        // mail.setSubject("Banking API — Confirmation de retrait");
        // mail.setText(messageClient);
        // mailSender.send(mail);
        // log.info("Email OTP envoyé à {}", client.getEmail());
    }

    /**
     * Construit le texte du message de notification envoyé au client.
     *
     * @param nomClient    nom complet du client
     * @param numeroCompte numéro de compte débité
     * @param montant      montant hors frais
     * @param frais        frais (2 %)
     * @param montantTotal total débité
     * @param codeOtp      code OTP à 6 chiffres
     * @return le texte formaté du message
     */
    private String buildMessage(String nomClient, String numeroCompte,
                                BigDecimal montant, BigDecimal frais,
                                BigDecimal montantTotal, String codeOtp) {
        return String.format(
            "Bonjour %s,%n%n" +
            "Un retrait de %s FCFA (frais : %s FCFA, total : %s FCFA) " +
            "a été initié sur votre compte %s.%n%n" +
            "Votre code de confirmation : %s%n%n" +
            "Ce code est valable 5 minutes. " +
            "Communiquez-le à l'agent pour valider le retrait.%n" +
            "Si vous n'êtes pas à l'origine de cette demande, " +
            "contactez immédiatement votre banque.%n%n" +
            "Banking API CEMAC",
            nomClient, montant, frais, montantTotal, numeroCompte, codeOtp
        );
    }
}