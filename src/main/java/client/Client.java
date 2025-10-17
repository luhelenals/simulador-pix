package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Scanner;

import static server.controllers.UsuarioController.deleteUsuario;
import static server.controllers.UsuarioController.updateUsuario;

public class Client {

    private final Connection connection;
    private final Scanner scanner;
    private final ObjectMapper objectMapper;
    private String token; // Armazena o token de sessão do usuário logado

    public Client(String host, int port) {
        this.connection = new Connection(host, port);
        this.scanner = new Scanner(System.in);
        this.objectMapper = new ObjectMapper();
        this.token = null;
    }

    public void start() {
        if (!connection.connect()) {
            return; // Encerra se não conseguir conectar
        }

        boolean running = true;
        while (running) {
            if (token == null) {
                showMenuDeslogado();
            } else {
                showMenuLogado();
            }

            int escolha = scanner.nextInt();
            scanner.nextLine(); // Consome a nova linha

            if (token == null) {
                running = handleEscolhaDeslogado(escolha);
            } else {
                running = handleEscolhaLogado(escolha);
            }
        }
        connection.disconnect();
    }

    // --- MENUS ---
    private void showMenuDeslogado() {
        System.out.println("\n--- BANCO DISTRIBUÍDO ---");
        System.out.println("1. Criar Conta");
        System.out.println("2. Login");
        System.out.println("3. Sair");
        System.out.print("Escolha uma opção: ");
    }

    private void showMenuLogado() {
        System.out.println("\n--- ÁREA DO CLIENTE ---");
        System.out.println("1. Ver Saldo e Dados");
        System.out.println("2. Depositar");
        System.out.println("3. Fazer PIX (Transferência)");
        System.out.println("4. Logout");
        System.out.println("5. Editar conta");
        System.out.println("6. Deletar conta");
        System.out.print("Escolha uma opção: ");
    }

    // --- LÓGICA DE ESCOLHA ---
    private boolean handleEscolhaDeslogado(int escolha) {
        switch (escolha) {
            case 1: criarConta(); break;
            case 2: login(); break;
            case 3: return false; // Sair do loop
            default: System.out.println("Opção inválida.");
        }
        return true;
    }

    private boolean handleEscolhaLogado(int escolha) {
        switch (escolha) {
            case 1: verSaldo(); break;
            case 2: depositar(); break;
            case 3: fazerPix(); break;
            case 4: logout(); break;
            case 5: update(); break;
            case 6: delete(); break;
            default: System.out.println("Opção inválida.");
        }
        return true;
    }

    // --- MÉTODOS DE OPERAÇÃO ---
    private void criarConta() {
        System.out.print("Nome completo: ");
        String nome = scanner.nextLine();
        System.out.print("CPF (formato 000.000.000-00): ");
        String cpf = scanner.nextLine();
        System.out.print("Senha (mínimo 6 caracteres): ");
        String senha = scanner.nextLine();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_criar");
        request.put("nome", nome);
        request.put("cpf", cpf);
        request.put("senha", senha);

        processarResposta(request.toString());
    }

    private void login() {
        System.out.print("CPF: ");
        String cpf = scanner.nextLine();
        System.out.print("Senha: ");
        String senha = scanner.nextLine();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_login");
        request.put("cpf", cpf);
        request.put("senha", senha);

        String responseJson = connection.sendRequest(request.toString());
        System.out.println("Resposta do servidor: " + responseJson);
        if (responseJson != null) {
            try {
                JsonNode response = objectMapper.readTree(responseJson);
                System.out.println("Servidor: " + response.get("info").asText());
                if (response.get("status").asBoolean()) {
                    this.token = response.get("token").asText(); // Armazena o token
                    System.out.println("Token recebido do servidor: " + token);
                }
            } catch (JsonProcessingException e) {
                System.err.println("Erro ao processar resposta do servidor.");
            }
        }
    }

    private void logout() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_logout");
        request.put("token", this.token);

        processarResposta(request.toString());
        this.token = null; // Limpa o token localmente
    }

    private void verSaldo() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_ler");
        request.put("token", this.token);

        String responseJson = connection.sendRequest(request.toString());
        if (responseJson != null) {
            try {
                JsonNode response = objectMapper.readTree(responseJson);
                System.out.println("Servidor: " + response.get("info").asText());
                if (response.get("status").asBoolean()) {
                    JsonNode usuario = response.get("usuario");
                    System.out.println("  Nome: " + usuario.get("nome").asText());
                    System.out.println("  CPF: " + usuario.get("cpf").asText());
                    System.out.printf("  Saldo: R$ %.2f\n", usuario.get("saldo").asDouble());
                }
            } catch (JsonProcessingException e) {
                System.err.println("Erro ao processar resposta do servidor.");
            }
        }
    }

    private void delete() {
        System.out.print("Deletando este usuário");

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_deletar");
        request.put("token", this.token);

        processarResposta(request.toString());
        logout();
    }

    private void update() {
        System.out.print("Novo nome (deixar em branco para manter): ");
        String nome = scanner.nextLine();
        System.out.print("Nova senha (deixar em branco para manter): ");
        String senha = scanner.nextLine();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_atualizar");
        request.put("token", this.token);

        ObjectNode node = objectMapper.createObjectNode();
        if(!nome.isEmpty()) node.put("nome", nome);
        if(!senha.isEmpty()) node.put("senha", senha);

        request.put("usuario", node);

        processarResposta(request.toString());
    }

    private void depositar() {
        System.out.print("Valor a depositar: ");
        double valor = scanner.nextDouble();
        scanner.nextLine();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "depositar");
        request.put("token", this.token);
        request.put("valor_enviado", valor);

        processarResposta(request.toString());
    }

    private void fazerPix() {
        System.out.print("CPF de destino: ");
        String cpfDestino = scanner.nextLine();
        System.out.print("Valor a transferir: ");
        double valor = scanner.nextDouble();
        scanner.nextLine();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "transacao_criar");
        request.put("token", this.token);
        request.put("cpf_destino", cpfDestino);
        request.put("valor", valor);

        processarResposta(request.toString());
    }

    /**
     * Método genérico para enviar uma requisição e imprimir a resposta do servidor.
     */
    private void processarResposta(String requestJson) {
        System.out.println("Enviando requisição para o servidor: " + requestJson);
        String responseJson = connection.sendRequest(requestJson);
        if (responseJson != null) {
            try {
                JsonNode response = objectMapper.readTree(responseJson);
                System.out.println("Servidor: " + response.get("info").asText());
            } catch (JsonProcessingException e) {
                System.err.println("Erro ao processar resposta do servidor.");
            }
        }
    }

    public static void main(String[] args) {
        // Conecta ao servidor rodando na mesma máquina (localhost) na porta 9000
        String host = "localhost";
        int port = 9000;

        Scanner scanner = new Scanner(System.in);

        System.out.println("IP do servidor: ");
        host = scanner.nextLine();
        System.out.println("Porta do servidor: ");
        port = scanner.nextInt();

        Client client = new Client(host, port);
        client.start();
    }
}