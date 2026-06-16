package cm.edu.banking.model;

import cm.edu.banking.model.enums.StatutBanque;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité JPA représentant une banque membre de la plateforme multi-banques
 * (Skill 3 : Gestion des banques).
 *
 * <p>Une banque possède une trésorerie propre suivie au travers de deux
 * indicateurs financiers :</p>
 * <ul>
 *   <li>{@code montantActif} : trésorerie disponible de la banque, initialisée
 *       à {@code montantInitial} lors de la création, et mise à jour au fil
 *       des opérations (frais perçus, approvisionnements d'agents, retraits
 *       administrateur, etc.) ;</li>
 *   <li>{@code montantLigne} : indicateur représentant la somme de tous les
 *       soldes des comptes clients et agents rattachés à la banque. Ce champ
 *       est recalculé automatiquement après chaque opération financière
 *       impactant un compte de la banque (Skill 13).</li>
 * </ul>
 *
 * <p>Le statut {@link StatutBanque#SUSPENDUE} d'une banque bloque toutes les
 * opérations financières des utilisateurs (agents et clients) qui lui sont
 * rattachés (Skill 15).</p>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Entity
@Table(
        name = "banques",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_banque_nom", columnNames = "nom")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"comptesBancaires", "utilisateurs"})
public class Banque {

    /**
     * Identifiant technique unique de la banque, généré automatiquement
     * par la base de données.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Nom commercial unique de la banque. Utilisé notamment pour
     * différencier les transferts internes des transferts interbancaires
     * (Skills 11 et 12).
     */
    @NotBlank(message = "Le nom de la banque est obligatoire")
    @Column(name = "nom", nullable = false, unique = true, length = 150)
    private String nom;

    /**
     * Statut opérationnel courant de la banque.
     *
     * <p>Valeur par défaut à la création : {@link StatutBanque#ACTIVE}.</p>
     */
    @NotNull(message = "Le statut de la banque est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private StatutBanque statut = StatutBanque.ACTIVE;

    /**
     * Montant de capital initial injecté par la banque lors de sa création.
     *
     * <p>Règle métier : ce montant doit être supérieur ou égal à
     * 2 000 000 FCFA (Skill 3). Cette contrainte est validée au niveau de
     * la couche service.</p>
     */
    @NotNull(message = "Le montant initial est obligatoire")
    @DecimalMin(value = "2000000.00", message = "Le montant initial doit être supérieur ou égal à 2 000 000 FCFA")
    @Column(name = "montant_initial", nullable = false, precision = 19, scale = 2)
    private BigDecimal montantInitial;

    /**
     * Trésorerie active actuellement disponible pour la banque.
     *
     * <p>Initialisée à la valeur de {@code montantInitial} lors de la
     * création de la banque. Elle évolue ensuite avec :</p>
     * <ul>
     *   <li>les frais perçus sur les retraits et transferts (Skills 10, 11, 12) ;</li>
     *   <li>les approvisionnements d'agents, qui la diminuent (Skill 5) ;</li>
     *   <li>les retraits administrateur, qui la diminuent, plafonnés à
     *       1 000 000 FCFA par opération (Skill 4).</li>
     * </ul>
     */
    @NotNull(message = "Le montant actif est obligatoire")
    @Column(name = "montant_actif", nullable = false, precision = 19, scale = 2)
    private BigDecimal montantActif;

    /**
     * Indicateur représentant la somme des soldes de tous les comptes
     * (clients et agents) rattachés à cette banque.
     *
     * <p>Initialisé à zéro lors de la création de la banque, puis recalculé
     * automatiquement après chaque opération impactant un solde de compte
     * (dépôt, retrait, transfert interne, transfert interbancaire,
     * approvisionnement d'agent) conformément à la Skill 13.</p>
     */
    @NotNull(message = "Le montant ligne est obligatoire")
    @Column(name = "montant_ligne", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal montantLigne = BigDecimal.ZERO;

    /**
     * Date et heure de création de la banque, renseignée automatiquement
     * par Hibernate lors de la persistance initiale et jamais modifiée
     * par la suite.
     */
    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /**
     * Liste des comptes bancaires (clients et agents) rattachés à cette
     * banque.
     *
     * <p>Relation bidirectionnelle gérée côté {@link CompteBancaire} via le
     * champ {@code banque}. Marquée {@link JsonIgnore} afin d'éviter les
     * boucles de sérialisation JSON et l'exposition massive de données
     * sensibles dans les réponses relatives à une banque.</p>
     */
    @JsonIgnore
    @OneToMany(mappedBy = "banque", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<CompteBancaire> comptesBancaires = new ArrayList<>();

    /**
     * Liste des utilisateurs (superviseurs et agents) directement
     * rattachés à cette banque.
     *
     * <p>Relation bidirectionnelle gérée côté {@link User} via le champ
     * {@code banque}. Marquée {@link JsonIgnore} afin d'éviter les boucles
     * de sérialisation JSON.</p>
     */
    @JsonIgnore
    @OneToMany(mappedBy = "banque", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<User> utilisateurs = new ArrayList<>();
}
