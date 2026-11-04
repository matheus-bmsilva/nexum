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
        this.statusCobranca = statusCobranca.EM_ABERTO;
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
        return statusCobranca;
    }
    
    public void registrarPagamento(){
        this.dataPagamento = dataPagamento;
        this.statusCobranca = statusCobranca.PAGO;
    }

    public void gerarMensagemCobranca(){
        String nomeCliente = this.orcamento.getCliente().getNome();
        return "Olá, " + nomeCliente + "! Passando para te lembrar sobre o pagamento do seu serviço conosco, no valor de R$ " + this.valor + " para a data " + this.dataVencimento + ". Qualquer dúvida estamos à disposição!";
    }
}
