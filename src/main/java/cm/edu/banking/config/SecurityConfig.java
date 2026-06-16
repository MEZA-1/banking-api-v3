package cm.edu.banking.config;

import cm.edu.banking.security.CustomUserDetailsService;
import cm.edu.banking.security.JwtAuthenticationEntryPoint;
import cm.edu.banking.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration centrale de Spring Security 6.x pour l'API bancaire.
 *
 * <p>Principes appliqués :</p>
 * <ul>
 *   <li><strong>Stateless</strong> : aucune session HTTP n'est créée ni
 *       utilisée ({@link SessionCreationPolicy#STATELESS}) ; l'état
 *       d'authentification est entièrement porté par le token JWT.</li>
 *   <li><strong>CSRF désactivé</strong> : inutile pour une API REST
 *       stateless consommée par des clients non-navigateur.</li>
 *   <li><strong>Règles d'autorisation par URL</strong> : les endpoints
 *       d'authentification ({@code /api/auth/**}) et de documentation
 *       OpenAPI sont publics ; toutes les autres routes requièrent une
 *       authentification.</li>
 *   <li><strong>Sécurité au niveau méthode</strong> :
 *       {@link EnableMethodSecurity} active les annotations
 *       {@code @PreAuthorize} et {@code @PostAuthorize}, utilisées dans
 *       les contrôleurs pour affiner les droits par rôle.</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // =========================================================================
    //  Beans de sécurité
    // =========================================================================

    /**
     * Encodeur de mots de passe BCrypt avec le facteur de coût par défaut
     * ({@code strength = 10}).
     *
     * <p>Utilisé lors de l'inscription, de la création du Super Admin et
     * lors de l'authentification pour comparer le mot de passe fourni avec
     * le hash stocké en base de données.</p>
     *
     * @return une instance de {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Fournisseur d'authentification DAO, configuré avec le
     * {@link CustomUserDetailsService} et l'encodeur BCrypt.
     *
     * <p>C'est ce composant qui est invoqué par Spring Security lorsque
     * l'utilisateur soumet ses identifiants (email / mot de passe) à
     * l'endpoint de connexion.</p>
     *
     * @return une instance de {@link DaoAuthenticationProvider} configurée
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expose l'{@link AuthenticationManager} en tant que bean Spring,
     * permettant son injection dans le service d'authentification
     * ({@link cm.edu.banking.security.AuthService}) pour déclencher
     * programmatiquement l'authentification lors du login.
     *
     * @param config la configuration d'authentification fournie par Spring
     * @return l'{@link AuthenticationManager} de l'application
     * @throws Exception si la résolution de l'AuthenticationManager échoue
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // =========================================================================
    //  Chaîne de filtres de sécurité
    // =========================================================================

    /**
     * Définit la chaîne de filtres de sécurité principale de l'application.
     *
     * <p>Règles d'autorisation par URL (ordre important — Spring Security
     * évalue la première règle correspondante) :</p>
     * <ul>
     *   <li>{@code POST /api/auth/**} : public (inscription, connexion).</li>
     *   <li>{@code GET /v3/api-docs/**}, {@code /swagger-ui/**},
     *       {@code /swagger-ui.html} : public (documentation OpenAPI).</li>
     *   <li>Toute autre requête : authentification requise.</li>
     * </ul>
     *
     * @param http le constructeur de sécurité HTTP Spring
     * @return la {@link SecurityFilterChain} configurée
     * @throws Exception si la configuration de la chaîne de filtres échoue
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Désactiver CSRF (API REST stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // Gestion des erreurs d'authentification → réponse JSON 401
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // Pas de session HTTP (mode stateless)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Règles d'autorisation par URL
                .authorizeHttpRequests(auth -> auth
                        // --- Endpoints publics ---
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()

                        // --- Documentation OpenAPI ---
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // --- Toutes les autres routes : authentification obligatoire ---
                        // Les restrictions par rôle sont gérées via @PreAuthorize
                        // au niveau des contrôleurs .
                        .anyRequest().authenticated()
                )

                // Fournisseur d'authentification DAO + BCrypt
                .authenticationProvider(authenticationProvider())

                // Placer le filtre JWT avant le filtre d'authentification par
                // username/password natif de Spring Security
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
