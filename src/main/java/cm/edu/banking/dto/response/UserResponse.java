package cm.edu.banking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import cm.edu.banking.model.enums.Role;
import cm.edu.banking.model.enums.StatutCompte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse représentant un utilisateur (sans données sensibles).
 *
 * <p>Le champ {@code motDePasse} est volontairement absent. Le champ
 * {@code banqueId} est renseigné uniquement pour les rôles
 * {@code SUPERVISEUR} et {@code AGENT}.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private Role role;
    private Long banqueId;
    private String nomBanque;
    
 // --- Compte bancaire associé (AGENT / CLIENT) ---
    /** Identifiant technique du compte bancaire, {@code null} si absent. */
    private Long compteId;

    /** Numéro de compte unique, {@code null} si l'utilisateur n'a pas de compte. */
    private String numeroCompte;

    /**
     * Solde actuel du compte en FCFA, {@code null} si l'utilisateur
     * n'a pas de compte bancaire.
     */
    private BigDecimal soldeCompte;

    /**
     * Statut opérationnel du compte ({@code ACTIF} ou {@code SUSPENDU}),
     * {@code null} si l'utilisateur n'a pas de compte bancaire.
     */
    private StatutCompte statutCompte;
    
    private LocalDateTime dateCreation;
    
    
    
}
