package cm.edu.banking.dto.response;

import cm.edu.banking.model.enums.StatutBanque;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse représentant une banque, exposé par les endpoints
 * de gestion des banques.
 *
 * <p>Ne contient jamais de listes d'utilisateurs ou de comptes pour
 * éviter l'exposition massive de données sensibles.</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanqueResponse {

    private Long id;
    private String nom;
    private StatutBanque statut;
    private BigDecimal montantInitial;
    private BigDecimal montantActif;
    private BigDecimal montantLigne;
    private LocalDateTime dateCreation;
}
