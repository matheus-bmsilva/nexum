package nexum;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import nexum.database.ConnectionFactory;
import nexum.database.DatabaseInitializer;
import nexum.repository.ClienteRepository;
import nexum.repository.CobrancaRepository;
import nexum.repository.OrcamentoRepository;
import nexum.repository.ServicoRepository;
import nexum.repository.UsuarioRepository;
import nexum.repository.sqlite.SqliteClienteRepository;
import nexum.repository.sqlite.SqliteCobrancaRepository;
import nexum.repository.sqlite.SqliteOrcamentoRepository;
import nexum.repository.sqlite.SqliteServicoRepository;
import nexum.repository.sqlite.SqliteUsuarioRepository;

/**
 * Suite de verificação e testes de integração da Camada de Persistência (Etapa 1).
 * Executa testes end-to-end com banco de dados SQLite isolado e validação de polimorfismo.
 */
public class DatabaseIntegrationTest {

    private static final String TEST_DB_PATH = "database/test_nexum.db";

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" INICIANDO TESTES DE INTEGRACAO DA PERSISTENCIA   ");
        System.out.println("==================================================");

        // 1. Configura banco de dados de teste isolado e inicializa DDL
        File testDb = new File(TEST_DB_PATH);
        if (testDb.exists()) {
            testDb.delete();
        }
        ConnectionFactory.setDbPath(TEST_DB_PATH);
        DatabaseInitializer.initialize();
        System.out.println("[OK] Banco de dados e DDL inicializados com sucesso.");

        ClienteRepository clienteRepo = new SqliteClienteRepository();
        UsuarioRepository usuarioRepo = new SqliteUsuarioRepository();
        ServicoRepository servicoRepo = new SqliteServicoRepository(clienteRepo);
        OrcamentoRepository orcamentoRepo = new SqliteOrcamentoRepository(servicoRepo, clienteRepo);
        CobrancaRepository cobrancaRepo = new SqliteCobrancaRepository(orcamentoRepo);

        // 2. Teste de CRUD de Cliente
        Cliente cliente = new Cliente("Carlos Eduardo", "111.222.333-44", "(11) 98765-4321");
        clienteRepo.salvar(cliente);
        assert cliente.getId() > 0 : "ID do cliente gerado deve ser maior que zero";

        Optional<Cliente> clienteBuscado = clienteRepo.buscarPorCpf("111.222.333-44");
        assert clienteBuscado.isPresent() : "Cliente deve ser localizado pelo CPF";
        assert "Carlos Eduardo".equals(clienteBuscado.get().getNome()) : "Nome do cliente incompatível";

        cliente.setNome("Carlos E. Arquiteto");
        clienteRepo.atualizar(cliente);
        Cliente clienteAtualizado = clienteRepo.buscarPorId(cliente.getId()).orElseThrow();
        assert "Carlos E. Arquiteto".equals(clienteAtualizado.getNome()) : "Nome não foi atualizado";
        System.out.println("[OK] ClienteRepository: Salvar, BuscarPorCpf, BuscarPorId e Atualizar validados.");

        // 3. Teste de UsuarioRepository (incluindo persistência e atualização de RBT12)
        Usuario usuario = new Usuario("Matheus Silva", "matheus@nexus.arq", "hashSenhaForte123", "(11) 91234-5678", "12.345.678/0001-90", 240000.0);
        usuarioRepo.salvar(usuario);
        assert usuario.getId() > 0 : "ID de usuario deve ser gerado";

        Optional<Usuario> usuarioBuscado = usuarioRepo.buscarPorCnpj("12.345.678/0001-90");
        assert usuarioBuscado.isPresent() : "Usuário deve ser encontrado por CNPJ";
        assert "Matheus Silva".equals(usuarioBuscado.get().getNome()) : "Nome do usuário incorreto";
        assert Math.abs(usuarioBuscado.get().getRbt12() - 240000.0) < 0.01 : "RBT12 do usuário não foi persistido corretamente";

        usuario.setRbt12(350000.0);
        usuarioRepo.atualizar(usuario);
        Usuario usuarioAtualizado = usuarioRepo.buscarPorId(usuario.getId()).orElseThrow();
        assert Math.abs(usuarioAtualizado.getRbt12() - 350000.0) < 0.01 : "RBT12 do usuário não foi atualizado corretamente";
        System.out.println("[OK] UsuarioRepository: Salvar, BuscarPorCnpj e Atualizar (com RBT12) validados.");

        // 4. Teste de Persistência Polimórfica de Serviços (4 modalidades)
        // 4.1. ServicoPorMetroQuadrado: 150m² * R$ 40/m² = R$ 6000.0
        ServicoPorMetroQuadrado sM2 = new ServicoPorMetroQuadrado(
            0, "Projeto Residencial Vila Madalena", "2026-11-01", Padrao.ALTO, clienteAtualizado, 0.06, 150.0, 40.0
        );
        servicoRepo.salvar(sM2);
        assert sM2.getId() > 0 : "ID de ServicoPorMetroQuadrado deve ser gerado";

        // 4.2. ServicoPorEtapa: Levantamento (1000) + Estudo (2000) + Executivo (4500) = R$ 7500.0
        List<ItemEtapa> itens = new ArrayList<>();
        itens.add(new ItemEtapa(Etapa.LEVANTAMENTO, 1000.0));
        itens.add(new ItemEtapa(Etapa.ESTUDO_PRELIMINAR, 2000.0));
        itens.add(new ItemEtapa(Etapa.PROJETO_EXECUTIVO, 4500.0));
        ServicoPorEtapa sEtapa = new ServicoPorEtapa(
            0, "Projeto Comercial Jardins", "2026-11-02", Padrao.ALTO, clienteAtualizado, 0.06, itens
        );
        servicoRepo.salvar(sEtapa);
        assert sEtapa.getId() > 0 : "ID de ServicoPorEtapa deve ser gerado";

        // 4.3. ServicoPorHora: 35 horas * R$ 120/h = R$ 4200.0
        ServicoPorHora sHora = new ServicoPorHora(
            0, "Consultoria de Interiores", "2026-11-03", Padrao.MEDIO, clienteAtualizado, 0.06, 35.0, 120.0
        );
        servicoRepo.salvar(sHora);
        assert sHora.getId() > 0 : "ID de ServicoPorHora deve ser gerado";

        // 4.4. ServicoPorPercentualObra: Obra R$ 500.000 * 5% (0.05) = R$ 25000.0
        ServicoPorPercentualObra sPerc = new ServicoPorPercentualObra(
            0, "Gerenciamento Global da Obra", "2026-11-04", Padrao.ALTO, clienteAtualizado, 0.06, 500000.0, 0.05
        );
        servicoRepo.salvar(sPerc);
        assert sPerc.getId() > 0 : "ID de ServicoPorPercentualObra deve ser gerado";

        // Validação da Reconstrução Polimórfica (LSP e OCP)
        Servico recuperadoM2 = servicoRepo.buscarPorId(sM2.getId()).orElseThrow();
        assert recuperadoM2 instanceof ServicoPorMetroQuadrado : "Deveria ser instância de ServicoPorMetroQuadrado";
        assert Math.abs(recuperadoM2.calcularValor() - 6000.0) < 0.001 : "Cálculo M² incorreto: " + recuperadoM2.calcularValor();

        Servico recuperadoEtapa = servicoRepo.buscarPorId(sEtapa.getId()).orElseThrow();
        assert recuperadoEtapa instanceof ServicoPorEtapa : "Deveria ser instância de ServicoPorEtapa";
        assert ((ServicoPorEtapa) recuperadoEtapa).getItensEtapas().size() == 3 : "Deveria conter 3 itens de etapa";
        assert Math.abs(recuperadoEtapa.calcularValor() - 7500.0) < 0.001 : "Cálculo Etapas incorreto: " + recuperadoEtapa.calcularValor();

        Servico recuperadoHora = servicoRepo.buscarPorId(sHora.getId()).orElseThrow();
        assert recuperadoHora instanceof ServicoPorHora : "Deveria ser instância de ServicoPorHora";
        assert Math.abs(recuperadoHora.calcularValor() - 4200.0) < 0.001 : "Cálculo Horas incorreto: " + recuperadoHora.calcularValor();

        Servico recuperadoPerc = servicoRepo.buscarPorId(sPerc.getId()).orElseThrow();
        assert recuperadoPerc instanceof ServicoPorPercentualObra : "Deveria ser instância de ServicoPorPercentualObra";
        assert Math.abs(recuperadoPerc.calcularValor() - 25000.0) < 0.001 : "Cálculo Percentual incorreto: " + recuperadoPerc.calcularValor();
        System.out.println("[OK] ServicoRepository: Persistência e reconstrução polimórfica de todas as 4 modalidades validadas.");

        // 5. Teste de OrcamentoRepository
        Orcamento orcamento = new Orcamento(
            0, "2026-11-05", 15, recuperadoM2.calcularValor(), StatusOrcamento.PENDENTE, recuperadoM2, clienteAtualizado
        );
        orcamentoRepo.salvar(orcamento);
        assert orcamento.getId() > 0 : "ID do orcamento deve ser gerado";

        Orcamento orcamentoRecuperado = orcamentoRepo.buscarPorId(orcamento.getId()).orElseThrow();
        assert orcamentoRecuperado.getStatusOrcamento() == StatusOrcamento.PENDENTE : "Status inicial deve ser PENDENTE";
        assert orcamentoRecuperado.getServico() != null : "Serviço associado não pode ser nulo";
        assert orcamentoRecuperado.getCliente() != null : "Cliente associado não pode ser nulo";

        orcamentoRepo.atualizarStatus(orcamento.getId(), StatusOrcamento.APROVADO);
        Orcamento orcAprovado = orcamentoRepo.buscarPorId(orcamento.getId()).orElseThrow();
        assert orcAprovado.getStatusOrcamento() == StatusOrcamento.APROVADO : "Status não foi atualizado para APROVADO";

        String mensagemWhats = orcAprovado.enviarWhatsApp();
        assert mensagemWhats.contains("Olá, Carlos!") : "Mensagem de WhatsApp deve saudar apenas pelo primeiro nome do cliente";
        assert !mensagemWhats.contains("Carlos E. Arquiteto") : "Mensagem de WhatsApp não deve exibir o nome completo na saudação";
        assert mensagemWhats.contains("Proposta válida até 20/11/2026") : "Mensagem de WhatsApp deve exibir data limite fixa (05/11/2026 + 15 dias = 20/11/2026)";
        assert "Cliente".equals(Cliente.extrairPrimeiroNome(null)) : "Fallback de primeiro nome nulo deve ser 'Cliente'";
        assert "Cliente".equals(Cliente.extrairPrimeiroNome("   ")) : "Fallback de primeiro nome em branco deve ser 'Cliente'";
        System.out.println("[OK] OrcamentoRepository: Salvar, associar serviço/cliente, atualizar status, saudação pelo primeiro nome e data limite fixa validados.");

        // 6. Teste de CobrancaRepository
        Cobranca cobranca = new Cobranca(
            0, orcAprovado.getValorTotal(), "2026-12-05", null, StatusCobranca.EM_ABERTO, orcAprovado
        );
        cobrancaRepo.salvar(cobranca);
        assert cobranca.getId() > 0 : "ID da cobranca deve ser gerado";

        Cobranca cobrancaRecuperada = cobrancaRepo.buscarPorId(cobranca.getId()).orElseThrow();
        assert cobrancaRecuperada.getStatusCobranca() == StatusCobranca.EM_ABERTO : "Status da cobrança deve ser EM_ABERTO";

        String mensagemCobranca = cobrancaRecuperada.gerarMensagemCobranca();
        assert mensagemCobranca.contains("R$ 6.000,00") : "Mensagem deve conter o valor da cobrança formatado em BRL (R$ 6.000,00)";
        assert mensagemCobranca.contains("05/12/2026") : "Mensagem deve conter a data de vencimento formatada em dd/MM/yyyy";

        cobrancaRepo.registrarPagamento(cobranca.getId(), "2026-11-20");
        Cobranca cobrancaPaga = cobrancaRepo.buscarPorId(cobranca.getId()).orElseThrow();
        assert cobrancaPaga.getStatusCobranca() == StatusCobranca.PAGO : "Status deve ser PAGO";
        assert "2026-11-20".equals(cobrancaPaga.getDataPagamento()) : "Data de pagamento incorreta";
        System.out.println("[OK] CobrancaRepository: Salvar, gerar mensagem e registrar liquidação validados.");

        // 7. Teste de verificação de vínculos e exclusão em cascata transacional de Cliente
        assert clienteRepo.possuiVinculos(clienteAtualizado.getId()) : "Cliente com serviços/orçamentos/cobranças deve acusar vínculo";
        clienteRepo.deletarEmCascata(clienteAtualizado.getId());
        assert clienteRepo.buscarPorId(clienteAtualizado.getId()).isEmpty() : "Cliente deveria ter sido removido na exclusão em cascata";
        assert servicoRepo.listarPorCliente(clienteAtualizado.getId()).isEmpty() : "Serviços do cliente deveriam ter sido removidos em cascata";
        assert orcamentoRepo.listarPorCliente(clienteAtualizado.getId()).isEmpty() : "Orçamentos do cliente deveriam ter sido removidos em cascata";
        assert cobrancaRepo.buscarPorId(cobranca.getId()).isEmpty() : "Cobranças do cliente deveriam ter sido removidas em cascata";
        System.out.println("[OK] ClienteRepository: Verificação de vínculos e exclusão em cascata transacional validadas.");

        // 8. Limpeza do ambiente de teste
        ConnectionFactory.setDbPath("database/nexum.db"); // restaura caminho padrão
        testDb.delete();
        System.out.println("==================================================");
        System.out.println(" TODOS OS TESTES PASSARAM COM 100% DE SUCESSO!   ");
        System.out.println("==================================================");
    }
}
