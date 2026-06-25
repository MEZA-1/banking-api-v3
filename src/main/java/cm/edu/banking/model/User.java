package cm.edu.banking.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cm.edu.banking.model.enums.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entité JPA représentant un utilisateur de la plateforme bancaire
 * multi-banques.
 *
 * <p>Un utilisateur possède toujours un et un seul {@link Role} parmi
 * {@link Role#ADMIN}, {@link Role#SUPERVISEUR}, {@link Role#AGENT} et
 * {@link Role#CLIENT} (Skill 1).</p>
 *
 * <p><strong>Important :</strong> conformément à la Skill 8, le champ
 * {@code solde} a été entièrement retiré de cette entité. Toute opération
 * financière repose désormais exclusivement sur l'entité
 * {@link CompteBancaire}, à laquelle un utilisateur est relié par une
 * relation one-to-one.</p>
 *
 * <p>Rattachement à une banque :</p>
 * <ul>
 *   <li>{@link Role#ADMIN} : non rattaché à une banque ({@code banque == null}),
 *       l'administrateur opère au niveau global de la plateforme ;</li>
 *   <li>{@link Role#SUPERVISEUR} et {@link Role#AGENT} : rattachés
 *       directement à une banque via le champ {@code banque} ;</li>
 *   <li>{@link Role#CLIENT} : non rattaché directement à une banque ; sa
 *       banque est déterminée indirectement via son {@link CompteBancaire}.</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_user_telephone", columnNames = "telephone")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"motDePasse", "banque", "compteBancaire"})
public class User {

    /**
     * Identifiant technique unique de l'utilisateur, généré automatiquement
     * par la base de données.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Nom de famille de l'utilisateur.
     */
    @NotBlank(message = "Le nom est obligatoire")
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    /**
     * Prénom de l'utilisateur.
     */
    @NotBlank(message = "Le prénom est obligatoire")
    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    /**
     * Adresse e-mail de l'utilisateur. Utilisée comme identifiant de
     * connexion (login) et doit être unique sur l'ensemble de la
     * plateforme (Skill 2.2).
     */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Le format de l'email est invalide")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Numéro de téléphone de l'utilisateur. Doit être unique sur l'ensemble
     * de la plateforme (Skill 2.2).
     */
    @NotBlank(message = "Le téléphone est obligatoire")
    @Column(name = "telephone", nullable = false, unique = true, length = 30)
    private String telephone;

    /**
     * Mot de passe de l'utilisateur, stocké sous forme de hash BCrypt.
     *
     * <p>Ce champ est exclu de la sérialisation JSON via
     * {@link JsonIgnore} afin de ne jamais exposer le hash, même de
     * manière involontaire, dans les réponses de l'API.</p>
     */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @JsonIgnore
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    /**
     * Rôle applicatif de l'utilisateur. Déterminé automatiquement à
     * {@link Role#CLIENT} lors d'une inscription via l'API publique
     * (Skill 2.2), et attribué explicitement par un administrateur ou un
     * superviseur pour les autres rôles (Skills 4 et 5).
     */
    @NotNull(message = "Le rôle est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /**
     * Banque de rattachement de l'utilisateur.
     *
     * <p>Renseigné uniquement pour les utilisateurs de rôle
     * {@link Role#SUPERVISEUR} ou {@link Role#AGENT}. Reste {@code null}
     * pour un {@link Role#ADMIN} (portée globale) et pour un
     * {@link Role#CLIENT} (dont la banque est déduite de son
     * {@link CompteBancaire}).</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id", nullable = true)
    private Banque banque;

    /**
     * Compte bancaire associé à cet utilisateur.
     *
     * <p>Relation one-to-one bidirectionnelle dont le côté propriétaire
     * est {@link CompteBancaire} (via le champ {@code utilisateur}).
     * Conformément à la Skill 8, chaque utilisateur opérationnel
     * (client ou agent) possède au plus un compte bancaire, créé via une
     * API dédiée et dont le solde initial est obligatoirement nul.</p>
     */
    @OneToOne(mappedBy = "utilisateur", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    private CompteBancaire compteBancaire;

    /**
     * Date et heure de création du compte utilisateur, renseignée
     * automatiquement par Hibernate lors de la persistance initiale.
     */
    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /**
     * Date et heure de la dernière modification du compte utilisateur,
     * mise à jour automatiquement par Hibernate à chaque persistance.
     */
    @UpdateTimestamp
    @Column(name = "date_mise_a_jour")
    private LocalDateTime dateMiseAJour;
}
