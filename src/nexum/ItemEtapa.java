/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nexum;

/**
 *
 * @author kirit
 */

public class ItemEtapa {
    private Etapa etapa;
    private double valor;

    public ItemEtapa(Etapa etapa, double valor){
        this.etapa = etapa;
        this.valor = valor;
    }

    public void setValor(double valor){ 
        this.valor = valor; 
    }  
    
    public double getValor(){
        return valor; 
    }
  
  public void setEtapa(Etapa etapa){
        this.etapa = etapa; 
    }
  
  public Etapa getEtapa(){ 
        return etapa; 
    }
    
}
