package cm.edu.banking.model;

import cm.edu.banking.model.enums.TypeOperation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant une opération financière enregistrée dans
 * l'historique du système (Skill 14).
 *
 * <p>Une instance de {@code Operation} est créée systématiquement pour
 * chacun des types d'opérations suivants :</p>
 * <ul>
 *   <li>{@link TypeOperation#DEPOT} : dépôt agent → client ;</li>
 *   <li>{@link TypeOperation#RETRAIT} : retrait client via un agent ;</li>
 *   <li>{@link TypeOperation#TRANSFERT_INTERNE} : transfert entre deux
 *       clients d'une même banque ;</li>
 *   <li>{@link TypeOperation#TRANSFERT_INTERBANCAIRE} : transfert entre
 *       deux clients de banques différentes ;</li>
 *   <li>{@link TypeOperation#APPROVISIONNEMENT_AGENT} : approvisionnement
 *       d'un agent par son superviseur ;</li>
 *   <li>{@link TypeOperation#RETRAIT_ADMIN} : retrait effectué par un
 *       administrateur sur le {@code montantActif} d'une banque.</li>
 * </ul>
 *
 * <p>Selon le type d'opération, les champs {@code compteEmetteur} et
 * {@code compteDestinataire} peuvent être nuls :</p>
 * <ul>
 *   <li><strong>DEPOT</strong> : émetteur = compte de l'agent, destinataire = compte du client ;</li>
 *   <li><strong>RETRAIT</strong> : émetteur = compte du client, destinataire = compte de l'agent ;</li>
 *   <li><strong>TRANSFERT_INTERNE / TRANSFERT_INTERBANCAIRE</strong> : émetteur = compte source, destinataire = compte cible ;</li>
 *   <li><strong>APPROVISIONNEMENT_AGENT</strong> : émetteur = {@code null} (provient du {@code montantActif} de la banque), destinataire = compte de l'agent ;</li>
 *   <li><strong>RETRAIT_ADMIN</strong> : émetteur = {@code null} (provient du {@code montantActif} de la banque), destinataire = {@code null}.</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Entity
@Table(name = "operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"compteEmetteur", "compteDestinataire", "banque"})
public class Operation {

    /**
     * Identifiant technique unique de l'opération, généré automatiquement
     * par la base de données.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Type de l'opération financière (dépôt, retrait, transfert, etc.).
     *
     * @see TypeOperation
     */
    @NotNull(message = "Le type d'opération est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TypeOperation type;

    /**
     * Montant principal de l'opération, hors frais, exprimé en FCFA.
     *
     * <p>Pour un retrait ou un transfert, ce montant correspond à la
     * valeur effectivement créditée sur le compte destinataire. Le montant
     * réellement débité du compte émetteur correspond à
     * {@code montant + frais}.</p>
     */
    @NotNull(message = "Le montant de l'opération est obligatoire")
    @Column(name = "montant", nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    /**
     * Montant des frais prélevés lors de l'opération, exprimé en FCFA.
     *
     * <p>Valeurs attendues selon le type d'opération :</p>
     * <ul>
     *   <li>{@link TypeOperation#DEPOT} : toujours {@code 0} (aucun frais, Skill 9) ;</li>
     *   <li>{@link TypeOperation#RETRAIT} : {@code montant × 2 %} (Skill 10) ;</li>
     *   <li>{@link TypeOperation#TRANSFERT_INTERNE} : {@code montant × 2 %} (Skill 11) ;</li>
     *   <li>{@link TypeOperation#TRANSFERT_INTERBANCAIRE} : {@code montant × 4 %} (Skill 12) ;</li>
     *   <li>{@link TypeOperation#APPROVISIONNEMENT_AGENT} et
     *       {@link TypeOperation#RETRAIT_ADMIN} : toujours {@code 0}.</li>
     * </ul>
     */
    @NotNull(message = "Le montant des frais est obligatoire")
    @Column(name = "frais", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal frais = BigDecimal.ZERO;

    /**
     * Compte bancaire à l'origine de l'opération (compte débité).
     *
     * <p>Peut être {@code null} pour les opérations
     * {@link TypeOperation#APPROVISIONNEMENT_AGENT} et
     * {@link TypeOperation#RETRAIT_ADMIN}, dont la source est le
     * {@code montantActif} de la banque et non un compte bancaire
     * individuel.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_emetteur_id", nullable = true)
    @JsonIgnoreProperties({"utilisateur", "banque"})
    private CompteBancaire compteEmetteur;

    /**
     * Compte bancaire bénéficiaire de l'opération (compte crédité).
     *
     * <p>Peut être {@code null} pour l'opération
     * {@link TypeOperation#RETRAIT_ADMIN}, qui ne crédite aucun compte
     * bancaire individuel.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_destinataire_id", nullable = true)
    @JsonIgnoreProperties({"utilisateur", "banque"})
    private CompteBancaire compteDestinataire;

    /**
     * Banque concernée par l'opération.
     *
     * <p>Pour un transfert interbancaire (Skill 12), il s'agit de la
     * banque émettrice, sur le {@code montantActif} de laquelle les frais
     * sont crédités. Pour les autres types d'opérations, il s'agit de la
     * banque unique concernée par l'opération.</p>
     */
    @NotNull(message = "La banque concernée par l'opération est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id", nullable = false)
    @JsonIgnoreProperties({"comptesBancaires", "utilisateurs"})
    private Banque banque;

    /**
     * Description libre et optionnelle apportant un complément
     * d'information sur l'opération (par exemple un motif ou une
     * référence externe).
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Date et heure d'enregistrement de l'opération, renseignée
     * automatiquement par Hibernate lors de la persistance initiale et
     * jamais modifiée par la suite.
     */
    @CreationTimestamp
    @Column(name = "date_operation", nullable = false, updatable = false)
    private LocalDateTime dateOperation;
}
