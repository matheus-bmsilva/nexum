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
    private int id;
    private String nome;
    private String cpf;
    private String whatsApp;

    public Cliente(int id, String nome, String cpf, String whatsApp){
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.whatsApp = whatsApp;
    }

    public Cliente(String nome, String cpf, String whatsApp){
        this.nome = nome;
        this.cpf = cpf;
        this.whatsApp = whatsApp;
    }

    public void setId(int id){
        this.id = id;
    }

    public int getId(){
        return id;
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

    public String getPrimeiroNome() {
        return extrairPrimeiroNome(this.nome);
    }

    public static String extrairPrimeiroNome(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.trim().isEmpty()) {
            return "Cliente";
        }
        return nomeCompleto.trim().split("\\s+")[0];
    }
}
