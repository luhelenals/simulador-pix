package client;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Gerencia a comunicação de baixo nível (Socket) com o servidor.
 */
public class Connection {
    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    public Connection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Tenta estabelecer a conexão com o servidor.
     * @return true se a conexão for bem-sucedida, false caso contrário.
     */
    public boolean connect() {
        try {
            this.socket = new Socket(host, port);
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Thread.startVirtualThread(() -> sendRequest("{\"operacao\": \"conectar\"}"));

            System.out.println("Conectado ao servidor em " + host + ":" + port);
            return true;
        } catch (UnknownHostException e) {
            System.err.println("Host desconhecido: " + host);
            return false;
        } catch (IOException e) {
            System.err.println("Não foi possível conectar ao servidor. Verifique se ele está rodando.");
            return false;
        }
    }

    /**
     * Envia uma requisição JSON para o servidor e aguarda a resposta.
     * @param jsonRequest A string JSON da requisição.
     * @return A string JSON da resposta do servidor, ou null em caso de erro.
     */
    public String sendRequest(String jsonRequest) {
        System.out.println("\n[CONNECTION] Enviando para servidor: " + jsonRequest);
        try {
            writer.println(jsonRequest);
            return reader.readLine();
        } catch (IOException e) {
            System.err.println("Erro de comunicação com o servidor: " + e.getMessage());
            return null;
        }
    }

    /**
     * Fecha a conexão com o servidor.
     */
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Desconectado do servidor.");
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar a conexão: " + e.getMessage());
        }
    }
}