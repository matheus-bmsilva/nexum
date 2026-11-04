/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nexum;

/**
 *
 * @author kirit
 */
public class ServicoPorHora extends Servico{
    private double horasTrabalhadas;
    private double valorHora;

    public ServicoPorHora(double horasTrabalhadas, double valorHora){
        this.horasTrabalhadas = horasTrabalhadas;
        this.valorHora = valorHora;
    }

    public void setHorasTrabalhadas(double horasTrabalhadas){
        this.horasTrabalhadas = horasTrabalhadas;
    }

    public double getHorasTrabalhadas(){
        return horasTrabalhadas;
    }

    public void setValorHora(double valorHora){
        this.valorHora = valorHora;
    }

    public double getValorHora(){
        return valorHora;
    }

    @Override
    public double calcularValor(){
        return getValorHora * getHorasTrabalhadas;
    }
}
