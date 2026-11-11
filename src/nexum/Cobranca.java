/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nexum;
    
/**
 *
 * @author kirit
 */
public class Cobranca {
    private int id;
    private double valor;
    private String dataVencimento;
    private String dataPagamento;
    private StatusCobranca statusCobranca;
    private Orcamento orcamento;

    public Cobranca(int id, double valor, String dataVencimento, String dataPagamento, StatusCobranca statusCobranca, Orcamento orcamento){
        this.id = id;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.dataPagamento = dataPagamento;
        this.statusCobranca = (statusCobranca != null) ? statusCobranca : StatusCobranca.EM_ABERTO;
        this.orcamento = orcamento;
    }

    public void setId(int id){
        this.id = id;
    }

    public int getId(){
        return id;
    }
    
    public void setValor(double valor){
        this.valor = valor;
    }

    public double getValor(){
        return valor;
    }

    public void setDataVencimento(String dataVencimento){
        this.dataVencimento = dataVencimento;
    }

    public String getDataVencimento(){
        return dataVencimento;
    }

    
    public void setDataPagamento(String dataPagamento){
        this.dataPagamento = dataPagamento;
    }

    public String getDataPagamento(){
        return dataPagamento;
    }

    
    public void setStatusCobranca(StatusCobranca statusCobranca){
        this.statusCobranca = statusCobranca;
    }

    public StatusCobranca getStatusCobranca(){
        return statusCobranca;
    }

    public void setOrcamento(Orcamento orcamento){
        this.orcamento = orcamento;
    }

    public Orcamento getOrcamento(){
        return orcamento;
    }
    
    public void registrarPagamento(String dataPagamento){
        this.dataPagamento = dataPagamento;
        this.statusCobranca = StatusCobranca.PAGO;
    }

    public void registrarPagamento(){
        this.statusCobranca = StatusCobranca.PAGO;
    }

    public String gerarMensagemCobranca(){
        String primeiroNome = (this.orcamento != null && this.orcamento.getCliente() != null)
                ? this.orcamento.getCliente().getPrimeiroNome()
                : "Cliente";
        String valorFmt = String.format(java.util.Locale.GERMANY, "%,.2f", this.valor);
        String vencimentoFmt = formatarDataBR(this.dataVencimento);
        return "Olá, " + primeiroNome + "! Tudo bem? Segue o lembrete referente ao investimento previsto para a etapa do seu projeto de arquitetura, no valor de R$ " + valorFmt + " com vencimento em " + vencimentoFmt + ". Qualquer dúvida, estamos à disposição!";
    }

    public static String formatarDataBR(String dataIso) {
        if (dataIso == null || dataIso.isBlank()) {
            return "";
        }
        String limpa = dataIso.trim();
        String[] partes = limpa.split("T")[0].split("-");
        if (partes.length == 3 && partes[0].length() == 4) {
            return partes[2] + "/" + partes[1] + "/" + partes[0];
        }
        return limpa;
    }
}
