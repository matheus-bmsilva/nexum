package nexum;

import nexum.exception.ValidationException;
import nexum.service.PrecificacaoService;
import nexum.service.SimplesNacionalService;

/**
 * Suite de testes unitários para a Etapa 2:
 * Validação estrita das 6 faixas do Simples Nacional (Anexo III - Arquitetura e Urbanismo)
 * e cálculo do impacto tributário real na precificação de honorários.
 */
public class SimplesNacionalTest {

    private static final double EPSILON = 0.0001;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" TESTES DA ETAPA 2: SIMPLES NACIONAL (ANEXO III)  ");
        System.out.println("==================================================");

        SimplesNacionalService simplesService = new SimplesNacionalService();

        // 1. Faixa 1: RBT12 até 180.000,00 -> 6,00% (PD: 0)
        SimplesNacionalService.ResultadoSimples f1Zero = simplesService.calcular(0);
        assert f1Zero.faixa() == 1 : "Deveria ser Faixa 1";
        assert Math.abs(f1Zero.aliquotaEfetiva() - 0.06) < EPSILON : "Alíquota efetiva para RBT12 0 deve ser 6%";

        SimplesNacionalService.ResultadoSimples f1 = simplesService.calcular(100000.00);
        assert f1.faixa() == 1 : "Deveria ser Faixa 1";
        assert f1.parcelaADeduzir() == 0.0 : "Parcela a deduzir da Faixa 1 deve ser 0";
        assert Math.abs(f1.aliquotaEfetiva() - 0.06) < EPSILON : "Alíquota efetiva Faixa 1 deve ser 6%";

        SimplesNacionalService.ResultadoSimples f1Limite = simplesService.calcular(180000.00);
        assert f1Limite.faixa() == 1 : "Deveria ser Faixa 1 no limite superior";
        assert Math.abs(f1Limite.aliquotaEfetiva() - 0.06) < EPSILON : "Alíquota efetiva no limite de 180k deve ser 6%";
        System.out.println("[OK] 1ª Faixa validada: Até R$ 180.000,00 -> 6,00% | PD: R$ 0,00");

        // 2. Faixa 2: 180.000,01 a 360.000,00 -> Aliq Nominal: 11,20% | PD: 9.360,00
        // Para RBT12 = 200.000: ((200000 * 0.112) - 9360) / 200000 = (22400 - 9360) / 200000 = 13040 / 200000 = 0.0652 (6,52%)
        SimplesNacionalService.ResultadoSimples f2 = simplesService.calcular(200000.00);
        assert f2.faixa() == 2 : "Deveria ser Faixa 2";
        assert Math.abs(f2.aliquotaNominal() - 0.112) < EPSILON : "Alíquota nominal Faixa 2 deve ser 11,20%";
        assert Math.abs(f2.parcelaADeduzir() - 9360.00) < EPSILON : "Parcela a deduzir Faixa 2 deve ser 9.360,00";
        assert Math.abs(f2.aliquotaEfetiva() - 0.0652) < EPSILON : "Alíquota efetiva calculada deve ser 6,52%: " + f2.aliquotaEfetiva();
        System.out.println("[OK] 2ª Faixa validada: R$ 180.000,01 a R$ 360.000,00 -> 11,20% | PD: R$ 9.360,00 (Efetiva: 6,52%)");

        // 3. Faixa 3: 360.000,01 a 720.000,00 -> Aliq Nominal: 13,50% | PD: 17.640,00
        // Para RBT12 = 500.000: ((500000 * 0.135) - 17640) / 500000 = (67500 - 17640) / 500000 = 49860 / 500000 = 0.09972 (9,97%)
        SimplesNacionalService.ResultadoSimples f3 = simplesService.calcular(500000.00);
        assert f3.faixa() == 3 : "Deveria ser Faixa 3";
        assert Math.abs(f3.aliquotaNominal() - 0.135) < EPSILON : "Alíquota nominal Faixa 3 deve ser 13,50%";
        assert Math.abs(f3.parcelaADeduzir() - 17640.00) < EPSILON : "Parcela a deduzir Faixa 3 deve ser 17.640,00";
        assert Math.abs(f3.aliquotaEfetiva() - 0.09972) < EPSILON : "Alíquota efetiva Faixa 3 incorreta: " + f3.aliquotaEfetiva();
        System.out.println("[OK] 3ª Faixa validada: R$ 360.000,01 a R$ 720.000,00 -> 13,50% | PD: R$ 17.640,00 (Efetiva: 9,97%)");

        // 4. Faixa 4: 720.000,01 a 1.800.000,00 -> Aliq Nominal: 16,00% | PD: 35.640,00
        // Para RBT12 = 1.000.000: ((1000000 * 0.16) - 35640) / 1000000 = 124360 / 1000000 = 0.12436 (12,436%)
        SimplesNacionalService.ResultadoSimples f4 = simplesService.calcular(1000000.00);
        assert f4.faixa() == 4 : "Deveria ser Faixa 4";
        assert Math.abs(f4.aliquotaNominal() - 0.160) < EPSILON : "Alíquota nominal Faixa 4 deve ser 16,00%";
        assert Math.abs(f4.parcelaADeduzir() - 35640.00) < EPSILON : "Parcela a deduzir Faixa 4 deve ser 35.640,00";
        assert Math.abs(f4.aliquotaEfetiva() - 0.12436) < EPSILON : "Alíquota efetiva Faixa 4 incorreta: " + f4.aliquotaEfetiva();
        System.out.println("[OK] 4ª Faixa validada: R$ 720.000,01 a R$ 1.800.000,00 -> 16,00% | PD: R$ 35.640,00 (Efetiva: 12,44%)");

        // 5. Faixa 5: 1.800.000,01 a 3.600.000,00 -> Aliq Nominal: 21,00% | PD: 125.640,00
        // Para RBT12 = 2.000.000: ((2000000 * 0.21) - 125640) / 2000000 = (420000 - 125640) / 2000000 = 294360 / 2000000 = 0.14718
        SimplesNacionalService.ResultadoSimples f5 = simplesService.calcular(2000000.00);
        assert f5.faixa() == 5 : "Deveria ser Faixa 5";
        assert Math.abs(f5.aliquotaNominal() - 0.210) < EPSILON : "Alíquota nominal Faixa 5 deve ser 21,00%";
        assert Math.abs(f5.parcelaADeduzir() - 125640.00) < EPSILON : "Parcela a deduzir Faixa 5 deve ser 125.640,00";
        assert Math.abs(f5.aliquotaEfetiva() - 0.14718) < EPSILON : "Alíquota efetiva Faixa 5 incorreta: " + f5.aliquotaEfetiva();
        System.out.println("[OK] 5ª Faixa validada: R$ 1.800.000,01 a R$ 3.600.000,00 -> 21,00% | PD: R$ 125.640,00 (Efetiva: 14,72%)");

        // 6. Faixa 6: 3.600.000,01 a 4.800.000,00 -> Aliq Nominal: 33,00% | PD: 648.000,00
        // Para RBT12 = 4.000.000: ((4000000 * 0.33) - 648000) / 4000000 = (1320000 - 648000) / 4000000 = 672000 / 4000000 = 0.168
        SimplesNacionalService.ResultadoSimples f6 = simplesService.calcular(4000000.00);
        assert f6.faixa() == 6 : "Deveria ser Faixa 6";
        assert Math.abs(f6.aliquotaNominal() - 0.330) < EPSILON : "Alíquota nominal Faixa 6 deve ser 33,00%";
        assert Math.abs(f6.parcelaADeduzir() - 648000.00) < EPSILON : "Parcela a deduzir Faixa 6 deve ser 648.000,00";
        assert Math.abs(f6.aliquotaEfetiva() - 0.168) < EPSILON : "Alíquota efetiva Faixa 6 incorreta: " + f6.aliquotaEfetiva();
        System.out.println("[OK] 6ª Faixa validada: R$ 3.600.000,01 a R$ 4.800.000,00 -> 33,00% | PD: R$ 648.000,00 (Efetiva: 16,80%)");

        // 7. Validação de Limites e Exceções
        try {
            simplesService.calcular(-100.0);
            assert false : "Deveria lançar ValidationException para RBT12 negativo";
        } catch (ValidationException e) {
            System.out.println("[OK] Bloqueio de faturamento negativo validado.");
        }

        try {
            simplesService.calcular(4800000.01);
            assert false : "Deveria lançar ValidationException para RBT12 acima de R$ 4.800.000,00";
        } catch (ValidationException e) {
            System.out.println("[OK] Bloqueio de faturamento acima do teto de R$ 4.8M validado.");
        }

        // 8. Integração na Precificação (Demonstração do Impacto Tributário Real ao Arquiteto)
        PrecificacaoService precificacao = new PrecificacaoService();

        // Exemplo: Projeto de 100m² a R$ 80,00/m² -> Base = R$ 8.000,00
        // Arquiteto fatura R$ 200.000/ano (2ª Faixa -> Alíquota efetiva = 6,52%)
        ServicoPorMetroQuadrado sM2 = new ServicoPorMetroQuadrado(
            0, "Projeto Residencial Luxo", "2026-11-10", Padrao.ALTO, null, 0.0, 100.0, 80.0
        );

        PrecificacaoService.MemoriaCalculo memoria = precificacao.calcularMemoria(sM2, 200000.00);
        assert memoria.valorBase() == 8000.00 : "Valor base deveria ser R$ 8.000,00";
        assert memoria.faixaSimples() == 2 : "Deveria ser Faixa 2";
        assert Math.abs(memoria.aliquotaImposto() - 0.0652) < EPSILON : "Alíquota efetiva na memória deve ser 0.0652";
        assert Math.abs(memoria.valorImposto() - 521.60) < 0.01 : "Imposto deve ser R$ 521,60: " + memoria.valorImposto();
        assert Math.abs(memoria.valorTotalFinal() - 8521.60) < 0.01 : "Total final deve ser R$ 8.521,60: " + memoria.valorTotalFinal();
        System.out.println("[OK] Integração Precificação: Honorários Líquidos R$ 8.000,00 + Imposto Simples R$ 521,60 = Total R$ 8.521,60.");

        System.out.println("==================================================");
        System.out.println(" SUCESSO: ETAPA 2 100% VALIDADA E CONFORME!       ");
        System.out.println("==================================================");
    }
}
