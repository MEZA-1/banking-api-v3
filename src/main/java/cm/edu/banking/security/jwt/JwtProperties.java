package cm.edu.banking.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Conteneur typé des propriétés de configuration JWT de l'application,
 * lues depuis {@code application.properties} sous le préfixe {@code app.jwt}.
 *
 * <p>Propriétés disponibles :</p>
 * <ul>
 *   <li>{@code app.jwt.secret} : clé secrète HMAC-SHA256 encodée en
 *       Base64 (minimum 256 bits / 32 octets). En production, cette
 *       valeur doit impérativement être injectée via une variable
 *       d'environnement {@code JWT_SECRET}.</li>
 *   <li>{@code app.jwt.expiration} : durée de validité du token JWT en
 *       millisecondes. Valeur par défaut : 86 400 000 ms (24 heures).
 *       Peut être surchargée via la variable d'environnement
 *       {@code JWT_EXPIRATION}.</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Clé secrète HMAC-SHA256 encodée en Base64 utilisée pour signer et
     * vérifier les tokens JWT. Doit contenir au minimum 256 bits (32 octets
     * décodés) pour satisfaire l'exigence de l'algorithme HS256.
     */
    private String secret;

    /**
     * Durée de validité d'un token JWT en millisecondes.
     * Valeur par défaut : 86 400 000 ms (24 heures).
     */
    private long expiration = 86_400_000L;
}
