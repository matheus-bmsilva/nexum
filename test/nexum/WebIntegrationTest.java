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
import nexum.exception.EntityNotFoundException;
import nexum.exception.ValidationException;
import nexum.service.ClienteService;
import nexum.service.CobrancaService;
import nexum.service.OrcamentoService;
import nexum.service.PrecificacaoService;
import nexum.service.UsuarioService;
import nexum.validation.DocumentoValidator;
import nexum.web.NexumServer;

/**
 * Suite de testes automatizados de integração para Autenticação, Validações Rigorosas
 * e API Web HTTP Nativa do Nexum.
 */
public class WebIntegrationTest {

    private static final String TEST_DB_PATH = "database/test_web_nexum.db";
    private static final int TEST_PORT = 8089;

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println(" INICIANDO TESTES DE INTEGRACAO NEXUM (FULL MVP)  ");
        System.out.println("==================================================");

        // 1. Inicializa banco isolado
        File testDb = new File(TEST_DB_PATH);
        if (testDb.exists()) testDb.delete();
        ConnectionFactory.setDbPath(TEST_DB_PATH);
        DatabaseInitializer.initialize();
        System.out.println("[OK] Banco de testes inicializado.");

        // 2. Validação Matemática de Documentos (CPF e CNPJ)
        assert DocumentoValidator.isCpfValido("529.982.247-25") : "CPF válido deveria ser aprovado";
        assert !DocumentoValidator.isCpfValido("111.111.111-11") : "CPF com números repetidos deve ser rejeitado";
        assert !DocumentoValidator.isCpfValido("123.456.789-00") : "CPF com dígitos verificadores errados deve ser rejeitado";

        assert DocumentoValidator.isCnpjValido("11.222.333/0001-81") : "CNPJ válido deveria ser aprovado";
        assert !DocumentoValidator.isCnpjValido("11.111.111/1111-11") : "CNPJ com números repetidos deve ser rejeitado";
        assert !DocumentoValidator.isCnpjValido("11.222.333/0001-00") : "CNPJ com dígitos verificadores errados deve ser rejeitado";
        System.out.println("[OK] DocumentoValidator: Validação matemática oficial de CPF e CNPJ aprovada.");

        // 3. Testes de ClienteService e Unicidade
        ClienteService clienteService = new ClienteService();

        // 3.1. Rejeição de CPF inválido
        try {
            clienteService.cadastrar("Cliente Invalido", "111.111.111-11", "(11) 98888-7777");
            assert false : "Deveria ter lançado ValidationException para CPF inválido";
        } catch (ValidationException e) {
            System.out.println("[OK] ClienteService: Bloqueio de CPF inválido validado com sucesso.");
        }

        // 3.2. Cadastro válido
        Cliente cliente = clienteService.cadastrar("Beatriz Engenheira", "529.982.247-25", "(11) 97777-8888");
        assert cliente.getId() > 0 : "Cliente deveria ter ID gerado";

        // 3.3. Rejeição de duplicidade
        try {
            clienteService.cadastrar("Outra Pessoa", "529.982.247-25", "(11) 91111-2222");
            assert false : "Deveria ter lançado ValidationException para CPF duplicado";
        } catch (ValidationException e) {
            System.out.println("[OK] ClienteService: Bloqueio de CPF duplicado validado com sucesso.");
        }

        // 4. Testes de UsuarioService (Autenticação, Senhas e CNPJ)
        UsuarioService usuarioService = new UsuarioService();

        // 4.1. Rejeição de CNPJ inválido
        try {
            usuarioService.cadastrar("Studio Fake", "fake@nexus.arq", "(11) 91234-5678", "00.000.000/0000-00", "senha123");
            assert false : "Deveria ter bloqueado CNPJ inválido";
        } catch (ValidationException e) {
            System.out.println("[OK] UsuarioService: Bloqueio de CNPJ inválido validado com sucesso.");
        }

        // 4.2. Cadastro de arquiteto com CNPJ válido
        Usuario arquiteto = usuarioService.cadastrar(
            "Studio Nexus Arquitetura", "contato@nexus.arq", "(11) 91234-5678", "11.222.333/0001-81", "segredoForte123"
        );
        assert arquiteto.getId() > 0 : "ID de usuário deve ser gerado";

        // 4.3. Bloqueio de CNPJ duplicado
        try {
            usuarioService.cadastrar("Outro Studio", "outro@nexus.arq", "(11) 99999-8888", "11.222.333/0001-81", "outraSenha123");
            assert false : "Deveria ter bloqueado CNPJ duplicado";
        } catch (ValidationException e) {
            System.out.println("[OK] UsuarioService: Bloqueio de CNPJ duplicado validado com sucesso.");
        }

        // 4.4. Login com sucesso (por CNPJ e por E-mail)
        Usuario logadoCnpj = usuarioService.login("11.222.333/0001-81", "segredoForte123");
        assert logadoCnpj.getId() == arquiteto.getId() : "Login por CNPJ falhou";

        Usuario logadoEmail = usuarioService.login("contato@nexus.arq", "segredoForte123");
        assert logadoEmail.getId() == arquiteto.getId() : "Login por e-mail falhou";

        // 4.5. Bloqueio de senha incorreta
        try {
            usuarioService.login("contato@nexus.arq", "senhaErrada");
            assert false : "Deveria ter bloqueado senha incorreta";
        } catch (ValidationException e) {
            System.out.println("[OK] UsuarioService: Login com PBKDF2 e bloqueio de senha incorreta validados.");
        }

        // 5. Testes de PrecificacaoService (Polimorfismo & Memória de Cálculo)
        PrecificacaoService precificacaoService = new PrecificacaoService();

        ServicoPorMetroQuadrado sM2 = precificacaoService.criarServicoMetroQuadrado(
            cliente.getId(), "Projeto de Interiores M²", "2026-11-01", Padrao.ALTO, 0.06, 100.0, 50.0
        );
        PrecificacaoService.MemoriaCalculo memM2 = precificacaoService.calcularMemoria(sM2);
        assert Math.abs(memM2.valorBase() - 5000.0) < 0.01 : "Base M2 incorreta";
        assert Math.abs(memM2.valorTotalFinal() - 5300.0) < 0.01 : "Total M2 incorreto";

        List<ItemEtapa> itens = new ArrayList<>();
        itens.add(new ItemEtapa(Etapa.LEVANTAMENTO, 1200.0));
        itens.add(new ItemEtapa(Etapa.PROJETO_EXECUTIVO, 3800.0));
        ServicoPorEtapa sEtapa = precificacaoService.criarServicoEtapa(
            cliente.getId(), "Projeto por Etapas", "2026-11-02", Padrao.ALTO, 0.06, itens
        );
        PrecificacaoService.MemoriaCalculo memEtapa = precificacaoService.calcularMemoria(sEtapa);
        assert Math.abs(memEtapa.valorTotalFinal() - 5300.0) < 0.01 : "Total Etapa incorreto";
        System.out.println("[OK] PrecificacaoService: Reconstrução polimórfica e tributação validadas.");

        // 6. Testes de OrcamentoService e link de WhatsApp
        OrcamentoService orcamentoService = new OrcamentoService();
        Orcamento orcamento = orcamentoService.emitirOrcamento(cliente.getId(), sM2.getId(), 20, "2026-11-05");
        assert Math.abs(orcamento.getValorTotal() - 5300.0) < 0.01 : "Valor do orçamento incorreto";

        String urlWhats = orcamentoService.gerarLinkWhatsApp(orcamento.getId());
        assert urlWhats.startsWith("https://wa.me/5511977778888?text=") : "Link universal do WhatsApp inválido";
        assert urlWhats.contains("Beatriz") : "Link deve conter nome do cliente";
        System.out.println("[OK] OrcamentoService: Emissão de proposta e link wa.me validados.");

        // 7. Testes de CobrancaService, Validação de Data e Mensagem Amigável
        CobrancaService cobrancaService = new CobrancaService();

        // 7.1. Bloqueio de vencimento retroativo
        try {
            cobrancaService.gerarCobrancaParaOrcamento(orcamento.getId(), "2020-01-01");
            assert false : "Deveria ter lançado ValidationException para data retroativa";
        } catch (ValidationException e) {
            System.out.println("[OK] CobrancaService: Bloqueio de vencimento retroativo validado com sucesso.");
        }

        // 7.2. Cadastro de cobrança válida com data futura
        String vencimentoValido = java.time.LocalDate.now().plusDays(30).toString();
        Cobranca cobranca = cobrancaService.gerarCobrancaParaOrcamento(orcamento.getId(), vencimentoValido);
        assert cobranca.getId() > 0 : "Cobrança deveria ter ID";

        // 7.3. Validação de mensagem amigável sem termo 'cobrança'
        String msgInvestimento = cobrancaService.gerarMensagemCobranca(cobranca.getId());
        assert !msgInvestimento.toLowerCase().contains("cobrança") : "Mensagem não pode conter a palavra 'cobrança'";
        assert msgInvestimento.contains("investimento") : "Mensagem deve mencionar investimento no projeto";
        System.out.println("[OK] CobrancaService: Mensagem não-negativa e cordial validada.");

        // 7.4. Liquidação e Extrato
        cobrancaService.registrarPagamento(cobranca.getId(), java.time.LocalDate.now().toString());
        Cobranca cobrancaLiquidada = cobrancaService.buscarPorId(cobranca.getId());
        assert cobrancaLiquidada.getStatusCobranca() == StatusCobranca.PAGO : "Deveria estar paga";
        System.out.println("[OK] CobrancaService: Liquidação validada.");

        // 8. Testes do Servidor Web HTTP Nativo (NexumServer)
        try {
            NexumServer.start(TEST_PORT);
            HttpClient client = HttpClient.newHttpClient();

            // 8.1. GET / (Frontend estático)
            HttpResponse<String> indexResp = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            assert indexResp.statusCode() == 200 : "Index HTML status deveria ser 200";
            assert indexResp.body().contains("Nexum") : "Página deve conter Nexum";
            assert !indexResp.body().contains("Simular Cálculo") : "Botão inútil de simulação deve ter sido removido";
            System.out.println("[OK] NexumServer: Entrega de HTML sem botão de simulação validada.");

            // 8.2. POST /api/auth/login via HTTP API
            String payloadLogin = "{\"identificador\": \"contato@nexus.arq\", \"senha\": \"segredoForte123\"}";
            HttpResponse<String> loginResp = client.send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadLogin))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            assert loginResp.statusCode() == 200 : "Login HTTP deve retornar 200";
            assert loginResp.body().contains("token") : "Resposta deve conter token";
            System.out.println("[OK] NexumServer: Endpoint POST /api/auth/login validado.");

            // Extrai token simples
            String token = loginResp.body().replaceAll(".*\"token\":\\s*\"([^\"]+)\".*", "$1");

            // 8.3. GET /api/auth/perfil com Bearer token
            HttpResponse<String> perfilResp = client.send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/perfil"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            assert perfilResp.statusCode() == 200 : "Perfil HTTP deve retornar 200 com token";
            assert perfilResp.body().contains("Studio Nexus Arquitetura") : "Perfil deve conter o nome do arquiteto";
            System.out.println("[OK] NexumServer: Endpoint GET /api/auth/perfil autenticado validado.");

            // 8.4. POST /api/servicos/calcular (Cálculo em tempo real)
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
            assert calculoResp.body().contains("5088.00") : "Valor final com imposto deveria ser 5088.00";
            System.out.println("[OK] NexumServer: Endpoint POST /api/servicos/calcular validado.");

            // 8.5. DELETE /api/cobrancas/{id}
            Cobranca cobrancaParaExcluir = cobrancaService.gerarCobrancaParaOrcamento(orcamento.getId(), java.time.LocalDate.now().plusDays(15).toString());
            int idExcluir = cobrancaParaExcluir.getId();
            HttpResponse<String> deleteResp = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/cobrancas/" + idExcluir)).DELETE().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            assert deleteResp.statusCode() == 200 : "DELETE /api/cobrancas/{id} deveria retornar 200";

            boolean foiRemovido = false;
            try {
                cobrancaService.buscarPorId(idExcluir);
            } catch (EntityNotFoundException e) {
                foiRemovido = true;
            }
            assert foiRemovido : "Cobrança deveria ter sido removida do banco de dados";
            System.out.println("[OK] NexumServer: Endpoint DELETE /api/cobrancas/{id} validado com sucesso.");

        } finally {
            NexumServer.stop();
            ConnectionFactory.setDbPath("database/nexum.db");
            testDb.delete();
        }

        System.out.println("==================================================");
        System.out.println(" TODOS OS TESTES PASSARAM COM 100% DE SUCESSO!    ");
        System.out.println("==================================================");
    }
}
