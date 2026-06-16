package cm.edu.banking.dto.response;

import cm.edu.banking.model.enums.TypeOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse représentant une opération financière de l'historique
 * (Skill 14).
 *
 * @author   MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationResponse {

    private Long id;
    private TypeOperation type;
    private BigDecimal montant;
    private BigDecimal frais;
    private BigDecimal montantTotal;

    /** Numéro du compte émetteur (peut être null selon le type). */
    private String numeroCompteEmetteur;
    private String nomEmetteur;

    /** Numéro du compte destinataire (peut être null selon le type). */
    private String numeroCompteDestinataire;
    private String nomDestinataire;

    private Long banqueId;
    private String nomBanque;
    private String description;
    private LocalDateTime dateOperation;
}
