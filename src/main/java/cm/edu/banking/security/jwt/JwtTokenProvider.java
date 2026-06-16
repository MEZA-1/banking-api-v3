package cm.edu.banking.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Composant responsable de la génération, de la validation et de
 * l'extraction des informations des tokens JWT.
 *
 * <p>Utilise la bibliothèque JJWT 0.12.x avec l'algorithme HMAC-SHA256
 * (HS256). La clé secrète est lue depuis {@link JwtProperties} (propriété
 * {@code app.jwt.secret}) et doit être encodée en Base64 standard.</p>
 *
 * <p>Cycle de vie d'un token :</p>
 * <ol>
 *   <li>L'utilisateur s'authentifie via {@code POST /api/auth/login}.</li>
 *   <li>{@link #generateToken(Authentication)} produit un token signé
 *       contenant le sujet (email) et la date d'expiration.</li>
 *   <li>À chaque requête protégée, le filtre
 *       {@link cm.edu.banking.security.filter.JwtAuthenticationFilter}
 *       extrait le token de l'en-tête {@code Authorization: Bearer <token>},
 *       appelle {@link #validateToken(String)} puis
 *       {@link #getEmailFromToken(String)} pour établir le contexte de
 *       sécurité.</li>
 * </ol>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    /**
     * Construit la clé cryptographique HMAC-SHA256 à partir de la valeur
     * Base64 définie dans {@link JwtProperties#getSecret()}.
     *
     * <p>Cette méthode est appelée à chaque opération JWT plutôt que d'être
     * mise en cache dans un champ, afin de supporter un rechargement
     * éventuel des propriétés en cas de rotation de clé sans redémarrage
     * (bien que cela ne soit pas encore implémenté).</p>
     *
     * @return la {@link SecretKey} dérivée pour l'algorithme HS256
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Génère un token JWT signé pour l'utilisateur authentifié.
     *
     * <p>Le sujet ({@code sub}) du token est l'adresse e-mail de
     * l'utilisateur, qui sert d'identifiant unique de connexion sur la
     * plateforme. La date d'émission ({@code iat}) et la date d'expiration
     * ({@code exp}) sont définies en fonction de
     * {@link JwtProperties#getExpiration()}.</p>
     *
     * @param authentication le contexte d'authentification Spring Security
     *                       résultant d'une connexion réussie
     * @return le token JWT sous forme de chaîne compacte
     *         ({@code header.payload.signature})
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Génère un token JWT signé directement à partir de l'adresse e-mail.
     *
     * <p>Surcharge de {@link #generateToken(Authentication)} utilisée
     * lors de l'inscription d'un nouveau client, pour renvoyer
     * immédiatement un token sans passer par un second appel
     * d'authentification.</p>
     *
     * @param email l'adresse e-mail de l'utilisateur nouvellement inscrit
     * @return le token JWT sous forme de chaîne compacte
     */
    public String generateTokenFromEmail(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrait l'adresse e-mail (sujet) contenue dans un token JWT.
     *
     * <p>Cette méthode suppose que le token a préalablement été validé
     * par {@link #validateToken(String)}.</p>
     *
     * @param token le token JWT compact à décoder
     * @return l'adresse e-mail encodée dans le sujet ({@code sub}) du token
     */
    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Valide un token JWT en vérifiant sa signature et sa date d'expiration.
     *
     * <p>Les erreurs possibles sont loguées au niveau {@code WARN} pour
     * faciliter le diagnostic en production sans exposer d'informations
     * sensibles dans les réponses HTTP.</p>
     *
     * @param token le token JWT compact à valider
     * @return {@code true} si le token est valide (signature correcte et
     *         non expiré), {@code false} dans tous les autres cas
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("Token JWT expiré : {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Token JWT non supporté : {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Token JWT malformé : {}", ex.getMessage());
        } catch (SecurityException ex) {
            log.warn("Signature JWT invalide : {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Token JWT vide ou nul : {}", ex.getMessage());
        }
        return false;
    }
}
