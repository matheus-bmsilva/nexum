/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nexum;

/**
 *
 * @author kirit
 */
public class Servico {
    protected int id;
    protected String descricao;
    protected String data;
    protected Padrao padrao;
    protected Cliente cliente;
    protected double imposto;

    public Servico(int id, String descricao, String data, Padrao padrao, Cliente cliente, double imposto){
        this.id = id;
        this.descricao = descricao;
        this.data = data;
        this.padrao = padrao;
        this.cliente = cliente;
        this.imposto = 0.06;
    }

    public void setId(int id){
        this.id = id;
    }

    public int getId(){
        return id;
    }

    public void setDescricao(String descricao){
        this.descricao = descricao;
    }

    public String getDescricao(){
        return descricao;
    }

    public void setData(String data){
        this.data = data;
    }

    public String getData(){
        return data;
    }

    public void setPadrao(Padrao padrao){
        this.padrao = padrao;
    }

    public Padrao getPadrao(){
        return padrao;
    }

    public void setCliente(Cliente cliente){
        this.cliente = cliente;
    }

    public Cliente getCliente(){
        return cliente;
    }

    public void setImposto(double imposto){
        this.imposto = imposto;
    }

    public double getImposto(){
        return imposto;
    }

    public double calcularValor() {
        return 0.0;
    }
}
