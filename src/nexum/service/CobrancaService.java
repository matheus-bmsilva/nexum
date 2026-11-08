package nexum.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import nexum.Cliente;
import nexum.Cobranca;
import nexum.Orcamento;
import nexum.StatusCobranca;
import nexum.exception.EntityNotFoundException;
import nexum.exception.ValidationException;
import nexum.repository.CobrancaRepository;
import nexum.repository.OrcamentoRepository;
import nexum.repository.sqlite.SqliteCobrancaRepository;
import nexum.repository.sqlite.SqliteOrcamentoRepository;

/**
 * Serviço de negócio para controle financeiro, recebíveis e conciliação de fluxo de caixa (RF06, RF07).
 */
public class CobrancaService {

    private final CobrancaRepository cobrancaRepository;
    private final OrcamentoRepository orcamentoRepository;

    public CobrancaService() {
        this(new SqliteCobrancaRepository(), new SqliteOrcamentoRepository());
    }

    public CobrancaService(CobrancaRepository cobrancaRepository, OrcamentoRepository orcamentoRepository) {
        this.cobrancaRepository = cobrancaRepository;
        this.orcamentoRepository = orcamentoRepository;
    }

    public record ExtratoFinanceiro(
        double totalFaturado,
        double totalEmAberto,
        int totalCobrancas,
        int cobrancasPagas,
        int cobrancasEmAberto
    ) {}

    public Cobranca gerarCobrancaParaOrcamento(int orcamentoId, String dataVencimento) {
        if (dataVencimento == null || dataVencimento.trim().isBlank()) {
            throw new ValidationException("A data de vencimento é obrigatória.");
        }

        LocalDate vencimento;
        try {
            vencimento = LocalDate.parse(dataVencimento.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Formato inválido para a data de vencimento. Utilize o formato AAAA-MM-DD.");
        }

        LocalDate hoje = LocalDate.now();
        if (vencimento.isBefore(hoje)) {
            throw new ValidationException("A data de vencimento não pode ser anterior à data atual.");
        }

        Orcamento orcamento = orcamentoRepository.buscarPorId(orcamentoId)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não localizado para o ID: " + orcamentoId));

        if (orcamento.getDataEmissao() != null && !orcamento.getDataEmissao().isBlank()) {
            try {
                LocalDate emissao = LocalDate.parse(orcamento.getDataEmissao().trim());
                if (vencimento.isBefore(emissao)) {
                    throw new ValidationException("A data de vencimento não pode ser anterior à data de emissão do orçamento (" + orcamento.getDataEmissao() + ").");
                }
            } catch (DateTimeParseException ignored) {
            }
        }

        Cobranca cobranca = new Cobranca(
            0, orcamento.getValorTotal(), dataVencimento.trim(), null, StatusCobranca.EM_ABERTO, orcamento
        );

        return cobrancaRepository.salvar(cobranca);
    }

    public void registrarPagamento(int cobrancaId, String dataPagamento) {
        buscarPorId(cobrancaId); // Valida existência
        String data = (dataPagamento != null && !dataPagamento.trim().isBlank())
                ? dataPagamento.trim()
                : LocalDate.now().toString();

        cobrancaRepository.registrarPagamento(cobrancaId, data);
    }

    public void deletar(int id) {
        buscarPorId(id); // Garante que a cobrança existe antes de deletar
        cobrancaRepository.deletar(id);
    }

    public String gerarMensagemCobranca(int cobrancaId) {
        Cobranca cobranca = buscarPorId(cobrancaId);
        Orcamento orcamento = cobranca.getOrcamento();
        Cliente cliente = (orcamento != null) ? orcamento.getCliente() : null;

        if (cliente == null) {
            throw new ValidationException("Cobrança sem cliente associado.");
        }

        String servicoDesc = (orcamento != null && orcamento.getServico() != null)
                ? orcamento.getServico().getDescricao()
                : "Projeto de Arquitetura";
        String valorFmt = String.format(Locale.GERMANY, "%.2f", cobranca.getValor());

        return String.format(
            "Olá, %s! Tudo bem?\n\n" +
            "Passando para compartilhar o lembrete referente ao investimento previsto para a próxima etapa do seu projeto de arquitetura:\n\n" +
            "• Projeto: %s\n" +
            "• Valor Previsto: R$ %s\n" +
            "• Data de Vencimento: %s\n\n" +
            "Seguimos dedicados a cada detalhe do seu projeto. Caso já tenha realizado este acerto ou precise de qualquer esclarecimento, estou à inteira disposição!",
            cliente.getNome(), servicoDesc, valorFmt, cobranca.getDataVencimento()
        );
    }

    public String gerarLinkWhatsAppCobranca(int cobrancaId) {
        Cobranca cobranca = buscarPorId(cobrancaId);
        Orcamento orcamento = cobranca.getOrcamento();
        Cliente cliente = (orcamento != null) ? orcamento.getCliente() : null;

        if (cliente == null) {
            throw new ValidationException("Cobrança sem cliente associado.");
        }

        String tel = ClienteService.sanitizarTelefone(cliente.getWhatsApp());
        if (tel.isBlank()) {
            throw new ValidationException("Telefone do cliente não cadastrado.");
        }

        String mensagem = gerarMensagemCobranca(cobrancaId);
        String encoded = URLEncoder.encode(mensagem, StandardCharsets.UTF_8);
        return "https://wa.me/" + tel + "?text=" + encoded;
    }

    public ExtratoFinanceiro obterExtratoFinanceiro() {
        List<Cobranca> cobrancas = cobrancaRepository.listarTodas();
        double totalFaturado = 0.0;
        double totalEmAberto = 0.0;
        int pagas = 0;
        int emAberto = 0;

        for (Cobranca c : cobrancas) {
            if (c.getStatusCobranca() == StatusCobranca.PAGO) {
                totalFaturado += c.getValor();
                pagas++;
            } else {
                totalEmAberto += c.getValor();
                emAberto++;
            }
        }

        return new ExtratoFinanceiro(
            round(totalFaturado),
            round(totalEmAberto),
            cobrancas.size(),
            pagas,
            emAberto
        );
    }

    public Cobranca buscarPorId(int id) {
        return cobrancaRepository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Cobrança não encontrada para o ID: " + id));
    }

    public List<Cobranca> listarTodas() {
        return cobrancaRepository.listarTodas();
    }

    public List<Cobranca> listarPorOrcamento(int orcamentoId) {
        return cobrancaRepository.listarPorOrcamento(orcamentoId);
    }

    private static double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
