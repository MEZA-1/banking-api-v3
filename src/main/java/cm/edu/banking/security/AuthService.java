package cm.edu.banking.security;

import cm.edu.banking.dto.request.LoginRequest;
import cm.edu.banking.dto.request.RegisterRequest;
import cm.edu.banking.dto.response.AuthResponse;
import cm.edu.banking.exception.DuplicateResourceException;
import cm.edu.banking.model.User;
import cm.edu.banking.model.enums.Role;
import cm.edu.banking.repository.UserRepository;
import cm.edu.banking.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service applicatif gérant l'authentification et l'inscription des
 * utilisateurs ainsi que l'initialisation automatique du
 * Super Admin au premier démarrage.
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li><strong>Inscription</strong> ({@link #register}) : valide
 *       l'unicité de l'email et du téléphone, hash le mot de passe en
 *       BCrypt, persiste l'utilisateur avec le rôle
 *       {@link Role#CLIENT} par défaut, et retourne immédiatement un
 *       token JWT sans nécessiter un second appel à {@code /login}.</li>
 *   <li><strong>Connexion</strong> ({@link #login}) : délègue
 *       l'authentification à Spring Security via
 *       {@link AuthenticationManager}, puis génère et retourne un token
 *       JWT signé.</li>
 *   <li><strong>Initialisation du Super Admin</strong>
 *       ({@link #initializeSuperAdmin}) : s'exécute au démarrage de
 *       l'application et crée un administrateur initial si aucun
 *       utilisateur avec le rôle {@link Role#ADMIN} n'existe encore en
 *       base de données (Skill 2.1).</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /** Email du Super Admin lu depuis {@code application.properties}. */
    @Value("${app.superadmin.email}")
    private String superAdminEmail;

    /** Mot de passe (en clair) du Super Admin, hashé en BCrypt avant persistance. */
    @Value("${app.superadmin.password}")
    private String superAdminPassword;

    // =========================================================================
    //  Initialisation automatique du Super Admin (Skill 2.1)
    // =========================================================================

    /**
     * Vérifie au démarrage de l'application si un administrateur existe.
     *
     * <p>Si aucun utilisateur portant le rôle {@link Role#ADMIN} n'est
     * présent en base de données, un Super Admin est automatiquement créé
     * avec les informations configurées dans {@code application.properties}
     * (ou les variables d'environnement correspondantes).</p>
     *
     * <p>Cette méthode est annotée {@link EventListener} sur
     * {@link ApplicationReadyEvent} : elle s'exécute une fois que le
     * contexte Spring est entièrement initialisé et que la base de données
     * est disponible, garantissant que toutes les dépendances (repository,
     * encodeur, etc.) sont prêtes.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeSuperAdmin() {
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Un administrateur existe déjà — aucune initialisation nécessaire.");
            return;
        }

        User superAdmin = User.builder()
                .nom("Super")
                .prenom("Admin")
                .email(superAdminEmail)
                .telephone("000000000")
                .motDePasse(passwordEncoder.encode(superAdminPassword))
                .role(Role.ADMIN)
                .build();

        userRepository.save(superAdmin);
        log.info("Super Admin initialisé avec succès : {}", superAdminEmail);
    }

    // =========================================================================
    //  Inscription (Skill 2.2)
    // =========================================================================

    /**
     * Inscrit un nouvel utilisateur sur la plateforme avec le rôle
     * {@link Role#CLIENT} par défaut.
     *
     * <p>Étapes du processus :</p>
     * <ol>
     *   <li>Vérifie que l'email n'est pas déjà utilisé.</li>
     *   <li>Vérifie que le téléphone n'est pas déjà utilisé.</li>
     *   <li>Hash le mot de passe avec BCrypt.</li>
     *   <li>Persiste l'utilisateur en base de données.</li>
     *   <li>Génère et retourne un token JWT valide.</li>
     * </ol>
     *
     * @param request les données d'inscription fournies par l'appelant
     * @return un {@link AuthResponse} contenant le token JWT et les
     *         informations de l'utilisateur créé
     * @throws DuplicateResourceException si l'email ou le téléphone est déjà
     *                                    utilisé par un autre compte
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Vérification d'unicité de l'email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Utilisateur", "email", request.getEmail());
        }

        // Vérification d'unicité du téléphone
        if (userRepository.existsByTelephone(request.getTelephone())) {
            throw new DuplicateResourceException("Utilisateur", "téléphone", request.getTelephone());
        }

        // Création et persistance de l'utilisateur
        User user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .motDePasse(passwordEncoder.encode(request.getMotDePasse()))
                .role(Role.CLIENT)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Nouvel utilisateur inscrit : {} (id={})", savedUser.getEmail(), savedUser.getId());

        // Génération du token JWT sans second appel d'authentification
        String token = jwtTokenProvider.generateTokenFromEmail(savedUser.getEmail());

        return buildAuthResponse(savedUser, token);
    }

    // =========================================================================
    //  Connexion (Skill 2.2)
    // =========================================================================

    /**
     * Authentifie un utilisateur existant et retourne un token JWT.
     *
     * <p>La validation des identifiants est entièrement déléguée à Spring
     * Security via {@link AuthenticationManager#authenticate}. En cas
     * d'identifiants incorrects, Spring Security lève une
     * {@link org.springframework.security.authentication.BadCredentialsException}
     * interceptée par le
     * {@link cm.edu.banking.exception.GlobalExceptionHandler}.</p>
     *
     * @param request les identifiants de connexion (email + mot de passe)
     * @return un {@link AuthResponse} contenant le token JWT et les
     *         informations de l'utilisateur authentifié
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Délégation à Spring Security (vérifie email + BCrypt password)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getMotDePasse()
                )
        );

        // Chargement de l'entité utilisateur pour construire la réponse
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException(
                        "Incohérence : utilisateur authentifié mais introuvable en base"));

        String token = jwtTokenProvider.generateToken(authentication);
        log.info("Connexion réussie pour : {}", user.getEmail());

        return buildAuthResponse(user, token);
    }

    // =========================================================================
    //  Méthode utilitaire
    // =========================================================================

    /**
     * Construit un {@link AuthResponse} à partir d'un utilisateur et d'un
     * token JWT.
     *
     * @param user  l'utilisateur authentifié ou nouvellement inscrit
     * @param token le token JWT généré pour cet utilisateur
     * @return le DTO de réponse d'authentification
     */
    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .nomComplet(user.getPrenom() + " " + user.getNom())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
