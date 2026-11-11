package nexum.service;

import java.util.List;
import nexum.Cliente;
import nexum.ItemEtapa;
import nexum.Padrao;
import nexum.Servico;
import nexum.ServicoPorEtapa;
import nexum.ServicoPorHora;
import nexum.ServicoPorMetroQuadrado;
import nexum.ServicoPorPercentualObra;
import nexum.exception.EntityNotFoundException;
import nexum.exception.ValidationException;
import nexum.repository.ServicoRepository;
import nexum.repository.sqlite.SqliteServicoRepository;

/**
 * Serviço de precificação e orquestração polimórfica de serviços de arquitetura (RF03, RNF02).
 * Respeita OCP e LSP, permitindo simulações e cálculos de propostas técnicas sem acoplar com persistência direta.
 */
public class PrecificacaoService {

    public static final double IMPOSTO_PADRAO_MEI = 0.06; // Alíquota referencial do Simples Nacional / Anexo III

    private final ServicoRepository servicoRepository;
    private final ClienteService clienteService;
    private final SimplesNacionalService simplesNacionalService;

    public PrecificacaoService() {
        this(new SqliteServicoRepository(), new ClienteService(), new SimplesNacionalService());
    }

    public PrecificacaoService(ServicoRepository servicoRepository, ClienteService clienteService) {
        this(servicoRepository, clienteService, new SimplesNacionalService());
    }

    public PrecificacaoService(ServicoRepository servicoRepository, ClienteService clienteService, SimplesNacionalService simplesNacionalService) {
        this.servicoRepository = servicoRepository;
        this.clienteService = clienteService;
        this.simplesNacionalService = simplesNacionalService;
    }

    /**
     * DTO imutável com a memória de cálculo completa de um serviço arquitetônico,
     * incluindo impacto tributário real do Simples Nacional (Anexo III).
     */
    public record MemoriaCalculo(
        String tipoServico,
        double valorBase,
        double aliquotaImposto,
        double valorImposto,
        double valorTotalFinal,
        int faixaSimples,
        double rbt12,
        double parcelaADeduzir,
        String descricaoFaixa
    ) {
        public MemoriaCalculo(String tipoServico, double valorBase, double aliquotaImposto, double valorImposto, double valorTotalFinal) {
            this(tipoServico, valorBase, aliquotaImposto, valorImposto, valorTotalFinal, 1, 0.0, 0.0, "1ª Faixa: Até R$ 180.000,00");
        }
    }

    public MemoriaCalculo calcularMemoria(Servico servico, double rbt12) {
        if (servico == null) {
            throw new ValidationException("Serviço não informado para cálculo.");
        }
        double base = servico.calcularValor();
        double aliquota;
        int faixa = 1;
        double pd = 0.0;
        String desc = "1ª Faixa: Até R$ 180.000,00";

        if (rbt12 > 0) {
            SimplesNacionalService.ResultadoSimples res = simplesNacionalService.calcular(rbt12);
            aliquota = res.aliquotaEfetiva();
            faixa = res.faixa();
            pd = res.parcelaADeduzir();
            desc = res.descricaoFaixa();
        } else {
            aliquota = (servico.getImposto() > 0) ? servico.getImposto() : IMPOSTO_PADRAO_MEI;
        }

        double imposto = base * aliquota;
        double total = base + imposto;
        return new MemoriaCalculo(
            servico.getClass().getSimpleName(),
            round(base),
            round(aliquota),
            round(imposto),
            round(total),
            faixa,
            rbt12,
            pd,
            desc
        );
    }

    public MemoriaCalculo calcularMemoria(Servico servico) {
        return calcularMemoria(servico, 0.0);
    }

    public SimplesNacionalService getSimplesNacionalService() {
        return simplesNacionalService;
    }

    public double calcularAliquotaSimples(double rbt12) {
        if (rbt12 <= 0) return IMPOSTO_PADRAO_MEI;
        return simplesNacionalService.calcular(rbt12).aliquotaEfetiva();
    }

    public ServicoPorMetroQuadrado criarServicoMetroQuadrado(
            int clienteId, String descricao, String data, Padrao padrao, double imposto, double area, double valorM2) {

        Cliente cliente = clienteService.buscarPorId(clienteId);
        validarDescricaoEData(descricao, data);
        if (area <= 0) {
            throw new ValidationException("A metragem da área projetada deve ser superior a zero.");
        }
        if (valorM2 <= 0) {
            throw new ValidationException("O valor do metro quadrado deve ser maior que zero.");
        }

        double taxaImposto = (imposto > 0) ? imposto : IMPOSTO_PADRAO_MEI;
        ServicoPorMetroQuadrado servico = new ServicoPorMetroQuadrado(
            0, descricao.trim(), data.trim(), (padrao != null ? padrao : Padrao.MEDIO), cliente, taxaImposto, area, valorM2
        );
        return (ServicoPorMetroQuadrado) servicoRepository.salvar(servico);
    }

    public ServicoPorEtapa criarServicoEtapa(
            int clienteId, String descricao, String data, Padrao padrao, double imposto, List<ItemEtapa> itens) {

        Cliente cliente = clienteService.buscarPorId(clienteId);
        validarDescricaoEData(descricao, data);
        if (itens == null || itens.isEmpty()) {
            throw new ValidationException("O serviço por etapas deve conter pelo menos uma etapa de projeto.");
        }
        for (ItemEtapa item : itens) {
            if (item.getValor() < 0) {
                throw new ValidationException("O valor de cada etapa não pode ser negativo.");
            }
        }

        double taxaImposto = (imposto > 0) ? imposto : IMPOSTO_PADRAO_MEI;
        ServicoPorEtapa servico = new ServicoPorEtapa(
            0, descricao.trim(), data.trim(), (padrao != null ? padrao : Padrao.MEDIO), cliente, taxaImposto, itens
        );
        return (ServicoPorEtapa) servicoRepository.salvar(servico);
    }

    public ServicoPorHora criarServicoHora(
            int clienteId, String descricao, String data, Padrao padrao, double imposto, double horasTrabalhadas, double valorHora) {

        Cliente cliente = clienteService.buscarPorId(clienteId);
        validarDescricaoEData(descricao, data);
        if (horasTrabalhadas <= 0) {
            throw new ValidationException("A quantidade de horas técnicas estimadas deve ser maior que zero.");
        }
        if (valorHora <= 0) {
            throw new ValidationException("O valor da hora técnica de arquitetura deve ser maior que zero.");
        }

        double taxaImposto = (imposto > 0) ? imposto : IMPOSTO_PADRAO_MEI;
        ServicoPorHora servico = new ServicoPorHora(
            0, descricao.trim(), data.trim(), (padrao != null ? padrao : Padrao.MEDIO), cliente, taxaImposto, horasTrabalhadas, valorHora
        );
        return (ServicoPorHora) servicoRepository.salvar(servico);
    }

    public ServicoPorPercentualObra criarServicoPercentualObra(
            int clienteId, String descricao, String data, Padrao padrao, double imposto, double custoTotalObra, double percentual) {

        Cliente cliente = clienteService.buscarPorId(clienteId);
        validarDescricaoEData(descricao, data);
        if (custoTotalObra <= 0) {
            throw new ValidationException("O custo total estimado da obra deve ser superior a zero.");
        }
        if (percentual <= 0 || percentual > 1.0) {
            throw new ValidationException("O percentual de honorários deve estar entre 0 e 1 (ex: 0.05 para 5%).");
        }

        double taxaImposto = (imposto > 0) ? imposto : IMPOSTO_PADRAO_MEI;
        ServicoPorPercentualObra servico = new ServicoPorPercentualObra(
            0, descricao.trim(), data.trim(), (padrao != null ? padrao : Padrao.MEDIO), cliente, taxaImposto, custoTotalObra, percentual
        );
        return (ServicoPorPercentualObra) servicoRepository.salvar(servico);
    }

    public Servico buscarPorId(int id) {
        return servicoRepository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Serviço não encontrado para o ID: " + id));
    }

    public List<Servico> listarPorCliente(int clienteId) {
        return servicoRepository.listarPorCliente(clienteId);
    }

    public List<Servico> listarTodos() {
        return servicoRepository.listarTodos();
    }

    private void validarDescricaoEData(String descricao, String data) {
        if (descricao == null || descricao.trim().isBlank()) {
            throw new ValidationException("A descrição do serviço é obrigatória.");
        }
        if (data == null || data.trim().isBlank()) {
            throw new ValidationException("A data do serviço é obrigatória.");
        }
    }

    private static double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
