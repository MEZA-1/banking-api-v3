package cm.edu.banking.model;

import cm.edu.banking.model.enums.StatutCompte;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant le compte bancaire d'un utilisateur (Skill 8).
 *
 * <p>Conformément aux exigences de l'étape 1, le champ {@code solde} a été
 * retiré de l'entité {@link User} : <strong>toutes les opérations
 * financières (dépôts, retraits, transferts internes et
 * interbancaires) reposent exclusivement sur {@code CompteBancaire}</strong>.</p>
 *
 * <p>Règles métier associées :</p>
 * <ul>
 *   <li>chaque utilisateur (client ou agent) possède au plus un compte
 *       bancaire, créé via une API dédiée ;</li>
 *   <li>le solde initial d'un compte nouvellement créé est obligatoirement
 *       égal à zéro ;</li>
 *   <li>le champ {@code solde} ne doit jamais être fourni ni accepté dans
 *       la requête de création d'un compte (cette contrainte est appliquée
 *       au niveau des DTOs/contrôleurs, l'entité elle-même garantissant
 *       uniquement la valeur par défaut) ;</li>
 *   <li>un compte au statut {@link StatutCompte#SUSPENDU} ne peut ni émettre
 *       ni recevoir aucune opération financière (Skill 15).</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Entity
@Table(
        name = "comptes_bancaires",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_compte_numero", columnNames = "numero_compte"),
                @UniqueConstraint(name = "uk_compte_utilisateur", columnNames = "utilisateur_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"utilisateur", "banque"})
public class CompteBancaire {

    /**
     * Identifiant technique unique du compte bancaire, généré
     * automatiquement par la base de données.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Numéro de compte bancaire, unique sur l'ensemble de la plateforme.
     *
     * <p>Ce numéro est généré par la couche service lors de la création du
     * compte (par exemple selon un format normalisé incluant un code
     * banque) et ne peut jamais être modifié après création.</p>
     */
    @NotBlank(message = "Le numéro de compte est obligatoire")
    @Column(name = "numero_compte", nullable = false, unique = true, length = 34, updatable = false)
    private String numeroCompte;

    /**
     * Solde courant du compte, exprimé en FCFA.
     *
     * <p>Initialisé obligatoirement à zéro lors de la création du compte
     * (Skill 8). Toute modification ultérieure de ce champ doit résulter
     * exclusivement d'une opération financière tracée dans
     * {@link Operation} (dépôt, retrait, transfert interne,
     * transfert interbancaire).</p>
     */
    @NotNull(message = "Le solde est obligatoire")
    @Column(name = "solde", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal solde = BigDecimal.ZERO;

    /**
     * Statut opérationnel courant du compte bancaire.
     *
     * <p>Valeur par défaut à la création : {@link StatutCompte#ACTIF}. Un
     * superviseur peut faire passer ce statut à
     * {@link StatutCompte#SUSPENDU} pour un agent ou un client de sa
     * banque (Skill 5), bloquant ainsi toute opération financière
     * impliquant ce compte (Skill 15).</p>
     */
    @NotNull(message = "Le statut du compte est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private StatutCompte statut = StatutCompte.ACTIF;

    /**
     * Utilisateur propriétaire de ce compte bancaire.
     *
     * <p>Relation one-to-one dont {@code CompteBancaire} est le côté
     * propriétaire (porteur de la clé étrangère {@code utilisateur_id}).
     * La propriété {@link JsonIgnoreProperties} permet d'éviter les
     * boucles de sérialisation infinies entre {@link User} et
     * {@code CompteBancaire} tout en conservant l'accès aux informations
     * utiles du propriétaire du compte.</p>
     */
    @NotNull(message = "Le compte doit être associé à un utilisateur")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"compteBancaire", "banque", "motDePasse"})
    private User utilisateur;

    /**
     * Banque à laquelle ce compte bancaire est rattaché.
     *
     * <p>Cette information est essentielle pour déterminer si une opération
     * de transfert constitue un transfert interne (Skill 11, même banque)
     * ou un transfert interbancaire (Skill 12, banques différentes), et
     * pour mettre à jour le {@code montantLigne} de la banque concernée
     * (Skill 13).</p>
     */
    @NotNull(message = "Le compte doit être associé à une banque")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id", nullable = false)
    @JsonIgnoreProperties({"comptesBancaires", "utilisateurs"})
    private Banque banque;

    /**
     * Date et heure de création du compte bancaire, renseignée
     * automatiquement par Hibernate lors de la persistance initiale.
     */
    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
