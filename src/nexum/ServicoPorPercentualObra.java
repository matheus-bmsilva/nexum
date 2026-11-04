/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nexum;

/**
 *
 * @author kirit
 */
public class ServicoPorPercentualObra extends Servico{
    private double custoTotalObra;
    private double percentual;

    public ServicoPorPercentualObra(double custoTotalObra, double percentual){
        this.custoTotalObra = custoTotalObra;
        this.percentual = percentual;
    }

    public void setCustoTotalObra(double custoTotalObra){
        this.custoTotalObra = custoTotalObra;
    }

    public double getCustoTotalObra(){
        return custoTotalObra;
    }

    public void setPercentual(double percentual){
        this.percentual = percentual;
    }

    public double getPercentual(){
        return percentual;
    }

    @Override
    public double calcularValor(){
        return getCustoTotalObra() * getPercentual();
    }
}
