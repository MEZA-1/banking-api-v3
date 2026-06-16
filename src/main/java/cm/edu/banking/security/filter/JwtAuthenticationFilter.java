package cm.edu.banking.security.filter;

import cm.edu.banking.security.CustomUserDetailsService;
import cm.edu.banking.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre de sécurité Spring chargé d'intercepter chaque requête HTTP,
 * d'extraire et de valider le token JWT présent dans l'en-tête
 * {@code Authorization}, puis de peupler le
 * {@link SecurityContextHolder} avec le principal authentifié.
 *
 * <p>Ce filtre étend {@link OncePerRequestFilter}, garantissant qu'il
 * n'est exécuté qu'une seule fois par requête HTTP, même en cas de
 * redirections ou d'inclusions internes.</p>
 *
 * <p>Comportement du filtre :</p>
 * <ol>
 *   <li>Extrait le token depuis l'en-tête {@code Authorization: Bearer <token>}.
 *       Si l'en-tête est absent ou mal formé, la requête est transmise
 *       immédiatement à la suite de la chaîne sans modification du
 *       contexte de sécurité (les endpoints publics seront accessibles,
 *       les endpoints protégés seront rejetés par Spring Security).</li>
 *   <li>Valide le token via {@link JwtTokenProvider#validateToken(String)}.
 *       Un token invalide (expiré, mal signé, etc.) est ignoré silencieusement
 *       (la cause est loguée par {@link JwtTokenProvider}).</li>
 *   <li>Si le token est valide et qu'aucune authentification n'est déjà
 *       présente dans le contexte, charge l'utilisateur via
 *       {@link CustomUserDetailsService} et crée un
 *       {@link UsernamePasswordAuthenticationToken} qui est placé dans
 *       le {@link SecurityContextHolder}.</li>
 * </ol>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Point d'entrée principal du filtre, appelé une seule fois par
     * requête HTTP.
     *
     * @param request     la requête HTTP entrante
     * @param response    la réponse HTTP sortante
     * @param filterChain la chaîne de filtres à laquelle la requête doit
     *                    être transmise après traitement
     * @throws ServletException en cas d'erreur de traitement du filtre
     * @throws IOException      en cas d'erreur d'entrée/sortie
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);

            // Charger l'utilisateur uniquement si le contexte de sécurité
            // est encore vide (évite un rechargement inutile sur les
            // requêtes déjà authentifiées au sein de la même requête).
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Contexte de sécurité établi pour l'utilisateur : {}", email);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrait le token JWT brut depuis l'en-tête HTTP {@code Authorization}.
     *
     * <p>L'en-tête attendu a la forme {@code Bearer <token>}. Si l'en-tête
     * est absent ou ne commence pas par le préfixe {@code Bearer }, la
     * méthode retourne {@code null}.</p>
     *
     * @param request la requête HTTP entrante
     * @return le token JWT brut (sans le préfixe {@code Bearer }), ou
     *         {@code null} si l'en-tête est absent ou mal formé
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
