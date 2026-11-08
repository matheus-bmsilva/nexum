package nexum.repository.sqlite;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import nexum.Cliente;
import nexum.ItemEtapa;
import nexum.Padrao;
import nexum.Servico;
import nexum.ServicoPorEtapa;
import nexum.ServicoPorHora;
import nexum.ServicoPorMetroQuadrado;
import nexum.ServicoPorPercentualObra;
import nexum.exception.DatabaseException;

/**
 * Fábrica polimórfica para instanciação e mapeamento de tipos de Servico (Princípio Aberto/Fechado - OCP).
 * Centraliza a resolução do discriminador relacional 'tipo_servico' sem poluir os repositórios.
 */
public class ServicoFactory {

    public static final String TIPO_METRO_QUADRADO = "METRO_QUADRADO";
    public static final String TIPO_ETAPA = "ETAPA";
    public static final String TIPO_HORA = "HORA";
    public static final String TIPO_PERCENTUAL_OBRA = "PERCENTUAL_OBRA";
    public static final String TIPO_GENERICO = "GENERICO";

    private ServicoFactory() {
    }

    /**
     * Identifica a constante do discriminador de persistência a partir do tipo em tempo de execução do objeto Servico.
     */
    public static String obterTipoServico(Servico servico) {
        if (servico instanceof ServicoPorMetroQuadrado) {
            return TIPO_METRO_QUADRADO;
        } else if (servico instanceof ServicoPorEtapa) {
            return TIPO_ETAPA;
        } else if (servico instanceof ServicoPorHora) {
            return TIPO_HORA;
        } else if (servico instanceof ServicoPorPercentualObra) {
            return TIPO_PERCENTUAL_OBRA;
        }
        return TIPO_GENERICO;
    }

    /**
     * Reconstrói o objeto de serviço concreto polimórfico a partir de um registro do ResultSet.
     */
    public static Servico criarServico(ResultSet rs, Cliente cliente, List<ItemEtapa> itens) throws SQLException {
        int id = rs.getInt("id");
        String tipo = rs.getString("tipo_servico");
        String descricao = rs.getString("descricao");
        String data = rs.getString("data");
        String padraoStr = rs.getString("padrao");
        Padrao padrao = (padraoStr != null) ? Padrao.valueOf(padraoStr) : Padrao.MEDIO;
        double imposto = rs.getDouble("imposto");

        return switch (tipo) {
            case TIPO_METRO_QUADRADO -> {
                double area = rs.getDouble("area");
                double valorM2 = rs.getDouble("valor_metro_quadrado");
                yield new ServicoPorMetroQuadrado(id, descricao, data, padrao, cliente, imposto, area, valorM2);
            }
            case TIPO_ETAPA -> new ServicoPorEtapa(id, descricao, data, padrao, cliente, imposto, itens);
            case TIPO_HORA -> {
                double horas = rs.getDouble("horas_trabalhadas");
                double valorHora = rs.getDouble("valor_hora");
                yield new ServicoPorHora(id, descricao, data, padrao, cliente, imposto, horas, valorHora);
            }
            case TIPO_PERCENTUAL_OBRA -> {
                double custoTotal = rs.getDouble("custo_total_obra");
                double percentual = rs.getDouble("percentual");
                yield new ServicoPorPercentualObra(id, descricao, data, padrao, cliente, imposto, custoTotal, percentual);
            }
            case TIPO_GENERICO -> new Servico(id, descricao, data, padrao, cliente, imposto);
            default -> throw new DatabaseException("Tipo de serviço desconhecido na persistência: " + tipo);
        };
    }
}
