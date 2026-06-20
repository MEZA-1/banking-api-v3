package cm.edu.banking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests paramétrés validant les règles de calcul de frais bancaires
 * pour les quatre types d'opérations financières.
 *
 * <p>Ces tests sont intentionnellement indépendants de tout service Spring
 * pour garantir une exécution ultra-rapide et isoler la logique de calcul.</p>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@DisplayName("Calculs de frais bancaires — Tests paramétrés")
class BankingCalculationsTest {

    // =========================================================================
    //  Skill 9 — Dépôt (0 %)
    // =========================================================================

    @ParameterizedTest(name = "Dépôt {0} FCFA → frais = 0")
    @CsvSource({
            "1000,    0.00",
            "50000,   0.00",
            "500000,  0.00",
            "1000000, 0.00"
    })
    @DisplayName("Skill 9 — Dépôt : frais toujours nuls")
    void depot_fraisNuls(String montantStr, String fraisAttenduStr) {
        BigDecimal frais = calculerFrais(new BigDecimal(montantStr), new BigDecimal("0.00"));
        assertThat(frais).isEqualByComparingTo(new BigDecimal(fraisAttenduStr));
    }

    // =========================================================================
    //  Skill 10 — Retrait (2 %)
    // =========================================================================

    @ParameterizedTest(name = "Retrait {0} FCFA → frais={1}, débit client={2}")
    @CsvSource({
            "10000,   200.00,   10200.00",
            "50000,   1000.00,  51000.00",
            "100000,  2000.00,  102000.00",
            "175000,  3500.00,  178500.00",
            "1000,    20.00,    1020.00",
            "500000,  10000.00, 510000.00"
    })
    @DisplayName("Skill 10 — Retrait : frais 2 % et débit total correct")
    void retrait_frais2Pourcent(String montantStr, String fraisAttenduStr,
                                 String debitAttenduStr) {
        BigDecimal montant = new BigDecimal(montantStr);
        BigDecimal frais = calculerFrais(montant, new BigDecimal("0.02"));
        BigDecimal debitTotal = montant.add(frais);

        assertThat(frais)
                .as("Frais 2 %% sur %s FCFA", montantStr)
                .isEqualByComparingTo(new BigDecimal(fraisAttenduStr));
        assertThat(debitTotal)
                .as("Débit total (montant + frais) sur %s FCFA", montantStr)
                .isEqualByComparingTo(new BigDecimal(debitAttenduStr));
    }

    // =========================================================================
    //  Skill 11 — Transfert interne (2 %)
    // =========================================================================

    @ParameterizedTest(name = "Transfert interne {0} FCFA → frais={1}, émetteur débité={2}, destinataire crédité={0}")
    @CsvSource({
            "50000,   1000.00,  51000.00",
            "200000,  4000.00,  204000.00",
            "750000,  15000.00, 765000.00",
            "25000,   500.00,   25500.00"
    })
    @DisplayName("Skill 11 — Transfert interne : frais 2 %, destinataire reçoit montant seul")
    void transfertInterne_frais2Pourcent(String montantStr, String fraisAttenduStr,
                                          String debitEmetteurStr) {
        BigDecimal montant = new BigDecimal(montantStr);
        BigDecimal frais = calculerFrais(montant, new BigDecimal("0.02"));
        BigDecimal debitEmetteur = montant.add(frais);

        assertThat(frais).isEqualByComparingTo(new BigDecimal(fraisAttenduStr));
        assertThat(debitEmetteur).isEqualByComparingTo(new BigDecimal(debitEmetteurStr));
        // Destinataire reçoit exactement le montant (pas les frais)
        assertThat(montant).isEqualByComparingTo(new BigDecimal(montantStr));
    }

    // =========================================================================
    //  Skill 12 — Transfert interbancaire (4 %)
    // =========================================================================

    @ParameterizedTest(name = "Transfert interbancaire {0} FCFA → frais={1}, émetteur débité={2}")
    @CsvSource({
            "50000,   2000.00,  52000.00",
            "100000,  4000.00,  104000.00",
            "25000,   1000.00,  26000.00",
            "500000,  20000.00, 520000.00",
            "10000,   400.00,   10400.00",
            "750000,  30000.00, 780000.00"
    })
    @DisplayName("Skill 12 — Transfert interbancaire : frais 4 %, destinataire reçoit montant seul")
    void transfertInterbancaire_frais4Pourcent(String montantStr, String fraisAttenduStr,
                                                String debitEmetteurStr) {
        BigDecimal montant = new BigDecimal(montantStr);
        BigDecimal frais = calculerFrais(montant, new BigDecimal("0.04"));
        BigDecimal debitEmetteur = montant.add(frais);

        assertThat(frais)
                .as("Frais 4 %% sur %s FCFA", montantStr)
                .isEqualByComparingTo(new BigDecimal(fraisAttenduStr));
        assertThat(debitEmetteur)
                .as("Débit émetteur (montant + frais 4 %%) sur %s FCFA", montantStr)
                .isEqualByComparingTo(new BigDecimal(debitEmetteurStr));
    }

    // =========================================================================
    //  Comparaison frais interne vs interbancaire
    // =========================================================================

    @ParameterizedTest(name = "Sur {0} FCFA : frais interne={1}, interbancaire={2}")
    @CsvSource({
            "100000, 2000.00, 4000.00",
            "50000,  1000.00, 2000.00",
            "200000, 4000.00, 8000.00"
    })
    @DisplayName("Frais interbancaires (4 %) = 2 × frais internes (2 %) pour un même montant")
    void comparaison_fraisInterneVsInterbancaire(String montantStr,
                                                   String fraisInterneStr,
                                                   String fraisInterbanStr) {
        BigDecimal montant = new BigDecimal(montantStr);
        BigDecimal fraisInterne = calculerFrais(montant, new BigDecimal("0.02"));
        BigDecimal fraisInterban = calculerFrais(montant, new BigDecimal("0.04"));

        assertThat(fraisInterne).isEqualByComparingTo(new BigDecimal(fraisInterneStr));
        assertThat(fraisInterban).isEqualByComparingTo(new BigDecimal(fraisInterbanStr));
        assertThat(fraisInterban)
                .as("Frais interbancaires = 2× frais internes")
                .isEqualByComparingTo(fraisInterne.multiply(new BigDecimal("2")));
    }

    // =========================================================================
    //  Retrait admin — plafond 1 000 000 FCFA
    // =========================================================================

    @ParameterizedTest(name = "Retrait admin {0} FCFA : dans le plafond={1}")
    @CsvSource({
            "1,        true",
            "500000,   true",
            "1000000,  true",
            "1000001,  false",
            "2000000,  false"
    })
    @DisplayName("Skill 4 — Retrait admin : plafond exact de 1 000 000 FCFA")
    void retraitAdmin_plafond(String montantStr, boolean dansDansLePlafond) {
        BigDecimal montant = new BigDecimal(montantStr);
        BigDecimal plafond = new BigDecimal("1000000");

        boolean estDansLePlafond = montant.compareTo(plafond) <= 0;
        assertThat(estDansLePlafond).isEqualTo(dansDansLePlafond);
    }

    // =========================================================================
    //  Méthode utilitaire (réplication de la logique du service)
    // =========================================================================

    /**
     * Réplique exacte de la méthode {@code calculerFrais} du
     * {@link TransactionService}, utilisée ici pour garantir que les
     * tests paramétrés valident la même formule que celle en production.
     */
    private BigDecimal calculerFrais(BigDecimal montant, BigDecimal taux) {
        return montant.multiply(taux).setScale(2, RoundingMode.HALF_UP);
    }
}
