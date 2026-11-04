/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nexum;

/**
 *
 * @author kirit
 */
public class Usuario {
    
    private String nome;
    private String senha;
    private String whatsApp;
    private String cnpj;

    public Usuario(String nome, String senha, String WhatsApp, String cnpj){
        this.nome = nome;
        this.senha = senha;
        this.whatsApp = whatsApp;
        this.cnpj = cnpj;
    }

    public void setNome(String nome){
        this.nome = nome;
    }

    public String getNome(){
        return nome;
    }

    public void setSenha(String senha){
        this.senha = senha;
    }

    public String getSenha(){
        return senha;
    }

    public void setWhatsApp(String whatsApp){
        this.whatsApp = whatsApp;
    }

    public String getWhatsApp(){
        return whatsApp;
    }

    public void setCnpj(String cnpj){
        this.cnpj = cnpj;
    }

    public String getCnpj(){
        return cnpj;
    }

    public void fazerLogin(){
        
    }

    public void editarPerfil(){
        
    }
}
