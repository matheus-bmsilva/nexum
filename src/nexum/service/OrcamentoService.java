package nexum.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import nexum.Cliente;
import nexum.Orcamento;
import nexum.Servico;
import nexum.StatusOrcamento;
import nexum.exception.EntityNotFoundException;
import nexum.exception.ValidationException;
import nexum.repository.ClienteRepository;
import nexum.repository.OrcamentoRepository;
import nexum.repository.ServicoRepository;
import nexum.repository.sqlite.SqliteClienteRepository;
import nexum.repository.sqlite.SqliteOrcamentoRepository;
import nexum.repository.sqlite.SqliteServicoRepository;

/**
 * Serviço de negócio para geração, precificação final e emissão de orçamentos (RF04, RF05).
 */
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final ServicoRepository servicoRepository;
    private final ClienteRepository clienteRepository;

    public OrcamentoService() {
        this(new SqliteOrcamentoRepository(), new SqliteServicoRepository(), new SqliteClienteRepository());
    }

    public OrcamentoService(OrcamentoRepository orcamentoRepository, ServicoRepository servicoRepository, ClienteRepository clienteRepository) {
        this.orcamentoRepository = orcamentoRepository;
        this.servicoRepository = servicoRepository;
        this.clienteRepository = clienteRepository;
    }

    public Orcamento emitirOrcamento(int clienteId, int servicoId, int validadeDias, String dataEmissao) {
        if (validadeDias <= 0) {
            throw new ValidationException("A validade do orçamento deve ser de pelo menos 1 dia.");
        }
        if (dataEmissao == null || dataEmissao.trim().isBlank()) {
            throw new ValidationException("A data de emissão do orçamento é obrigatória.");
        }

        Cliente cliente = clienteRepository.buscarPorId(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado para o ID: " + clienteId));

        Servico servico = servicoRepository.buscarPorId(servicoId)
                .orElseThrow(() -> new EntityNotFoundException("Serviço não encontrado para o ID: " + servicoId));

        // Calcula o valor total com impostos embutidos
        double valorBase = servico.calcularValor();
        double valorTotalComImposto = valorBase * (1.0 + servico.getImposto());
        double valorTotalFinal = Math.round(valorTotalComImposto * 100.0) / 100.0;

        Orcamento orcamento = new Orcamento(
            0, dataEmissao.trim(), validadeDias, valorTotalFinal, StatusOrcamento.PENDENTE, servico, cliente
        );

        return orcamentoRepository.salvar(orcamento);
    }

    public String gerarLinkWhatsApp(int orcamentoId) {
        Orcamento orcamento = buscarPorId(orcamentoId);
        Cliente cliente = orcamento.getCliente();
        if (cliente == null) {
            throw new ValidationException("O orçamento não possui cliente vinculado.");
        }

        String tel = ClienteService.sanitizarTelefone(cliente.getWhatsApp());
        if (tel.isBlank()) {
            throw new ValidationException("O cliente não possui telefone de WhatsApp cadastrado.");
        }

        String primeiroNome = cliente.getPrimeiroNome();
        String servicoDesc = (orcamento.getServico() != null) ? orcamento.getServico().getDescricao() : "Projeto Arquitetônico";
        String valorFmt = String.format(Locale.GERMANY, "%,.2f", orcamento.getValorTotal());
        String dataValidadeFmt = orcamento.calcularDataValidadeFormatada();

        String mensagem = String.format(
            "Olá, %s! Tudo bem?\n\n" +
            "Segue a proposta orçamentária para o seu projeto de arquitetura:\n\n" +
            "• Projeto: %s\n" +
            "• Investimento Total: R$ %s\n" +
            "• Proposta válida até %s\n\n" +
            "Qualquer dúvida técnica ou ajuste de escopo, estou à sua inteira disposição!",
            primeiroNome, servicoDesc, valorFmt, dataValidadeFmt
        );

        String textoCodificado = URLEncoder.encode(mensagem, StandardCharsets.UTF_8);
        return "https://wa.me/" + tel + "?text=" + textoCodificado;
    }

    public void atualizarStatus(int orcamentoId, StatusOrcamento novoStatus) {
        buscarPorId(orcamentoId); // Valida existência
        orcamentoRepository.atualizarStatus(orcamentoId, novoStatus);
    }

    public Orcamento buscarPorId(int id) {
        return orcamentoRepository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não localizado para o ID: " + id));
    }

    public List<Orcamento> listarTodos() {
        return orcamentoRepository.listarTodos();
    }

    public List<Orcamento> listarPorCliente(int clienteId) {
        return orcamentoRepository.listarPorCliente(clienteId);
    }
}
