package cm.edu.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée principal de l'application Spring Boot — Banking API
 * Multi-Banques CEMAC.
 *
 * <p>Cette application expose une API REST sécurisée par JWT permettant
 * la gestion de banques, d'utilisateurs, de comptes bancaires et
 * d'opérations financières (dépôts, retraits, transferts internes et
 * interbancaires) conformément aux règles COBAC/CEMAC.</p>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
public class BankingApplication {

    /**
     * Lance le contexte Spring Boot de l'application.
     *
     * @param args arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
    }
}
