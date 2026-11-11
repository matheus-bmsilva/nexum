package nexum.service;

import nexum.exception.ValidationException;

/**
 * Serviço de cálculo de tributação do Simples Nacional (Anexo III - Serviços de Arquitetura e Urbanismo).
 * Conforme a Lei Complementar nº 123/2006 e alterações da LC nº 155/2016.
 *
 * Fórmula da Alíquota Efetiva:
 *   Alíquota Efetiva = ((RBT12 * Alíquota Nominal) - Parcela a Deduzir) / RBT12
 */
public class SimplesNacionalService {

    public static final double LIMITE_MAXIMO_RBT12 = 4800000.00;
    public static final double LIMITE_PRIMEIRA_FAIXA = 180000.00;

    public record FaixaSimples(
        int numero,
        double limiteSuperior,
        double aliquotaNominal,
        double parcelaADeduzir,
        String descricao
    ) {}

    private static final FaixaSimples[] FAIXAS = {
        new FaixaSimples(1, 180000.00, 0.060, 0.00, "1ª Faixa: Até R$ 180.000,00"),
        new FaixaSimples(2, 360000.00, 0.112, 9360.00, "2ª Faixa: R$ 180.000,01 a R$ 360.000,00"),
        new FaixaSimples(3, 720000.00, 0.135, 17640.00, "3ª Faixa: R$ 360.000,01 a R$ 720.000,00"),
        new FaixaSimples(4, 1800000.00, 0.160, 35640.00, "4ª Faixa: R$ 720.000,01 a R$ 1.800.000,00"),
        new FaixaSimples(5, 3600000.00, 0.210, 125640.00, "5ª Faixa: R$ 1.800.000,01 a R$ 3.600.000,00"),
        new FaixaSimples(6, 4800000.00, 0.330, 648000.00, "6ª Faixa: R$ 3.600.000,01 a R$ 4.800.000,00")
    };

    public record ResultadoSimples(
        int faixa,
        double rbt12,
        double aliquotaNominal,
        double parcelaADeduzir,
        double aliquotaEfetiva,
        String descricaoFaixa
    ) {}

    /**
     * Calcula a alíquota efetiva do Simples Nacional (Anexo III) com base na Receita Bruta
     * Total dos últimos 12 meses (RBT12).
     *
     * @param rbt12 Receita Bruta Total acumulada nos 12 meses anteriores
     * @return Resultado detalhado com faixa, alíquota nominal, dedução e alíquota efetiva calculada
     */
    public ResultadoSimples calcular(double rbt12) {
        if (rbt12 < 0) {
            throw new ValidationException("O faturamento acumulado RBT12 não pode ser negativo.");
        }
        if (rbt12 > LIMITE_MAXIMO_RBT12) {
            throw new ValidationException("O faturamento RBT12 informado (R$ " + rbt12 +
                ") excede o limite máximo permitido de R$ 4.800.000,00 para o Simples Nacional.");
        }

        // Se RBT12 for zero ou menor ou igual a 180.000,00, aplica-se a alíquota nominal direta da 1ª Faixa (6%)
        if (rbt12 <= LIMITE_PRIMEIRA_FAIXA) {
            FaixaSimples f1 = FAIXAS[0];
            return new ResultadoSimples(
                f1.numero(),
                rbt12,
                f1.aliquotaNominal(),
                f1.parcelaADeduzir(),
                f1.aliquotaNominal(),
                f1.descricao()
            );
        }

        FaixaSimples faixaSelecionada = FAIXAS[FAIXAS.length - 1];
        for (FaixaSimples faixa : FAIXAS) {
            if (rbt12 <= faixa.limiteSuperior()) {
                faixaSelecionada = faixa;
                break;
            }
        }

        // Fórmula oficial da Alíquota Efetiva: ((RBT12 * AliqNominal) - PD) / RBT12
        double aliquotaEfetiva = ((rbt12 * faixaSelecionada.aliquotaNominal()) - faixaSelecionada.parcelaADeduzir()) / rbt12;

        return new ResultadoSimples(
            faixaSelecionada.numero(),
            rbt12,
            faixaSelecionada.aliquotaNominal(),
            faixaSelecionada.parcelaADeduzir(),
            aliquotaEfetiva,
            faixaSelecionada.descricao()
        );
    }
}
