/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nexum;

/**
 *
 * @author kirit
 */
public class Orcamento {
    
    private int id;
    private String dataEmissao;
    private int validade;
    private double valorTotal;
    private StatusOrcamento statusOrcamento;
    private Servico servico;
    private Cliente cliente;

    public Orcamento(int id, String dataEmissao, int validade, double valorTotal, StatusOrcamento statusOrcamento, Servico servico, Cliente cliente){
        this.id = id;
        this.dataEmissao = dataEmissao;
        this.validade = validade;
        this.valorTotal = valorTotal;
        this.statusOrcamento = StatusOrcamento.PENDENTE;
        this.servico = servico;
        this.cliente = cliente;
        
    }

    public void setId(int id){
        this.id = id;
    }

    public int getId(){
        return id;
    }
    
    public void setDataEmissao(String dataEmissao){
        this.dataEmissao = dataEmissao;
    }

    public String getDataEmissao(){
        return dataEmissao;
    }
    
    public void setValidade(int validade){
        this.validade = validade;
    }

    public int getValidade(){
        return validade;
    }

    public void setValorTotal(double valorTotal){
        this.valorTotal = valorTotal;
    }

    public double getValorTotal(){
        return valorTotal;
    }

    public void setStatusOrcamento(StatusOrcamento statusOrcamento){
        this.statusOrcamento = statusOrcamento;
    }

    public StatusOrcamento getStatusOrcamento(){
        return statusOrcamento;
    }
    
    public void setServico(Servico servico){
        this.servico = servico;
    }

    public Servico getServico(){
        return servico;
    }

    public void setCliente(Cliente cliente){
        this.cliente = cliente;
    }

    public Cliente getCliente(){
        return cliente;
    }

    public void gerarPdf(){
        
    }

    public String enviarWhatsApp(){
        String nomeCliente = this.cliente.getNome();
        return "Olá, " + nomeCliente + "! Segue o orçamento prévio para os serviços solicitados. Qualquer dúvida, estamos à disposição!"
    }
}
