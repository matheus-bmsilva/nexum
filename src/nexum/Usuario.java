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
    
    private int id;
    private String nome;
    private String email;
    private String senha;
    private String whatsApp;
    private String cnpj;

    public Usuario(int id, String nome, String email, String senha, String whatsApp, String cnpj) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.whatsApp = whatsApp;
        this.cnpj = cnpj;
    }

    public Usuario(String nome, String email, String senha, String whatsApp, String cnpj) {
        this(0, nome, email, senha, whatsApp, cnpj);
    }

    public Usuario(int id, String nome, String senha, String whatsApp, String cnpj) {
        this(id, nome, "", senha, whatsApp, cnpj);
    }

    public Usuario(String nome, String senha, String whatsApp, String cnpj) {
        this(0, nome, "", senha, whatsApp, cnpj);
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

    public void setEmail(String email){
        this.email = email;
    }

    public String getEmail(){
        return email;
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
