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

        // 1.1. Teste de flexibilidade da ConnectionFactory (PostgreSQL vs SQLite)
        ConnectionFactory.setConnectionConfig("jdbc:postgresql://localhost:5432/nexum_cloud", "postgres", "secret123");
        assert ConnectionFactory.isPostgres() : "Deveria identificar PostgreSQL como ativo";
        assert ConnectionFactory.getDbPath().equals("jdbc:postgresql://localhost:5432/nexum_cloud") : "URL PostgreSQL incorreta";
        ConnectionFactory.setDbPath(TEST_DB_PATH);
        assert !ConnectionFactory.isPostgres() : "Deveria voltar para SQLite de teste";
        System.out.println("[OK] ConnectionFactory: Resolução dinâmica de PostgreSQL/SQLite validada.");

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

        // 3.0. Listagem inicial quando a tabela está vazia
        assert clienteService.listarTodos().isEmpty() : "A tabela de clientes deve iniciar vazia";
        System.out.println("[OK] ClienteService: Listagem de clientes vazia tratada corretamente.");

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
            usuarioService.cadastrar("Studio Fake", "fake@nexus.arq", "(11) 91234-5678", "00.000.000/0000-00", "Segredo@Forte2026");
            assert false : "Deveria ter bloqueado CNPJ inválido";
        } catch (ValidationException e) {
            System.out.println("[OK] UsuarioService: Bloqueio de CNPJ inválido validado com sucesso.");
        }

        // 4.2. Rejeição de senha fraca (menos de 12 caracteres, sem maiúscula ou sem caractere especial)
        try {
            usuarioService.cadastrar("Studio Nexus", "teste@nexus.arq", "(11) 91234-5678", "11.222.333/0001-81", "fraca123");
            assert false : "Deveria ter bloqueado senha com menos de 12 caracteres";
        } catch (ValidationException e) {
            System.out.println("[OK] UsuarioService: Bloqueio de senha curta validado com sucesso.");
        }

        try {
            usuarioService.cadastrar("Studio Nexus", "teste@nexus.arq", "(11) 91234-5678", "11.222.333/0001-81", "senhalongasemespecial123");
            assert false : "Deveria ter bloqueado senha sem caractere especial e maiúscula";
        } catch (ValidationException e) {
            System.out.println("[OK] UsuarioService: Bloqueio de senha sem maiúscula/especial validado com sucesso.");
        }

        // 4.3. Cadastro de arquiteto com CNPJ válido, senha forte (>= 12 chars, maiúscula e especial) e RBT12
        Usuario arquiteto = usuarioService.cadastrar(
            "Studio Nexus Arquitetura", "contato@nexus.arq", "(11) 91234-5678", "11.222.333/0001-81", "Segredo@Forte2026", 300000.0
        );
        assert arquiteto.getId() > 0 : "ID de usuário deve ser gerado";
        assert Math.abs(arquiteto.getRbt12() - 300000.0) < 0.01 : "RBT12 inicial do arquiteto deve ser 300.000,00";

        // 4.4. Bloqueio de CNPJ duplicado
        try {
            usuarioService.cadastrar("Outro Studio", "outro@nexus.arq", "(11) 99999-8888", "11.222.333/0001-81", "Outra@SenhaForte2026");
            assert false : "Deveria ter bloqueado CNPJ duplicado";
        } catch (ValidationException e) {
            System.out.println("[OK] UsuarioService: Bloqueio de CNPJ duplicado validado com sucesso.");
        }

        // 4.5. Login com sucesso (por CNPJ e por E-mail)
        Usuario logadoCnpj = usuarioService.login("11.222.333/0001-81", "Segredo@Forte2026");
        assert logadoCnpj.getId() == arquiteto.getId() : "Login por CNPJ falhou";

        Usuario logadoEmail = usuarioService.login("contato@nexus.arq", "Segredo@Forte2026");
        assert logadoEmail.getId() == arquiteto.getId() : "Login por e-mail falhou";

        // 4.6. Bloqueio de senha incorreta
        try {
            usuarioService.login("contato@nexus.arq", "SenhaErrada@2026");
            assert false : "Deveria ter bloqueado senha incorreta";
        } catch (ValidationException e) {
            System.out.println("[OK] UsuarioService: Login com PBKDF2 e bloqueio de senha incorreta validados.");
        }

        // 5. Testes de PrecificacaoService e SimplesNacionalService (Anexo III)
        nexum.service.SimplesNacionalService simplesService = new nexum.service.SimplesNacionalService();

        // 5.1. Faixa 1 (<= 180.000)
        var resF1 = simplesService.calcular(150000.0);
        assert resF1.faixa() == 1 : "Deveria ser Faixa 1";
        assert Math.abs(resF1.aliquotaEfetiva() - 0.060) < 0.0001 : "Alíquota efetiva Faixa 1 deve ser 6.00%";

        // 5.2. Faixa 2 (180.000,01 a 360.000,00)
        var resF2 = simplesService.calcular(300000.0);
        assert resF2.faixa() == 2 : "Deveria ser Faixa 2";
        // ((300000 * 0.112) - 9360) / 300000 = (33600 - 9360) / 300000 = 24240 / 300000 = 0.0808 (8.08%)
        assert Math.abs(resF2.aliquotaEfetiva() - 0.0808) < 0.0001 : "Alíquota efetiva Faixa 2 deve ser 8.08%";

        // 5.3. Faixa 3 (360.000,01 a 720.000,00)
        var resF3 = simplesService.calcular(500000.0);
        assert resF3.faixa() == 3 : "Deveria ser Faixa 3";
        // ((500000 * 0.135) - 17640) / 500000 = (67500 - 17640) / 500000 = 49860 / 500000 = 0.09972
        assert Math.abs(resF3.aliquotaEfetiva() - 0.09972) < 0.0001 : "Alíquota efetiva Faixa 3 incorreta";

        // 5.4. Faixa 4 (720.000,01 a 1.800.000,00)
        var resF4 = simplesService.calcular(1000000.0);
        assert resF4.faixa() == 4 : "Deveria ser Faixa 4";
        // ((1000000 * 0.16) - 35640) / 1000000 = (160000 - 35640) / 1000000 = 124360 / 1000000 = 0.12436
        assert Math.abs(resF4.aliquotaEfetiva() - 0.12436) < 0.0001 : "Alíquota efetiva Faixa 4 incorreta";

        // 5.5. Faixa 5 (1.800.000,01 a 3.600.000,00)
        var resF5 = simplesService.calcular(2000000.0);
        assert resF5.faixa() == 5 : "Deveria ser Faixa 5";
        // ((2000000 * 0.21) - 125640) / 2000000 = (420000 - 125640) / 2000000 = 294360 / 2000000 = 0.14718
        assert Math.abs(resF5.aliquotaEfetiva() - 0.14718) < 0.0001 : "Alíquota efetiva Faixa 5 incorreta";

        // 5.6. Faixa 6 (3.600.000,01 a 4.800.000,00)
        var resF6 = simplesService.calcular(4000000.0);
        assert resF6.faixa() == 6 : "Deveria ser Faixa 6";
        // ((4000000 * 0.33) - 648000) / 4000000 = (1320000 - 648000) / 4000000 = 672000 / 4000000 = 0.168
        assert Math.abs(resF6.aliquotaEfetiva() - 0.1680) < 0.0001 : "Alíquota efetiva Faixa 6 incorreta";

        // 5.7. Excesso de teto (> 4.800.000,00)
        try {
            simplesService.calcular(5000000.0);
            assert false : "Deveria ter lançado ValidationException para RBT12 acima de 4.8M";
        } catch (ValidationException e) {
            System.out.println("[OK] SimplesNacionalService: Bloqueio de RBT12 > 4.8M validado.");
        }

        System.out.println("[OK] SimplesNacionalService: Todas as 6 faixas do Anexo III validadas com sucesso.");

        // 5.8. PrecificacaoService com integração ao Simples Nacional
        PrecificacaoService precificacaoService = new PrecificacaoService();

        ServicoPorMetroQuadrado sM2 = precificacaoService.criarServicoMetroQuadrado(
            cliente.getId(), "Projeto de Interiores M²", "2026-11-01", Padrao.ALTO, 0.06, 100.0, 50.0
        );
        PrecificacaoService.MemoriaCalculo memM2 = precificacaoService.calcularMemoria(sM2);
        assert Math.abs(memM2.valorBase() - 5000.0) < 0.01 : "Base M2 incorreta";
        assert Math.abs(memM2.valorTotalFinal() - 5300.0) < 0.01 : "Total M2 incorreto";

        // Memória com RBT12 Faixa 2 (R$ 300.000) -> 8.08%
        PrecificacaoService.MemoriaCalculo memM2Simples = precificacaoService.calcularMemoria(sM2, 300000.0);
        assert memM2Simples.faixaSimples() == 2 : "Faixa deve ser 2";
        assert Math.abs(memM2Simples.aliquotaImposto() - 0.0808) < 0.001 : "Alíquota efetiva deve ser 8.08%";
        assert Math.abs(memM2Simples.valorImposto() - 404.0) < 0.01 : "Imposto deve ser 404.00";
        assert Math.abs(memM2Simples.valorTotalFinal() - 5404.0) < 0.01 : "Total com imposto efetivo deve ser 5404.00";

        List<ItemEtapa> itens = new ArrayList<>();
        itens.add(new ItemEtapa(Etapa.LEVANTAMENTO, 1200.0));
        itens.add(new ItemEtapa(Etapa.PROJETO_EXECUTIVO, 3800.0));
        ServicoPorEtapa sEtapa = precificacaoService.criarServicoEtapa(
            cliente.getId(), "Projeto por Etapas", "2026-11-02", Padrao.ALTO, 0.06, itens
        );
        PrecificacaoService.MemoriaCalculo memEtapa = precificacaoService.calcularMemoria(sEtapa);
        assert Math.abs(memEtapa.valorTotalFinal() - 5300.0) < 0.01 : "Total Etapa incorreto";
        System.out.println("[OK] PrecificacaoService: Reconstrução polimórfica e Simples Nacional validados.");

        // 6. Testes de OrcamentoService e link de WhatsApp
        OrcamentoService orcamentoService = new OrcamentoService();
        Orcamento orcamento = orcamentoService.emitirOrcamento(cliente.getId(), sM2.getId(), 20, "2026-11-05");
        assert Math.abs(orcamento.getValorTotal() - 5300.0) < 0.01 : "Valor do orçamento incorreto";

        String urlWhats = orcamentoService.gerarLinkWhatsApp(orcamento.getId());
        assert urlWhats.startsWith("https://wa.me/5511977778888?text=") : "Link universal do WhatsApp inválido";
        String decodedWhats = java.net.URLDecoder.decode(urlWhats, java.nio.charset.StandardCharsets.UTF_8);
        assert decodedWhats.contains("Olá, Beatriz!") : "Mensagem de proposta WhatsApp deve saudar apenas pelo primeiro nome";
        assert !decodedWhats.contains("Beatriz Engenheira") : "Mensagem de proposta WhatsApp não deve conter o sobrenome na saudação";
        assert decodedWhats.contains("R$ 5.300,00") : "Mensagem de proposta WhatsApp deve conter o valor em BRL (R$ 5.300,00)";
        assert decodedWhats.contains("Proposta válida até 25/11/2026") : "Mensagem de proposta WhatsApp deve conter a data limite exata (05/11/2026 + 20 dias = 25/11/2026)";
        assert !decodedWhats.contains("20 dias") : "Mensagem de proposta WhatsApp não deve mais exibir prazo relativo em dias";
        System.out.println("[OK] OrcamentoService: Emissão de proposta e link wa.me (primeiro nome + BRL + validade fixa dd/MM/yyyy) validados.");

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

        // 7.3. Validação de mensagem amigável sem termo 'cobrança' e com primeiro nome + BRL + dd/MM/yyyy
        String msgInvestimento = cobrancaService.gerarMensagemCobranca(cobranca.getId());
        assert !msgInvestimento.toLowerCase().contains("cobrança") : "Mensagem não pode conter a palavra 'cobrança'";
        assert msgInvestimento.contains("Olá, Beatriz!") : "Mensagem de cobrança deve saudar apenas pelo primeiro nome";
        assert !msgInvestimento.contains("Beatriz Engenheira") : "Mensagem de cobrança não deve conter o sobrenome na saudação";
        assert msgInvestimento.contains("investimento") : "Mensagem deve mencionar investimento no projeto";
        assert msgInvestimento.contains("R$ 5.300,00") : "Mensagem de cobrança deve exibir o valor em formato BRL (R$ 5.300,00)";
        assert msgInvestimento.contains(Cobranca.formatarDataBR(vencimentoValido)) : "Mensagem de cobrança deve exibir vencimento em dd/MM/yyyy";
        System.out.println("[OK] CobrancaService: Mensagem não-negativa, cordial e formatada (primeiro nome + BRL + dd/MM/yyyy) validada.");

        // 7.4. Liquidação e Extrato
        cobrancaService.registrarPagamento(cobranca.getId(), java.time.LocalDate.now().toString());
        Cobranca cobrancaLiquidada = cobrancaService.buscarPorId(cobranca.getId());
        assert cobrancaLiquidada.getStatusCobranca() == StatusCobranca.PAGO : "Deveria estar paga";
        System.out.println("[OK] CobrancaService: Liquidação validada.");

        // 7.5. Bloqueio de geração de cobrança para proposta recusada (Regra de Negócio Etapa 5)
        Orcamento orcRecusado = orcamentoService.emitirOrcamento(cliente.getId(), sEtapa.getId(), 15, "2026-11-06");
        orcamentoService.atualizarStatus(orcRecusado.getId(), StatusOrcamento.RECUSADO);
        try {
            cobrancaService.gerarCobrancaParaOrcamento(orcRecusado.getId(), vencimentoValido);
            assert false : "Deveria ter lançado ValidationException ao tentar gerar cobrança para proposta recusada";
        } catch (ValidationException e) {
            System.out.println("[OK] CobrancaService: Trava defensiva contra cobrança em proposta recusada validada.");
        }

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
            assert indexResp.body().contains("Não há clientes cadastrados.") : "HTML deve conter mensagem para lista vazia";
            assert indexResp.body().contains("id=\"busca-clientes\"") : "HTML deve conter barra de busca de clientes";
            assert indexResp.body().contains("id=\"busca-orcamentos\"") : "HTML deve conter barra de busca de propostas";
            assert indexResp.body().contains("id=\"busca-cobrancas\"") : "HTML deve conter barra de busca de cobranças";
            assert !indexResp.body().contains("Simular Cálculo") : "Botão inútil de simulação deve ter sido removido";
            System.out.println("[OK] NexumServer: Entrega de HTML com barras de busca dinâmicas e mensagem amigável validada.");

            // 8.2. POST /api/auth/login via HTTP API
            String payloadLogin = "{\"identificador\": \"contato@nexus.arq\", \"senha\": \"Segredo@Forte2026\"}";
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
            assert perfilResp.body().contains("300000.00") : "Perfil deve retornar o RBT12 cadastrado";
            System.out.println("[OK] NexumServer: Endpoint GET /api/auth/perfil autenticado (com RBT12) validado.");

            // 8.4. POST /api/servicos/calcular buscando RBT12 diretamente do perfil do usuário logado
            String payloadCalculo = "{\"tipo\": \"METRO_QUADRADO\", \"area\": 80, \"valorMetroQuadrado\": 60}";
            HttpResponse<String> calculoResp = client.send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + TEST_PORT + "/api/servicos/calcular"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(payloadCalculo))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            assert calculoResp.statusCode() == 200 : "API calcular status deveria ser 200";
            assert calculoResp.body().contains("\"faixaSimples\": 2") : "Deveria retornar faixa 2 do Simples com base no perfil logado";
            assert calculoResp.body().contains("0.0808") : "Alíquota efetiva calculada deve ser 8.08%";
            assert calculoResp.body().contains("5187.84") : "Valor final com imposto efetivo deveria ser 5187.84";
            System.out.println("[OK] NexumServer: Endpoint POST /api/servicos/calcular com RBT12 do perfil logado validado.");

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

            // 8.6. POST /api/cobrancas para orçamento recusado deve retornar HTTP 400
            String payloadCobrancaRecusada = "{\"orcamentoId\": " + orcRecusado.getId() + ", \"dataVencimento\": \"" + vencimentoValido + "\"}";
            HttpResponse<String> cobRecusadaResp = client.send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + TEST_PORT + "/api/cobrancas"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadCobrancaRecusada))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            assert cobRecusadaResp.statusCode() == 400 : "POST /api/cobrancas para proposta recusada deveria retornar HTTP 400";
            System.out.println("[OK] NexumServer: Bloqueio HTTP 400 ao tentar cobrar proposta recusada validado.");

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
