package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import server.repository.Database; // Importa a classe do banco de dados

public class Server {

    // Define a porta como uma constante para fácil alteração
    private static final int PORTA = 15151;

    public static void main(String[] args) {
        // 1. Inicializa o banco de dados antes de tudo
        // Isso garante que as tabelas 'usuarios' e 'transacoes' existam.
        Database.initialize();

        // Usamos um try-with-resources para garantir que o ServerSocket seja fechado
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            System.out.println("Servidor iniciado e ouvindo na porta " + PORTA);

            // 2. Loop infinito para aceitar conexões de clientes continuamente
            while (true) {
                try {
                    System.out.println("Aguardando conexão de um novo cliente...");
                    Socket clientSocket = serverSocket.accept(); // Bloqueia até um cliente se conectar
                    System.out.println("Cliente conectado: " + clientSocket.getInetAddress().getHostAddress());

                    // 3. CORREÇÃO PRINCIPAL: Inicia uma nova Thread para cada cliente
                    // Cria o handler para o cliente
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    // Cria uma nova Thread para executar o handler
                    Thread clientThread = new Thread(clientHandler);
                    // Inicia a Thread. O método run() do clientHandler será executado.
                    clientThread.start();

                } catch (IOException e) {
                    System.err.println("Erro ao aceitar conexão do cliente: " + e.getMessage());
                    // Não quebra o loop, o servidor continua tentando aceitar outras conexões
                }
            }
        } catch (IOException e) {
            System.err.println("Erro fatal ao iniciar o servidor na porta " + PORTA + ": " + e.getMessage());
            e.printStackTrace();
            // Se não conseguirmos nem abrir a porta, o programa não pode continuar.
        }
    }
}