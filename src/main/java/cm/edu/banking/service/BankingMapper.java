package cm.edu.banking.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import cm.edu.banking.dto.response.CompteBancaireResponse;
import cm.edu.banking.dto.response.OperationResponse;
import cm.edu.banking.dto.response.RetraitEnAttenteResponse;
import cm.edu.banking.dto.response.UserResponse;
import cm.edu.banking.model.CompteBancaire;
import cm.edu.banking.model.Operation;
import cm.edu.banking.model.RetraitEnAttente;
import cm.edu.banking.model.User;
import cm.edu.banking.model.enums.StatutRetrait;

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
     * Convertit un {@link User} en {@link UserResponse} sans compte associé.
     *
     * <p>Utiliser {@link #toUserResponse(User, CompteBancaire)} dès qu'un
     * compte bancaire est disponible pour peupler les champs
     * {@code numeroCompte}, {@code soldeCompte} et {@code statutCompte}.</p>
     *
     * @param user l'entité à convertir
     * @return le DTO de réponse (champs compte = {@code null})
     */
    public UserResponse toUserResponse(User user) {
        return toUserResponse(user, user.getCompteBancaire());
    }

    /**
     * Convertit un {@link User} en {@link UserResponse} en embarquant
     * les informations du compte bancaire associé lorsqu'il est fourni.
     *
     * <p>Cette surcharge est utilisée dans les méthodes de listing du
     * {@link SuperviseurService} où le compte est déjà chargé en mémoire
     * (évite un accès Lazy supplémentaire).</p>
     *
     * @param user   l'entité utilisateur à convertir
     * @param compte le compte bancaire associé, peut être {@code null}
     * @return le DTO de réponse avec les champs compte renseignés si disponibles
     */
    public UserResponse toUserResponse(User user, CompteBancaire compte) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .role(user.getRole())
                .banqueId(user.getBanque() != null ? user.getBanque().getId() : null)
                .nomBanque(user.getBanque() != null ? user.getBanque().getNom() : null)
                .dateCreation(user.getDateCreation());

        if (compte != null) {
            builder.compteId(compte.getId())
                   .numeroCompte(compte.getNumeroCompte())
                   .soldeCompte(compte.getSolde())
                   .statutCompte(compte.getStatut());
        }

        return builder.build();
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


