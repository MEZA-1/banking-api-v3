package cm.edu.banking.dto.response;

import cm.edu.banking.model.enums.StatutCompte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse représentant un compte bancaire.
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompteBancaireResponse {

    private Long id;
    private String numeroCompte;
    private BigDecimal solde;
    private StatutCompte statut;
    private Long utilisateurId;
    private String nomProprietaire;
    private Long banqueId;
    private String nomBanque;
    private LocalDateTime dateCreation;
}
