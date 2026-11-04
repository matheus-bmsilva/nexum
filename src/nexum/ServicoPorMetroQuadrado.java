/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nexum;

/**
 *
 * @author kirit
 */
public class ServicoPorMetroQuadrado extends Servico{
    private double area;
    private double valorMetroQuadrado;

    public ServicoPorMetroQuadrado(double area, double valorMetroQuadrado){
        this.area = area;
        this.valorMetroQuadrado = valorMetroQuadrado;
    }

    public void setArea(double area){
        this.area = area;
    }

    public double getArea(){
        return area;
    }
    
    public void setValorMetroQuadrado(double valorMetroQuadrado){
        this.valorMetroQuadrado = valorMetroQuadrado;
    }

    public double getValorMetroQuadrado(){
        return valorMetroQuadrado;
    }

    @Override
    public double calcularValor(){
        return getValorMetroQuadrado() * getArea();
    }
}
