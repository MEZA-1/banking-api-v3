package cm.edu.banking.service;

import cm.edu.banking.dto.response.CompteBancaireResponse;
import cm.edu.banking.dto.response.OperationResponse;
import cm.edu.banking.dto.response.UserResponse;
import cm.edu.banking.model.CompteBancaire;
import cm.edu.banking.model.Operation;
import cm.edu.banking.model.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Composant utilitaire de conversion (mapping) entre les entités JPA
 * et leurs DTOs de réponse correspondants.
 *
 * <p>Centralise la logique de transformation pour éviter la duplication
 * dans les services et garantir une exposition cohérente des données au
 * niveau de la couche REST.</p>
 *
 * @authorMEZT-coding
 * @since 1.0.0
 */
@Component
public class BankingMapper {

    /**
     * Convertit une entité {@link User} en {@link UserResponse}.
     *
     * @param user l'entité à convertir
     * @return le DTO de réponse correspondant
     */
    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .role(user.getRole())
                .banqueId(user.getBanque() != null ? user.getBanque().getId() : null)
                .nomBanque(user.getBanque() != null ? user.getBanque().getNom() : null)
                .dateCreation(user.getDateCreation())
                .build();
    }

    /**
     * Convertit une entité {@link CompteBancaire} en
     * {@link CompteBancaireResponse}.
     *
     * @param compte l'entité à convertir
     * @return le DTO de réponse correspondant
     */
    public CompteBancaireResponse toCompteBancaireResponse(CompteBancaire compte) {
        User proprietaire = compte.getUtilisateur();
        return CompteBancaireResponse.builder()
                .id(compte.getId())
                .numeroCompte(compte.getNumeroCompte())
                .solde(compte.getSolde())
                .statut(compte.getStatut())
                .utilisateurId(proprietaire.getId())
                .nomProprietaire(proprietaire.getPrenom() + " " + proprietaire.getNom())
                .banqueId(compte.getBanque().getId())
                .nomBanque(compte.getBanque().getNom())
                .dateCreation(compte.getDateCreation())
                .build();
    }

    /**
     * Convertit une entité {@link Operation} en {@link OperationResponse}.
     *
     * <p>Les champs liés à l'émetteur et au destinataire sont nullables
     * selon le type d'opération (voir Javadoc de {@link Operation}).</p>
     *
     * @param op l'entité opération à convertir
     * @return le DTO de réponse correspondant
     */
    public OperationResponse toOperationResponse(Operation op) {
        BigDecimal montantTotal = op.getMontant().add(op.getFrais());

        String numeroEmetteur = null;
        String nomEmetteur = null;
        if (op.getCompteEmetteur() != null) {
            numeroEmetteur = op.getCompteEmetteur().getNumeroCompte();
            User u = op.getCompteEmetteur().getUtilisateur();
            nomEmetteur = u.getPrenom() + " " + u.getNom();
        }

        String numeroDestinataire = null;
        String nomDestinataire = null;
        if (op.getCompteDestinataire() != null) {
            numeroDestinataire = op.getCompteDestinataire().getNumeroCompte();
            User u = op.getCompteDestinataire().getUtilisateur();
            nomDestinataire = u.getPrenom() + " " + u.getNom();
        }

        return OperationResponse.builder()
                .id(op.getId())
                .type(op.getType())
                .montant(op.getMontant())
                .frais(op.getFrais())
                .montantTotal(montantTotal)
                .numeroCompteEmetteur(numeroEmetteur)
                .nomEmetteur(nomEmetteur)
                .numeroCompteDestinataire(numeroDestinataire)
                .nomDestinataire(nomDestinataire)
                .banqueId(op.getBanque().getId())
                .nomBanque(op.getBanque().getNom())
                .description(op.getDescription())
                .dateOperation(op.getDateOperation())
                .build();
    }
}
