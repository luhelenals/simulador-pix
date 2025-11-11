package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
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
        System.out.println("4. Ver extrato");
        System.out.println("5. Logout");
        System.out.println("6. Editar conta");
        System.out.println("7. Deletar conta");
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
            case 4: verExtrato(); break;
            case 5: logout(); break;
            case 6: update(); break;
            case 7: delete(); break;
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
        String responseJson = obterDadosUsuario();

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

    private String obterDadosUsuario() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_ler");
        request.put("token", this.token);

        return connection.sendRequest(request.toString());
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

    private void verExtrato() {
        System.out.print("Data inicial (yyyy-mm-dd): ");
        String dataInicial = scanner.nextLine().strip() + "T00:00:00Z";
        System.out.print("Data final (yyyy-mm-dd, deixar em branco para hoje): ");
        String dataFinal = scanner.nextLine().strip();
        dataFinal = dataFinal.isBlank() ? LocalDate.now().toString() : dataFinal + "T00:00:00Z";

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "transacao_ler");
        request.put("token", this.token);
        request.put("data_inicial", dataInicial);
        request.put("data_final", dataFinal);

        String dadosUsuarioJson = obterDadosUsuario();
        String responseJson = connection.sendRequest(request.toString());

        if (responseJson != null && dadosUsuarioJson != null) {
            try {
                JsonNode response = objectMapper.readTree(responseJson);
                System.out.println("Servidor: " + response.get("info").asText());

                JsonNode responseUsuario = objectMapper.readTree(dadosUsuarioJson);
                String cpfUsuario = responseUsuario.get("cpf").asText();

                if (response.get("status").asBoolean()) {
                    JsonNode transacoesNode = response.get("transacoes");
                    for (JsonNode transacao : transacoesNode) {
                        String id = transacao.get("id").asText();
                        double valor = transacao.get("valor_enviado").asDouble();

                        JsonNode enviadorNode = transacao.get("usuario_enviador");
                        String nome_enviador = enviadorNode.get("nome").asText();
                        String cpf_enviador = enviadorNode.get("cpf").asText();

                        JsonNode recebedorNode = transacao.get("usuario_recebedor");
                        String nome_recebedor = recebedorNode.get("nome").asText();
                        String cpf_recebedor = recebedorNode.get("cpf").asText();

                        String criado = transacao.get("criado_em").asText().substring(0,9);

                        System.out.println("=====================");
                        System.out.println(cpf_recebedor.equals(cpfUsuario) ? "RECEBIDO" : "ENVIADO");
                        System.out.println("Data: " + criado);
                        System.out.printf("Valor: R$ %.2f\n", valor);
                        System.out.println(cpf_recebedor.equals(cpfUsuario) ? "Origem: " : "Destino: ");
                        System.out.println("  Nome: " + (cpf_recebedor.equals(cpfUsuario) ? nome_enviador : nome_recebedor));
                        System.out.println("  CPF: " + (cpf_recebedor.equals(cpfUsuario) ? cpf_enviador : cpf_recebedor));
                        System.out.println("=====================");
                    }
                }
            } catch (JsonProcessingException e) {
                System.err.println("Erro ao processar resposta do servidor.");
            }
        }
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