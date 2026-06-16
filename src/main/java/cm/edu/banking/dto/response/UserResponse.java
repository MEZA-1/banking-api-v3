package cm.edu.banking.dto.response;

import cm.edu.banking.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private LocalDateTime dateCreation;
}
