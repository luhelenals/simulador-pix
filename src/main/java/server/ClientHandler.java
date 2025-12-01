package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Locale;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import common.validator.RulesEnum;

import static common.util.RespostaManager.criarResposta;
import static common.util.RespostaManager.criarRespostaErro;
import static common.validator.Validator.validateClient;
import server.controllers.UsuarioController;
import server.controllers.TransacaoController;

/**
 * Esta classe é responsável por lidar com a comunicação de um único cliente.
 * Ela será executada em uma Thread separada para cada cliente conectado.
 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;

    // Construtor que recebe o socket do cliente conectado
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        // Usamos try-with-resources para garantir que o leitor, escritor e o socket sejam fechados
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        try (
                // Prepara para ler dados do cliente (requisições)
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                // Prepara para enviar dados para o cliente (respostas)
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            // Register client in GUI
            Server.registerClient(clientIp);

            String requestJson;
            // Loop para ler continuamente as mensagens do cliente
            while ((requestJson = reader.readLine()) != null) {
                System.out.println("Recebido do cliente: " + requestJson);
                // Log received message to GUI
                Server.logMessage(clientIp, "RECEIVED", requestJson);

                try {
                    String response = handleRequest(requestJson);
                    System.out.println("Enviando para o cliente: " + response + "\n");
                    // Log sent response to GUI
                    Server.logMessage(clientIp, "SENT", response);
                    writer.println(response);
                } catch (Exception e) {
                    System.out.println(e);
                    String response = criarResposta("usuario_login", false, e.getMessage());
                    System.out.println("Enviando para o cliente: " + response + "\n");
                    Server.logMessage(clientIp, "SENT", response);
                    writer.println(response);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro de comunicação com o cliente: " + e.getMessage());
        } finally {
            System.out.println("Cliente desconectado: " + clientIp);
            Server.unregisterClient(clientIp);
            try {
                clientSocket.close(); // Garante que o socket seja fechado
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String handleRequest(String request) throws Exception {
        try {
            JsonNode json = validateClient(request);
            RulesEnum operacao = RulesEnum.getEnum(json.get("operacao").asText());
            String response;

            // Adicionar testes de mensagens erradas enviadas para cliente
            boolean testesErros = false;
            System.out.println("Ativar modo testes para enviar erros? (S/N) ");
            Scanner scanner = new Scanner(System.in);
            if (scanner.nextLine().toUpperCase(Locale.ROOT).equals("S")) testesErros = true;

            switch(operacao) {
                case CONECTAR:
                    response = criarResposta(json.get("operacao").asText(), true, "Conectado com sucesso.");
                    break;
                case USUARIO_LOGIN:
                    response = testesErros ? criarRespostaErro() : UsuarioController.login(json);
                    break;
                case USUARIO_LOGOUT:
                    response = testesErros ? criarRespostaErro() : UsuarioController.logout(json);
                    break;
                case USUARIO_CRIAR:
                    response = testesErros ? criarRespostaErro() : UsuarioController.criarUsuario(json);
                    break;
                case USUARIO_LER:
                    response = testesErros ? criarRespostaErro() : UsuarioController.lerUsuario(json);
                    break;
                case USUARIO_ATUALIZAR:
                    response = testesErros ? criarRespostaErro() : UsuarioController.updateUsuario(json);
                    break;
                case USUARIO_DELETAR:
                    response = testesErros ? criarRespostaErro() : UsuarioController.deleteUsuario(json);
                    break;
                case TRANSACAO_CRIAR:
                    response = testesErros ? criarRespostaErro() : TransacaoController.criarTransacao(json);
                    break;
                case TRANSACAO_LER:
                    response = testesErros ? criarRespostaErro() : TransacaoController.getTransacoes(json);
                    break;
                case DEPOSITAR:
                    response = testesErros ? criarRespostaErro() : TransacaoController.depositar(json);
                    break;
                case ERRO_SERVIDOR:
                    response = criarResposta(json.get("operacao").asText(), true, "Mensagem de erro recebida.");
                    break;
                default:
                    throw new IllegalArgumentException("Operação do cliente desconhecida ou não suportada: " + operacao);
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}