/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package nexum;

import nexum.database.DatabaseInitializer;
import nexum.web.NexumServer;

/**
 * Ponto de entrada da aplicação Nexum.
 */
public class Nexum {

    public static final int DEFAULT_PORT = 8080;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Inicializando o Nexum - Gestão para Arquitetos...");
        DatabaseInitializer.initialize();
        System.out.println("Banco de dados (" + (nexum.database.ConnectionFactory.isPostgres() ? "PostgreSQL" : "SQLite") + ") inicializado com sucesso.");

        int port = DEFAULT_PORT;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                port = Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException ignored) {
            }
        } else if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            NexumServer.start(port);
        } catch (Exception e) {
            System.err.println("Erro ao inicializar o servidor HTTP: " + e.getMessage());
        }
    }
    
}
