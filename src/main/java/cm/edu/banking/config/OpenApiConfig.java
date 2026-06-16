package cm.edu.banking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de la documentation OpenAPI 3 (Swagger UI) pour
 * l'API bancaire multi-banques CEMAC.
 *
 * <p>Déclare le schéma de sécurité {@code bearerAuth} (JWT Bearer Token)
 * appliqué globalement à tous les endpoints, permettant aux développeurs
 * de tester l'API directement depuis Swagger UI en fournissant leur
 * token JWT via le bouton "Authorize".</p>
 *
 * <p>URL d'accès une fois l'application démarrée :</p>
 * <ul>
 *   <li>Swagger UI : {@code http://localhost:8080/swagger-ui.html}</li>
 *   <li>OpenAPI JSON : {@code http://localhost:8080/v3/api-docs}</li>
 * </ul>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Construit et configure l'instance {@link OpenAPI} utilisée par
     * SpringDoc pour générer la documentation interactive.
     *
     * @return l'instance {@link OpenAPI} configurée avec les métadonnées
     *         du projet et le schéma de sécurité JWT Bearer
     */
    @Bean
    public OpenAPI bankingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking API Multi-Banques CEMAC")
                        .description("""
                                API REST sécurisée de gestion bancaire multi-banques développée
                                dans le cadre du projet académique supervisé par Pr KIMBI XAVERIA.
                                
                                **Rôles disponibles :**
                                - `ADMIN` : gestion globale des banques et des superviseurs
                                - `SUPERVISEUR` : gestion des agents et des clients de sa banque
                                - `AGENT` : dépôts et retraits pour les clients
                                - `CLIENT` : consultation, transferts
                                
                                **Authentification :** JWT Bearer Token.
                                Obtenez votre token via `POST /api/auth/login`,
                                puis cliquez sur **Authorize** et saisissez `Bearer <votre_token>`.
                                """)
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Équipe MEZT-CODING")
                                .email("mezatiogeril@gmail.com"))
                        .license(new License()
                                .name("Projet Académique — cm.edu.banking")
                                .url("https://github.com/MEZA-1/banking-api-v3.git")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("lLle1qYulKMWrpZDYH6F0MfXLIVSnRvRGYGF/zfWbyywtY+0aktwfAyAzu5RU1uaMv1KC6tegOGu8uU8XEqkQg==")));
    }
}
