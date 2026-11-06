/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nexum;

import java.util.List;
/**
 *
 * @author kirit
 */
public class ServicoPorEtapa extends Servico{
    private List<ItemEtapa> itensEtapas;

    public ServicoPorEtapa(int id, String descricao, String data, Padrao padrao, Cliente cliente, double imposto, List<ItemEtapa> itensEtapas) {
        super(id, descricao, data, padrao, cliente, imposto);
        this.itensEtapas = itensEtapas;
    }

    public List<ItemEtapa> getItensEtapas() {
        return itensEtapas;
    }

    public void setItensEtapas(List<ItemEtapa> itensEtapas) {
        this.itensEtapas = itensEtapas;
    }

    @Override
    public double calcularValor() {
        double total = 0.0;
        if (itensEtapas != null) {
            for (ItemEtapa item : itensEtapas) {
                total += item.getValor(); 
            }
        }
        return total;
    }
}
