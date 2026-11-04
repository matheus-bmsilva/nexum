/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nexum;

/**
 *
 * @author kirit
 */
public class Cliente {
    private String nome;
    private String cpf;
    private String whatsApp;

    public Cliente(String nome, String cpf, String WhatsApp){
        this.nome = nome;
        this.cpf = cpf;
        this.whatsApp = whatsApp;
    }

    public void setNome(String nome){
        this.nome = nome;
    }

    public String getNome(){
        return nome;
    }

    public void setCpf(String cpf){
        this.cpf = cpf;
    }

    public String getCpf(){
        return cpf;
    }

    public void setWhatsApp(String whatsApp){
        this.whatsApp = whatsApp;
    }

    public String getWhatsApp(){
        return whatsApp;
    }
}
