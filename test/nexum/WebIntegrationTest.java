package nexum;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import nexum.database.ConnectionFactory;
import nexum.database.DatabaseInitializer;
import nexum.exception.ValidationException;
import nexum.service.ClienteService;
import nexum.service.CobrancaService;
import nexum.service.OrcamentoService;
import nexum.service.PrecificacaoService;
import nexum.web.NexumServer;

/**
 * Suite de testes automatizados para a Camada de Serviços e API HTTP / Servidor Web (Etapa 2).
 */
public class WebIntegrationTest {

    private static final String TEST_DB_PATH = "database/test_web_nexum.db";
    private static final int TEST_PORT = 8089;

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println(" INICIANDO TESTES DA ETAPA 2 (SERVICES & API WEB) ");
        System.out.println("==================================================");

        // 1. Inicializa banco isolado
        File testDb = new File(TEST_DB_PATH);
        if (testDb.exists()) testDb.delete();
        ConnectionFactory.setDbPath(TEST_DB_PATH);
        DatabaseInitializer.initialize();
        System.out.println("[OK] Banco de testes inicializado.");

        // 2. Testes de ClienteService
        ClienteService clienteService = new ClienteService();
        Cliente cliente = clienteService.cadastrar("Beatriz Engenheira", "222.333.444-55", "(11) 97777-8888");
        assert cliente.getId() > 0 : "Cliente deveria ter ID gerado";

        try {
            clienteService.cadastrar("Outra Pessoa", "222.333.444-55", "(11) 91111-2222");
            assert false : "Deveria ter lançado ValidationException para CPF duplicado";
        } catch (ValidationException e) {
            System.out.println("[OK] ClienteService: Bloqueio de CPF duplicado validado com sucesso.");
        }

        // 3. Testes de PrecificacaoService (Polimorfismo & Impostos)
        PrecificacaoService precificacaoService = new PrecificacaoService();

        // 3.1. Metro Quadrado: 100m² * R$ 50 = R$ 5000 + 6% imposto = R$ 5300
        ServicoPorMetroQuadrado sM2 = precificacaoService.criarServicoMetroQuadrado(
            cliente.getId(), "Projeto de Interiores M²", "2026-11-01", Padrao.ALTO, 0.06, 100.0, 50.0
        );
        PrecificacaoService.MemoriaCalculo memM2 = precificacaoService.calcularMemoria(sM2);
        assert Math.abs(memM2.valorBase() - 5000.0) < 0.01 : "Base M2 incorreta";
        assert Math.abs(memM2.valorTotalFinal() - 5300.0) < 0.01 : "Total M2 incorreto";

        // 3.2. Etapas
        List<ItemEtapa> itens = new ArrayList<>();
        itens.add(new ItemEtapa(Etapa.LEVANTAMENTO, 1200.0));
        itens.add(new ItemEtapa(Etapa.PROJETO_EXECUTIVO, 3800.0));
        ServicoPorEtapa sEtapa = precificacaoService.criarServicoEtapa(
            cliente.getId(), "Projeto por Etapas", "2026-11-02", Padrao.ALTO, 0.06, itens
        );
        PrecificacaoService.MemoriaCalculo memEtapa = precificacaoService.calcularMemoria(sEtapa);
        assert Math.abs(memEtapa.valorTotalFinal() - 5300.0) < 0.01 : "Total Etapa incorreto";

        // 3.3. Horas: 20h * R$ 250 = R$ 5000 + 6% = R$ 5300
        ServicoPorHora sHora = precificacaoService.criarServicoHora(
            cliente.getId(), "Consultoria Técnica", "2026-11-03", Padrao.MEDIO, 0.06, 20.0, 250.0
        );
        PrecificacaoService.MemoriaCalculo memHora = precificacaoService.calcularMemoria(sHora);
        assert Math.abs(memHora.valorTotalFinal() - 5300.0) < 0.01 : "Total Hora incorreto";

        // 3.4. Percentual de Obra: R$ 100.000 * 5% = R$ 5000 + 6% = R$ 5300
        ServicoPorPercentualObra sPerc = precificacaoService.criarServicoPercentualObra(
            cliente.getId(), "Gerenciamento Obra", "2026-11-04", Padrao.ALTO, 0.06, 100000.0, 0.05
        );
        PrecificacaoService.MemoriaCalculo memPerc = precificacaoService.calcularMemoria(sPerc);
        assert Math.abs(memPerc.valorTotalFinal() - 5300.0) < 0.01 : "Total Percentual incorreto";
        System.out.println("[OK] PrecificacaoService: Memória de cálculo e persistência das 4 modalidades validadas.");

        // 4. Testes de OrcamentoService e link de WhatsApp
        OrcamentoService orcamentoService = new OrcamentoService();
        Orcamento orcamento = orcamentoService.emitirOrcamento(cliente.getId(), sM2.getId(), 20, "2026-11-05");
        assert Math.abs(orcamento.getValorTotal() - 5300.0) < 0.01 : "Valor do orçamento incorreto";

        String urlWhats = orcamentoService.gerarLinkWhatsApp(orcamento.getId());
        assert urlWhats.startsWith("https://wa.me/5511977778888?text=") : "Link universal do WhatsApp inválido: " + urlWhats;
        assert urlWhats.contains("Beatriz") : "Link do WhatsApp deve conter nome do cliente";
        System.out.println("[OK] OrcamentoService: Emissão e formatação de link do WhatsApp validadas.");

        // 5. Testes de CobrancaService e Extrato
        CobrancaService cobrancaService = new CobrancaService();

        // 5.1. Validação de data retroativa
        try {
            cobrancaService.gerarCobrancaParaOrcamento(orcamento.getId(), "2020-01-01");
            assert false : "Deveria ter lançado ValidationException para data de vencimento anterior à data atual";
        } catch (ValidationException e) {
            System.out.println("[OK] CobrancaService: Bloqueio de vencimento retroativo validado com sucesso.");
        }

        // 5.2. Cadastro de cobrança válida com data futura
        String vencimentoValido = java.time.LocalDate.now().plusDays(30).toString();
        Cobranca cobranca = cobrancaService.gerarCobrancaParaOrcamento(orcamento.getId(), vencimentoValido);
        assert cobranca.getId() > 0 : "Cobrança deveria ter ID";
        assert cobranca.getStatusCobranca() == StatusCobranca.EM_ABERTO : "Status inicial deve ser EM_ABERTO";

        // 5.3. Validação da mensagem não-negativa (sem termo 'cobrança', tom cordial de investimento)
        String msgInvestimento = cobrancaService.gerarMensagemCobranca(cobranca.getId());
        assert !msgInvestimento.toLowerCase().contains("cobrança") : "Mensagem não deve conter a palavra 'cobrança': " + msgInvestimento;
        assert msgInvestimento.contains("investimento") : "Mensagem deve mencionar o investimento no projeto: " + msgInvestimento;
        System.out.println("[OK] CobrancaService: Mensagem cordial e não-negativa validada com sucesso.");

        cobrancaService.registrarPagamento(cobranca.getId(), java.time.LocalDate.now().toString());
        Cobranca cobrancaLiquidada = cobrancaService.buscarPorId(cobranca.getId());
        assert cobrancaLiquidada.getStatusCobranca() == StatusCobranca.PAGO : "Deveria estar paga";

        CobrancaService.ExtratoFinanceiro extrato = cobrancaService.obterExtratoFinanceiro();
        assert extrato.cobrancasPagas() == 1 : "Extrato deve contabilizar 1 cobrança paga";
        assert Math.abs(extrato.totalFaturado() - 5300.0) < 0.01 : "Total faturado incorreto";
        System.out.println("[OK] CobrancaService: Liquidação e Extrato Financeiro validados.");

        // 6. Testes do Servidor Web HTTP Nativo (NexumServer)
        NexumServer.start(TEST_PORT);
        HttpClient client = HttpClient.newHttpClient();

        // 6.1. GET / (Frontend Static Index.html)
        HttpResponse<String> indexResp = client.send(
            HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assert indexResp.statusCode() == 200 : "Index HTML status deveria ser 200";
        assert indexResp.body().contains("Nexum") : "Página inicial deveria conter título Nexum";
        System.out.println("[OK] NexumServer: Entrega de arquivo estático index.html validada.");

        // 6.2. GET /api/clientes
        HttpResponse<String> clientesResp = client.send(
            HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/clientes")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assert clientesResp.statusCode() == 200 : "API clientes status deveria ser 200";
        assert clientesResp.body().contains("Beatriz Engenheira") : "JSON de clientes deve conter cliente salvo";
        System.out.println("[OK] NexumServer: Endpoint GET /api/clientes respondendo JSON 200.");

        // 6.3. POST /api/servicos/calcular (Simulação de cálculo)
        String payloadCalculo = "{\"tipo\": \"METRO_QUADRADO\", \"area\": 80, \"valorMetroQuadrado\": 60, \"imposto\": 0.06}";
        HttpResponse<String> calculoResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/servicos/calcular"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payloadCalculo))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assert calculoResp.statusCode() == 200 : "API calcular status deveria ser 200";
        assert calculoResp.body().contains("4800.00") : "Valor base deveria ser 4800.00";
        assert calculoResp.body().contains("5088.00") : "Valor final com imposto deveria ser 5088.00";
        System.out.println("[OK] NexumServer: Endpoint POST /api/servicos/calcular validado.");

        // 6.4. GET /api/cobrancas/extrato
        HttpResponse<String> extratoResp = client.send(
            HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/cobrancas/extrato")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assert extratoResp.statusCode() == 200 : "API extrato status deveria ser 200";
        assert extratoResp.body().contains("totalFaturado") : "JSON do extrato deve conter totalFaturado";
        System.out.println("[OK] NexumServer: Endpoint GET /api/cobrancas/extrato validado.");

        // 6.5. DELETE /api/cobrancas/{id}
        Cobranca cobrancaParaExcluir = cobrancaService.gerarCobrancaParaOrcamento(orcamento.getId(), java.time.LocalDate.now().plusDays(15).toString());
        HttpResponse<String> deleteResp = client.send(
            HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/cobrancas/" + cobrancaParaExcluir.getId())).DELETE().build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assert deleteResp.statusCode() == 200 : "DELETE /api/cobrancas/{id} deveria retornar 200";
        assert cobrancaService.buscarPorId(cobrancaParaExcluir.getId()) == null : "Cobrança deveria ter sido removida do banco";
        System.out.println("[OK] NexumServer: Endpoint DELETE /api/cobrancas/{id} validado com sucesso.");

        // Finaliza o servidor de teste e limpa recursos
        NexumServer.stop();
        ConnectionFactory.setDbPath("database/nexum.db");
        testDb.delete();

        System.out.println("==================================================");
        System.out.println(" TODOS OS TESTES DA ETAPA 2 PASSARAM COM SUCESSO! ");
        System.out.println("==================================================");
    }
}
